/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.sun.jna.LastErrorException;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * WindowsProcess is a wrapper using Java's Process around WinBase.PROCESS_INFORMATION.
 * This provides access to the underlying Windows object but in the Java Process wae.
 *
 * This is done to ensure that there is consistent object to track Processes and what's a better object in Java but Java's.
 */
@Slf4j
@RequiredArgsConstructor
public class WindowsProcess extends Process {

    private static final Integer UNKNOWN_EXIT_CODE = -2;
    // Used to parse our exception error messages. Windows 2012 are either HRESULT or NTSTATUS (parsed by ntdll)
    private static final String NTDLL_DLL = "ntdll.dll";

    private final WinNT.HANDLE processHandle;
    private final WinDef.DWORD processId;

    @Override
    public OutputStream getOutputStream() {
        // The output stream of the game server is not tracked on Windows
        return null;
    }

    @Override
    public InputStream getInputStream() {
        // The input stream of the game server is not tracked on Windows
        return null;
    }

    @Override
    public InputStream getErrorStream() {
        // The error stream of the game server is not tracked on Windows
        return null;
    }

    @Override
    public int waitFor() {
        Kernel32.INSTANCE.WaitForSingleObject(processHandle, WinBase.INFINITE);
        return exitValue();
    }

    @Override
    public int exitValue() {
        final IntByReference exitCode = new IntByReference();
        final boolean gotExitCode = Kernel32.INSTANCE.GetExitCodeProcess(processHandle, exitCode);

        final Optional<Integer> exit;
        if (gotExitCode) {
            if (Kernel32.STILL_ACTIVE == exitCode.getValue()) {
                exit = Optional.empty();
            } else {
                exit = Optional.of(exitCode.getValue());
            }
        } else {
            exit = Optional.of(UNKNOWN_EXIT_CODE);
        }

        exit.ifPresent(this::logExitMessage);

        return exit.orElseThrow(() -> new IllegalThreadStateException("The process has not exited."));
    }

    @Override
    public void destroy() {
        Kernel32.INSTANCE.TerminateProcess(processHandle, WindowsProcessCommons.FORCE_TERMINATED_EXIT_CODE);
    }

    private void logExitMessage(final int exitCode) {

        String message;
        try {
            message = Kernel32Util.formatMessage(exitCode);
            log.info("Found process {} exit code {} message: {}", getProcessId(), exitCode, message.trim());
            return;
        } catch (final LastErrorException e) {
            //Failed to parse exit code as a HRESULT. Check if it is a NTSTATUS
        }

        try {
            final PointerByReference buffer = new PointerByReference();
            // Call returns int with size of message. If 0, message doesn't exist, but it is not a concern here.
            // Try to match exit code with the data here:
            // https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-erref/596a1078-e883-4972-9bbc-49e60bebca55
            Kernel32.INSTANCE.FormatMessage(WinBase.FORMAT_MESSAGE_ALLOCATE_BUFFER | WinBase.FORMAT_MESSAGE_FROM_SYSTEM | WinBase.FORMAT_MESSAGE_FROM_HMODULE,
                    Kernel32.INSTANCE.GetModuleHandle(NTDLL_DLL).getPointer(), exitCode, 0, buffer, 0, null);
            if (buffer.getValue() != null) {
                // Buffer has been filled with some value, so it should be pulled
                message = buffer.getValue().getWideString(0L);
                // Cleanup buffer now that data is received out of it
                Kernel32.INSTANCE.LocalFree(buffer.getValue());
                log.info("Found process {} exit code {} message: {}", getProcessId(), exitCode, message.trim());
            }
        } catch (final Exception e) {
            // Normally, this case shouldn't be encountered because Kernel32 methods return even if an exception occurs,
            // depending on the caller to check GetLastError(). This should only happen if ntdll is accessed incorrectly.
            log.info("Failed to parse exit code {} message for process {}", exitCode, getProcessId(), e);
        }
    }

    @Override
    public long pid() {
        return processId.longValue();
    }

    private String getProcessId() {
        return processId.toString();
    }

    @Override
    public Stream<ProcessHandle> children() {
        return WindowsProcessCommons.getChildren(processId.longValue());
    }

    @Override
    public Stream<ProcessHandle> descendants() {
        return children().flatMap(ProcessHandle::children);
    }

    @Override
    public ProcessHandle toHandle() {
        return new WindowsProcessHandle(processId, processHandle);
    }

}
