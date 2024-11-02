/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.destroyer;

import com.amazon.gamelift.agent.model.OperatingSystem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class LinuxProcessDestroyerTest {

    private static final OperatingSystem OPERATING_SYSTEM = OperatingSystem.DEFAULT_OS;
    private static final Path PARENT_PROCESS = new File("tst/resources/parent_process.sh").toPath();
    private static final Path CHILD_PROCESS = new File("tst/resources/child_process.sh").toPath();

    @Mock private Process mockInternalProcess;
    @Mock private Process mockDestroyerProcess;
    @Mock private ProcessBuilder mockProcessBuilder;

    private LinuxProcessDestroyer linuxProcessDestroyer;

    @BeforeEach
    public void setup() {
        linuxProcessDestroyer = new LinuxProcessDestroyer(OPERATING_SYSTEM, mockProcessBuilder);
    }

    @Test
    public void GIVEN_validProcess_WHEN_destroyProcess_THEN_killProcessGroup() throws IOException, InterruptedException {
        // GIVEN
        long processUUID = Long.valueOf(RandomStringUtils.randomNumeric(12));
        when(mockInternalProcess.pid()).thenReturn(processUUID);
        when(mockProcessBuilder.start()).thenReturn(mockDestroyerProcess);
        when(mockDestroyerProcess.waitFor()).thenReturn(0);

        // WHEN
        linuxProcessDestroyer.destroyProcess(mockInternalProcess);

        // THEN
        verify(mockProcessBuilder).command(List.of("kill", "-9", "-" + processUUID));
    }

    @Test
    public void GIVEN_processBuilderFailsStart_WHEN_destroyProcess_THEN_throwsException() throws IOException {
        // GIVEN
        long processUUID = Long.valueOf(RandomStringUtils.randomNumeric(12));
        when(mockInternalProcess.pid()).thenReturn(processUUID);
        when(mockProcessBuilder.start()).thenThrow(new IOException());

        // WHEN
        assertThrows(RuntimeException.class, () -> linuxProcessDestroyer.destroyProcess(mockInternalProcess));

        // THEN - Exception
    }

    @Test
    public void GIVEN_processBuilderFailsWait_WHEN_destroyProcess_THEN_throwsException() throws IOException, InterruptedException {
        // GIVEN
        long processUUID = Long.valueOf(RandomStringUtils.randomNumeric(12));
        when(mockInternalProcess.pid()).thenReturn(processUUID);
        when(mockProcessBuilder.start()).thenReturn(mockDestroyerProcess);
        when(mockDestroyerProcess.waitFor()).thenThrow(new InterruptedException());

        // WHEN
        assertThrows(RuntimeException.class, () -> linuxProcessDestroyer.destroyProcess(mockInternalProcess));

        // THEN - Exception
    }

    /**
     * This test creates actual processes, without child processes, destroys one, and verifies that the other is
     * still running correctly.
     */
    @Disabled // Disabled in case sudo is not available. Remove annotation to test.
    @Test
    public void GIVEN_processesWithoutChildren_WHEN_destroyProcess_THEN_onlyKillsOneProcess()
            throws IOException, InterruptedException {
        // GIVEN - 2 processes that don't have children
        final Process process1 = createSimpleProcess();
        final Process process2 = createSimpleProcess();
        // Sleep briefly to processes are started
        Thread.sleep(500);
        // Log information about the processes
        log.info("Parent process 1: {}", process1.pid());
        log.info("Parent process 2: {}", process2.pid());

        // WHEN - Create a LinuxProcessDestroyer and destroy process 1
        final LinuxProcessDestroyer linuxProcessDestroyer = new LinuxProcessDestroyer(OPERATING_SYSTEM);
        linuxProcessDestroyer.destroyProcess(process1);
        // Sleep briefly to ensure the kill command has completed
        Thread.sleep(500);

        // THEN - verify process 1 is terminated and process 2 is still running and healthy
        assertFalse(process1.isAlive());
        assertTrue(process2.isAlive());
    }

    /**
     * This test uses actual processes, which each create a child process, to verify that the child process is
     * killed when the parent is killed.  And also that the child of the second process is not killed accidentally.
     */
    @Disabled // Disabled in case sudo is not available. Remove annotation to test.
    @Test
    public void GIVEN_processesWithChildren_WHEN_destroyProcess_THEN_onlyKillsOneProcessAndChild()
            throws IOException, InterruptedException {
        // GIVEN - 2 processes that each create a child process
        final Process process1 = createProcessWithChild();
        final Process process2 = createProcessWithChild();
        // Sleep briefly to ensure child processes are created
        Thread.sleep(500);
        // Log information about the process and child
        log.info("Parent process 1: {}, child: {}", process1.pid(), process1.children()
                .map(ProcessHandle::pid).collect(Collectors.toList()));
        log.info("Parent process 2: {}, child: {}", process2.pid(), process2.children()
                .map(ProcessHandle::pid).collect(Collectors.toList()));

        // WHEN - Create a LinuxProcessDestroyer and destroy process 1
        final LinuxProcessDestroyer linuxProcessDestroyer = new LinuxProcessDestroyer(OPERATING_SYSTEM);
        linuxProcessDestroyer.destroyProcess(process1);
        // Sleep briefly to ensure the kill command has completed
        Thread.sleep(500);

        // THEN - verify process 1 is terminated with no children and process 2 / child are still running and healthy
        assertFalse(process1.isAlive());
        assertTrue(process1.children().toList().isEmpty());
        assertTrue(process2.isAlive());
        assertTrue(process2.children().map(ProcessHandle::isAlive).toList().get(0));
    }

    /**
     * Helper method to start a process with a child.
     * - setsid assigns a separate PGID to the process, otherwise the process will be a child of the unit test thread.
     * - Passing in the full path of the child process so there isn't any issue with relative paths.
     */
    private Process createProcessWithChild() throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("setsid", "sh", PARENT_PROCESS.toString(), "-p", CHILD_PROCESS.toString());
        return processBuilder.start();
    }

    /**
     * Helper method to start a process without any children.  Using the child process from above will suffice.
     * - setsid assigns a separate PGID to the process, otherwise the process will be a child of the unit test thread.
     */
    private Process createSimpleProcess() throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("setsid", "sh", CHILD_PROCESS.toString());
        return processBuilder.start();
    }
}
