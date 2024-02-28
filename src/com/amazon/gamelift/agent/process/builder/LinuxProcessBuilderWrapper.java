/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.amazon.gamelift.agent.command.CommandTransform;
import com.amazon.gamelift.agent.command.LinuxCommandTransform;
import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.OperatingSystemFamily;
import com.amazon.gamelift.agent.model.exception.BadExecutablePathException;
import com.amazon.gamelift.agent.module.ConfigModule;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Singleton
public class LinuxProcessBuilderWrapper implements ProcessBuilderWrapper {

    // Initializing only a single commandTransform
    private static final CommandTransform COMMAND_TRANSFORM = new LinuxCommandTransform();

    private final GameProcessConfiguration processConfiguration;
    private final OperatingSystem operatingSystem;

    private final ProcessBuilder processBuilder;

    /**
     * Constructor for LinuxProcessBuilderWrapper
     * @param processConfiguration
     * @param operatingSystem
     */
    @Inject
    public LinuxProcessBuilderWrapper(final GameProcessConfiguration processConfiguration,
                                      @Named(ConfigModule.OPERATING_SYSTEM) final OperatingSystem operatingSystem) {
        if (!OperatingSystemFamily.LINUX.equals(operatingSystem.getOperatingSystemFamily())) {
            //Creation validation. This class should only be used for Linux-based OS
            throw new IllegalArgumentException("Attempted to create Linux process for non Linux-based OS. Found "
                    + operatingSystem);
        }

        this.processConfiguration = processConfiguration;
        this.operatingSystem = operatingSystem;

        processBuilder = new ProcessBuilder(COMMAND_TRANSFORM.getFullCommandFromConfig(processConfiguration));
    }

    /**
     * Test constructor for LinuxProcessBuilderWrapper
     * @param processConfiguration
     * @param operatingSystem
     * @param processBuilder
     * @paragm serverProcessLaunchUser
     */
    @VisibleForTesting
    public LinuxProcessBuilderWrapper(final GameProcessConfiguration processConfiguration,
                      @Named(ConfigModule.OPERATING_SYSTEM) final OperatingSystem operatingSystem,
                      final ProcessBuilder processBuilder) {
        if (!OperatingSystemFamily.LINUX.equals(operatingSystem.getOperatingSystemFamily())) {
            //Creation validation. This class should only be used for Linux-based OS
            throw new IllegalArgumentException("Attempted to create Linux process for non Linux-based OS. Found "
                    + operatingSystem);
        }

        this.processConfiguration = processConfiguration;
        this.operatingSystem = operatingSystem;
        this.processBuilder = processBuilder;
    }

    @Override
    public Process buildProcess(final Map<String, String> environmentVariables) throws BadExecutablePathException {

        if (!verifyLaunchFileExists()) {
            throw new BadExecutablePathException(String.format("Executable path (%s) is invalid.",
                                                                processConfiguration.getLaunchPath()));
        }
        // Mostly for testing, but if operating system could not be detected, we use GameLiftAgent running directory
        // Otherwise, use launch path prefix as our running directory
        File directory = new File(operatingSystem.getLaunchPathPrefix());
        if (directory.exists()) {
            processBuilder.directory(directory);
        }
        // Large amounts of data to stdout will cause the Process to hang, especially if the stream gets flushed.
        // Discarding stdout shouldn't affect specific log4j configurations to log to a File, but stdout will be
        // pointed to /dev/null to effectively discard the output.
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        // Discard error stream to avoid potential SIGPIPE
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

        final Map<String, String> processEnvironmentVariables = processBuilder.environment();
        if (environmentVariables != null) {
            processEnvironmentVariables.putAll(environmentVariables);
        }

        try {
            return processBuilder.start();
        } catch (IOException e) {
            String errorMessage =
                    String.format("Failed to start process from configuration [%s]", processConfiguration.toString());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Verify if the executable file pointed by launchPath exists
     * @return true if the file exists false otherwise
     */
    public boolean verifyLaunchFileExists() {
        File launchFile = new File(processConfiguration.getLaunchPath());
        return launchFile.isFile();
    }
}
