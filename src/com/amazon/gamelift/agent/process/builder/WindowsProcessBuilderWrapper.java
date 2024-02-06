/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.OperatingSystemFamily;
import com.amazon.gamelift.agent.model.exception.BadExecutablePathException;
import com.amazon.gamelift.agent.windows.MoreAdvapi32;
import com.amazon.gamelift.agent.model.constants.ProcessConstants;
import com.amazon.gamelift.agent.module.ConfigModule;
import com.google.common.annotations.VisibleForTesting;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class WindowsProcessBuilderWrapper implements ProcessBuilderWrapper {

    private final GameProcessConfiguration processConfiguration;
    private final OperatingSystem operatingSystem;

    /**
     * Constructor for WindowsProcessBuilderWrapper
     * @param processConfiguration
     * @param operatingSystem
     */
    @Inject
    public WindowsProcessBuilderWrapper(final GameProcessConfiguration processConfiguration,
                                        @Named(ConfigModule.OPERATING_SYSTEM) final OperatingSystem operatingSystem) {
        if (!OperatingSystemFamily.WINDOWS.equals(operatingSystem.getOperatingSystemFamily())) {
            //Creation validation. This class should only be used for Windows-based OS
            throw new IllegalArgumentException("Attempted to create Windows process for non Windows-based OS. Found "
                    + operatingSystem);
        }

        this.processConfiguration = processConfiguration;
        this.operatingSystem = operatingSystem;
    }

    @Override
    public Process buildProcess(Map<String, String> environmentVariables) throws BadExecutablePathException {

        try {
            // Create an environment variable pointer; see javadoc comment on function (below)
            Pointer envVars = createEnvironmentVariablePointer(environmentVariables);

            // See: https://msdn.microsoft.com/en-us/library/windows/desktop/ms686331(v=vs.85).aspx
            WinBase.STARTUPINFO startupInfo = new WinBase.STARTUPINFO();
            startupInfo.lpDesktop = StringUtils.EMPTY;

            // See: https://msdn.microsoft.com/en-us/library/windows/desktop/ms684873(v=vs.85).aspx
            WinBase.PROCESS_INFORMATION processInformation = new WinBase.PROCESS_INFORMATION();

            // See: https://msdn.microsoft.com/en-us/library/windows/desktop/ms682429(v=vs.85).aspx
            final boolean processCreated =
                    MoreAdvapi32.INSTANCE.CreateProcessAsUser(null,
                            null,
                            generateCommandLine(processConfiguration.getLaunchPath(),
                                    processConfiguration.getParameters()),
                            null,
                            null,
                            false,
                            Kernel32.CREATE_UNICODE_ENVIRONMENT,
                            envVars,
                            operatingSystem.getLaunchPathPrefix(),
                            startupInfo,
                            processInformation);

            if (processCreated) {
                return new WindowsProcess(processInformation.hProcess, processInformation.dwProcessId);
            } else {
                int errorCode = Kernel32.INSTANCE.GetLastError();
                // Exit code 2 implies that the executable path was not valid
                // In case of linux we use isFile() API to determine if the executable path is valid
                if (ProcessConstants.INVALID_LAUNCH_PATH_PROCESS_EXIT_CODE == errorCode) {
                    throw new BadExecutablePathException(String.format("Executable path (%s) is invalid.",
                                                                        processConfiguration.getLaunchPath()));
                }
                throw new Win32Exception(errorCode);
            }
        } catch (BadExecutablePathException e) {
            log.error("Error starting windows process", e);
            throw e;
        } catch (Exception e) {
            log.error("Error starting windows process", e);
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    String generateCommandLine(final String executable, final List<String> arguments) {
        final StringBuilder commandLine = new StringBuilder(String.format("\"%s\"", executable));

        String argsString = " ";
        if (CollectionUtils.isNotEmpty(arguments)) {
            argsString = String.join(" ", arguments);
        }
        commandLine.append(String.format(" %s", argsString));

        return commandLine.toString();
    }

    /**
     * https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-createprocessasusera
     * An environment block consists of a null-terminated block of null-terminated strings.
     * Each string is in the following form: name=value\0
     * Because the equal sign is used as a separator, it must not be used in the name of an environment variable.
     *
     * You may see older code pull current environment variables through:
     *     Userenv.INSTANCE.CreateEnvironmentBlock(environment, token, false);
     * Unfortunately, JNA doesn't make it easy to iterate through a given pointer as Windows uses UTF_16LE
     * (2 byte, Little Endian) while JNA pulls the chars as a single byte. To avoid doing any String encoding
     * conversion, we're just using java System.getEnv() to get the vars.
     */
    private Pointer createEnvironmentVariablePointer(final Map<String, String> toBeSet) {
        Map<String, String> completeEnvVars = new HashMap<>(toBeSet);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Windows is little endian
        Charset charset = StandardCharsets.UTF_16LE;

        Map<String, String> env = System.getenv();
        // Push system env vars into byte stream
        env.forEach((key, value) -> {
            try {
                bos.write(key.getBytes(charset));
                bos.write("=".getBytes(charset));
                bos.write(value.getBytes(charset));
                // env vars are separated by \0. need to ensure we have two bytes for UTF_16 - \u0000
                bos.write(Character.MIN_VALUE);
                bos.write(Character.MIN_VALUE);
            } catch (Exception e) {
                log.error("Failed to write to buffer: {}", e);
                throw new RuntimeException(e);
            }
        });

        // Pushing provided env vars to byte stream
        completeEnvVars.forEach((key, value) -> {
            try {
                bos.write(key.getBytes(charset));
                bos.write("=".getBytes(charset));
                bos.write(value.getBytes(charset));
                bos.write(Character.MIN_VALUE);
                bos.write(Character.MIN_VALUE);
            } catch (Exception e) {
                log.error("Failed to write to buffer: {}", e);
                throw new RuntimeException(e);
            }
        });
        // env vars are terminated with an extra terminal char
        bos.write(Character.MIN_VALUE);
        bos.write(Character.MIN_VALUE);
        byte[] byteArray = bos.toByteArray();
        log.debug("Generated environment vars: {}", new String(byteArray, StandardCharsets.UTF_16LE));
        // Allocating a new block of memory to store the env vars and writing to said block
        Pointer pointer = new Memory(byteArray.length);
        pointer.write(0, byteArray, 0, byteArray.length);
        return pointer;
    }
}
