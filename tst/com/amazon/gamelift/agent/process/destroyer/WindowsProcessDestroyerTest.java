/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.destroyer;

import com.amazon.gamelift.agent.model.OperatingSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WindowsProcessDestroyerTest {

    private static final OperatingSystem OPERATING_SYSTEM = OperatingSystem.WINDOWS_2019;

    @Mock private Process mockInternalProcess;
    @Mock private ProcessHandle mockChildProcessHandle;

    private WindowsProcessDestroyer windowsProcessDestroyer;

    @BeforeEach
    public void setup() {
        windowsProcessDestroyer = new WindowsProcessDestroyer(OPERATING_SYSTEM);
    }

    @Test
    public void GIVEN_validProcess_WHEN_destroyProcess_THEN_killProcessAndSubProcesses() {
        // GIVEN
        when(mockInternalProcess.descendants()).thenReturn(Stream.of(mockChildProcessHandle));

        // WHEN
        windowsProcessDestroyer.destroyProcess(mockInternalProcess);

        // THEN
        // Verify the child process is destroyed first
        InOrder inOrder = inOrder(mockChildProcessHandle, mockInternalProcess);
        inOrder.verify(mockChildProcessHandle).destroyForcibly();
        inOrder.verify(mockInternalProcess).destroyForcibly();
    }

    @Test
    public void GIVEN_destroyForciblyFails_WHEN_destroyProcess_THEN_nothing() {
        // GIVEN
        when(mockInternalProcess.descendants()).thenReturn(Stream.of(mockChildProcessHandle));
        when(mockInternalProcess.destroyForcibly()).thenThrow(new IllegalStateException());

        // WHEN
        windowsProcessDestroyer.destroyProcess(mockInternalProcess);

        // THEN - Nothing
    }
}
