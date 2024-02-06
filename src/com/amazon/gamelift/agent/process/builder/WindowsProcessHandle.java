/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*/
package com.amazon.gamelift.agent.process.builder;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class WindowsProcessHandle implements ProcessHandle {
    private final WinDef.DWORD processId;
    private final WinNT.HANDLE processHandle;

    @Override
    public long pid() {
        return processId.longValue();
    }

    @Override
    public Optional<ProcessHandle> parent() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public Stream<ProcessHandle> children() {
        return WindowsProcessCommons.getChildren(processId.longValue());
    }

    @Override
    public Stream<ProcessHandle> descendants() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public Info info() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CompletableFuture<ProcessHandle> onExit() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean supportsNormalTermination() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean destroy() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean destroyForcibly() {
        return Kernel32.INSTANCE.TerminateProcess(processHandle, WindowsProcessCommons.FORCE_TERMINATED_EXIT_CODE);
    }

    @Override
    public boolean isAlive() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public int compareTo(ProcessHandle other) {
        throw new NotImplementedException("Not implemented");
    }
}
