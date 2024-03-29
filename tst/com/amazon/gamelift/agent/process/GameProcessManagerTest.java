/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process;

import com.amazon.gamelift.agent.logging.UploadGameSessionLogsCallableFactory;
import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.exception.BadExecutablePathException;
import com.amazon.gamelift.agent.model.exception.NotFinishedException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.process.builder.ProcessBuilderFactory;
import com.amazon.gamelift.agent.process.builder.ProcessBuilderWrapper;
import com.amazon.gamelift.agent.logging.UploadGameSessionLogsCallable;
import com.amazon.gamelift.agent.manager.ProcessEnvironmentManager;
import com.amazon.gamelift.agent.model.constants.ProcessConstants;
import com.amazon.gamelift.agent.process.destroyer.ProcessDestroyerFactory;
import com.amazon.gamelift.agent.process.destroyer.WindowsProcessDestroyer;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GameProcessManagerTest {

    private static final String TEST_PROCESS_UUID = "TEST_PROCESS_UUID";
    private static final String TEST_GAME_SESSION_ID = "TEST_GAME_SESSION_ID";

    @Mock private ProcessEnvironmentManager mockProcessEnvironmentManager;
    @Mock private ProcessTerminationEventManager mockTerminationEventManager;
    @Mock private ProcessBuilderWrapper processBuilderWrapper;
    @Mock private Process mockProcess;
    @Mock private Process mockProcess2;
    @Mock private ProcessHandle mockChildProcessHandle;
    @Mock private UploadGameSessionLogsCallableFactory uploadGameSessionLogsCallableFactory;
    @Mock private UploadGameSessionLogsCallable mockUploadGameSessionLogsCallable;
    @Mock private ScheduledExecutorService executorService;
    private GameProcessManager processManager;

    @BeforeEach
    public void setup() {
        // This test attempts to spin up a Linux Process
        processManager = new GameProcessManager(mockProcessEnvironmentManager, mockTerminationEventManager,
                OperatingSystem.DEFAULT_OS, uploadGameSessionLogsCallableFactory, executorService);
    }

    @AfterEach
    public void teardown() {
        try {
            processManager.terminateAllProcessesForShutdown(0L, 0L);
        } catch (final Exception e) {
            // Swallow Exception
        }
    }

    @Test
    public void GIVEN_nullProcessUUID_WHEN_isProcessAlive_THEN_returnsFalse() {
        final String nullProcessUUID = null;

        assertThrows(NotFoundException.class, () -> processManager.isProcessAlive(nullProcessUUID));
    }

    @Test
    public void GIVEN_invalidProcessUUID_WHEN_isProcessAlive_THEN_throwsNotFoundException() {
        final String badProcessUUID = "1234";

        assertThrows(NotFoundException.class, () -> processManager.isProcessAlive(badProcessUUID));
    }

    @Test
    public void GIVEN_validProcessUUID_WHEN_isProcessAlive_THEN_returnTrue() throws AgentException {
        // GIVEN
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.isAlive()).thenReturn(true);
            when(mockProcess.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess.destroyForcibly()).thenAnswer(it -> {
                mockProcess.onExit().complete(mockProcess);
                return mockProcess;
            });
            when(mockProcess.descendants()).thenReturn(Stream.empty());

            processManager.startProcessFromConfiguration(processConfig);
            assertEquals(1, processManager.getAllProcessUUIDs().size());
            assertTrue(processManager.isProcessAlive(processManager.getAllProcessUUIDs().iterator().next()));
        }
    }

    @Test
    public void GIVEN_computeActive_WHEN_startProcessFromConfiguration_THEN_processLaunched()
            throws AgentException {
        // Given
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess.destroyForcibly()).thenAnswer(it -> {
                mockProcess.onExit().complete(mockProcess);
                return mockProcess;
            });
            when(mockProcess.descendants()).thenReturn(Stream.empty());

            processManager.startProcessFromConfiguration(processConfig);
            assertEquals(1, processManager.getAllProcessUUIDs().size());
            // Single process id, pull it out of the set
            processManager.getAllProcessUUIDs().iterator().next();
        }
    }

    @Test
    public void GIVEN_processTerminates_WHEN_processTerminates_THEN_reportTerminate() throws InterruptedException, AgentException {
        // GIVEN
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.isAlive()).thenReturn(true);
            // Call actual methods so call-back (BiFunction) is triggered
            when(mockProcess.onExit()).thenCallRealMethod();

            // WHEN
            processManager.startProcessFromConfiguration(processConfig);
            String processUUID = processManager.getAllProcessUUIDs().iterator().next();
            //Sleep to make sure onExit is executed
            Thread.sleep(200);

            // THEN
            verify(mockTerminationEventManager, times(1)).notifyServerProcessTermination(processUUID, 0, null);
            assertEquals(0, processManager.getAllProcessUUIDs().size());
        }
    }

    @Test
    public void GIVEN_processTerminatesAndNotifyTerminationFails_WHEN_processTerminates_THEN_swallowsException()
            throws InterruptedException, AgentException {
        // GIVEN
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess.destroyForcibly()).thenAnswer(it -> {
                mockProcess.onExit().complete(mockProcess);
                return mockProcess;
            });
            when(mockProcess.descendants()).thenReturn(Stream.empty());
            when(uploadGameSessionLogsCallableFactory.newUploadGameSessionLogsCallable(
                    anyString(), anyString(), eq(new ArrayList<>()), any()))
                    .thenReturn(mockUploadGameSessionLogsCallable);

            doThrow(new RuntimeException()).when(mockTerminationEventManager)
                    .notifyServerProcessTermination(anyString(), anyInt(), any());

            // WHEN
            processManager.startProcessFromConfiguration(processConfig);
            String processUUID = processManager.getAllProcessUUIDs().iterator().next();
            processManager.terminateProcessByUUID(processUUID);
            //Sleep to make sure onExit is executed
            Thread.sleep(200);

            // THEN
            verify(mockTerminationEventManager, times(1))
                    .notifyServerProcessTermination(processUUID, 0, ProcessTerminationReason.SERVER_PROCESS_FORCE_TERMINATED);
            verify(executorService).submit(eq(mockUploadGameSessionLogsCallable));
            assertEquals(0, processManager.getAllProcessUUIDs().size());
        }
    }

    @Test
    public void GIVEN_validProcessUUIDAndTerminationReason_WHEN_terminateProcessByUUID_THEN_terminatesProcess()
            throws AgentException, InterruptedException {
        // GIVEN
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess.destroyForcibly()).thenAnswer(it -> {
                mockProcess.onExit().complete(mockProcess);
                return mockProcess;
            });
            when(mockProcess.descendants()).thenReturn(Stream.empty());

            processManager.startProcessFromConfiguration(processConfig);

            // WHEN
            String processUUID = processManager.getAllProcessUUIDs().iterator().next();
            processManager.terminateProcessByUUID(processUUID, ProcessTerminationReason.SERVER_PROCESS_TERMINATED_UNHEALTHY);
            Thread.sleep(200);

            // THEN
            assertEquals(0, processManager.getAllProcessUUIDs().size());
            verify(mockTerminationEventManager, times(1)).notifyServerProcessTermination(
                    eq(processUUID), anyInt(), eq(ProcessTerminationReason.SERVER_PROCESS_TERMINATED_UNHEALTHY));
        }
    }

    @Test
    public void GIVEN_invalidProcessUUID_WHEN_terminateProcessByUUID_THEN_doesNothing() {
        // GIVEN
        final String badProcessUUID = "1234";

        // WHEN
        processManager.terminateProcessByUUID(badProcessUUID, ProcessTerminationReason.COMPUTE_SHUTTING_DOWN);

        // THEN - no exception
        assertEquals(0, processManager.getAllProcessUUIDs().size());
    }

    @Test
    public void GIVEN_multipleProcesses_WHEN_terminateAllProcessesForShutdown_THEN_allProcessesTerminated()
            throws AgentException {
        // GIVEN
        final long totalWaitMillis = 1000L;
        final long pollWaitMillis = 100L;
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("sleep")
                .parameters("100")
                .concurrentExecutions(3)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            ProcessBuilder processBuilder = new ProcessBuilder(new ImmutableList.Builder<String>()
                    .add(processConfig.getLaunchPath())
                    .addAll(processConfig.getParameters()).build());
            // Make sure to spin up 3 processes via thenAnswer(), not one process 3 times via thenReturn()
            when(processBuilderWrapper.buildProcess(any())).thenAnswer(i -> processBuilder.start());

            processManager.startProcessFromConfiguration(processConfig);
            processManager.startProcessFromConfiguration(processConfig);
            processManager.startProcessFromConfiguration(processConfig);

            // WHEN
            processManager.terminateAllProcessesForShutdown(totalWaitMillis, pollWaitMillis);

            // THEN
            verify(mockTerminationEventManager, times(3)).notifyServerProcessTermination(
                    anyString(), anyInt(), eq(ProcessTerminationReason.COMPUTE_SHUTTING_DOWN));
            assertEquals(0, processManager.getAllProcessUUIDs().size());
        }
    }

    @Test
    public void GIVEN_processNotCompletedInTime_WHEN_terminateAllProcessesForShutdown_THEN_throwsNotFinishedException()
            throws AgentException {
        // GIVEN
        // Use 0 second wait time to force immediate completion
        final long totalWaitMillis = 0L;
        final long pollWaitMillis = 0L;
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));

            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            // Never calls handleProcessExit
            when(mockProcess.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess.descendants()).thenReturn(Stream.empty());

            processManager.startProcessFromConfiguration(processConfig);

            // WHEN / THEN
            assertThrows(NotFinishedException.class, () ->
                    processManager.terminateAllProcessesForShutdown(totalWaitMillis, pollWaitMillis));
        }
    }

    @Test
    public void GIVEN_newProcess_WHEN_getInitializationTimedOutProcessUUIDs_THEN_returnsEmptyList() throws AgentException {
        // GIVEN
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("sleep")
                .parameters("100")
                .concurrentExecutions(3)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class)) {
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.onExit()).thenCallRealMethod();
            processManager.startProcessFromConfiguration(processConfig);

            // WHEN
            Set<String> result = processManager.getInitializationTimedOutProcessUUIDs();

            // THEN
            assertEquals(0, result.size());
        }
    }

    @Test
    public void GIVEN_noProcesses_WHEN_getProcessCountsByConfiguration_THEN_returnsEmptyMap() {
        // WHEN
        Map<GameProcessConfiguration, Long> result = processManager.getProcessCountsByConfiguration();

        // THEN
        assertEquals(0, result.size());
    }

    @Test
    public void GIVEN_activeProcesses_WHEN_getProcessCountsByConfiguration_THEN_returnsMapOfProcessCounts()
            throws AgentException {
        // GIVEN
        final GameProcessConfiguration processConfig1 = GameProcessConfiguration.builder()
                .launchPath("someexecutable1")
                .concurrentExecutions(1)
                .build();
        final GameProcessConfiguration processConfig2 = GameProcessConfiguration.builder()
                .launchPath("someexecutable2")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));

            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any()))
                    .thenReturn(mockProcess)
                    .thenReturn(mockProcess2);

            when(mockProcess.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess2.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess.descendants()).thenReturn(Stream.empty());
            when(mockProcess2.descendants()).thenReturn(Stream.empty());

            processManager.startProcessFromConfiguration(processConfig1);

            processManager.startProcessFromConfiguration(processConfig2);
            processManager.startProcessFromConfiguration(processConfig2);

            // WHEN
            Map<GameProcessConfiguration, Long> result = processManager.getProcessCountsByConfiguration();

            // THEN
            assertEquals(2, result.size());
            assertEquals(1L, result.get(processConfig1));
            assertEquals(2L, result.get(processConfig2));
        }
    }
    @Test
    public void GIVEN_invalidLaunchPath_WHEN_startProcessFromConfiguration_THEN_throwException()
            throws AgentException {
        // Given
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class)) {
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenThrow(BadExecutablePathException.class);

            assertThrows(BadExecutablePathException.class,
                         () -> processManager.startProcessFromConfiguration(processConfig));

            verify(mockTerminationEventManager, times(1)).notifyServerProcessTermination(
                    anyString(), eq(ProcessConstants.INVALID_LAUNCH_PATH_PROCESS_EXIT_CODE),
                    eq(ProcessTerminationReason.SERVER_PROCESS_INVALID_PATH));
        }
    }

    @Test
    public void GIVEN_processThrowsBadExecutablePathException_WHEN_startProcessFromConfiguration_THEN_throwException()
            throws BadExecutablePathException {
        // Given
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        // WHEN
        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class)) {
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenThrow(RuntimeException.class);

        // THEN
            assertThrows(RuntimeException.class,
                    () -> processManager.startProcessFromConfiguration(processConfig));
        }
    }

    @Test
    public void GIVEN_validInput_WHEN_setLogPathsForProcess_THEN_logPathsSavedForTerminate() throws AgentException {
        // GIVEN
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("sleep")
                .parameters("100")
                .concurrentExecutions(1)
                .build();

        final List<String> logPathsList = Arrays.asList("/log/path/1234", "C:\\Log\\Path\\1234", "/log/path/1234");
        final List<String> logPathsDedupedList = Arrays.asList("/log/path/1234", "C:\\Log\\Path\\1234");

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            // Perform a more functional test to ensure the log paths get correctly sent on the termination hook
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.isAlive()).thenReturn(true);
            // Call actual methods so call-back (BiFunction) is triggered
            when(mockProcess.onExit()).thenCallRealMethod();
            when(mockProcess.destroyForcibly()).thenCallRealMethod();

            when(uploadGameSessionLogsCallableFactory.newUploadGameSessionLogsCallable(
                    anyString(), anyString(), eq(logPathsDedupedList), any()))
                    .thenReturn(mockUploadGameSessionLogsCallable);

            processManager.startProcessFromConfiguration(processConfig);

            // WHEN
            for (final String processUUID : processManager.getAllProcessUUIDs()) {
                processManager.updateProcessOnRegistration(processUUID, logPathsList);
                processManager.terminateProcessByUUID(processUUID, ProcessTerminationReason.SERVER_PROCESS_CRASHED);
            }

            // THEN
            assertEquals(0, ProcessHandle.current().children().count());
            verify(mockTerminationEventManager, times(1)).notifyServerProcessTermination(
                    anyString(), anyInt(), eq(ProcessTerminationReason.SERVER_PROCESS_CRASHED));

            // Transiently tests that the callable is generated using the correct list of log paths
            verify(executorService).submit(eq(mockUploadGameSessionLogsCallable));
        }
    }

    @Test
    public void GIVEN_processUUIDDoesNotExist_WHEN_setLogPathsForProcess_THEN_throwsNotFoundException() {
        // GIVEN
        final List<String> logPathsList = Arrays.asList("/log/path/1234", "C:\\Log\\Path\\1234", "/log/path/1234");

        // WHEN/THEN
        assertThrows(NotFoundException.class, () -> processManager.updateProcessOnRegistration(TEST_PROCESS_UUID, logPathsList));
    }

    @Test
    public void GIVEN_validInput_WHEN_updateProcessOnGameSessionActivation_THEN_gameSessionIdUpdated()
            throws AgentException {
        // GIVEN
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("sleep")
                .parameters("100")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            // Perform a more functional test to ensure the log paths get correctly sent on the termination hook
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.isAlive()).thenReturn(true);
            // Call actual methods so call-back (BiFunction) is triggered
            when(mockProcess.onExit()).thenCallRealMethod();
            when(mockProcess.destroyForcibly()).thenCallRealMethod();

            when(uploadGameSessionLogsCallableFactory.newUploadGameSessionLogsCallable(
                    anyString(), anyString(), any(), eq(TEST_GAME_SESSION_ID)))
                    .thenReturn(mockUploadGameSessionLogsCallable);

            processManager.startProcessFromConfiguration(processConfig);

            // WHEN
            for (final String processUUID : processManager.getAllProcessUUIDs()) {
                processManager.updateProcessOnGameSessionActivation(processUUID, TEST_GAME_SESSION_ID);
                processManager.terminateProcessByUUID(processUUID, ProcessTerminationReason.SERVER_PROCESS_CRASHED);
            }

            // THEN
            assertEquals(0, ProcessHandle.current().children().count());
            verify(mockTerminationEventManager, times(1)).notifyServerProcessTermination(
                    anyString(), anyInt(), eq(ProcessTerminationReason.SERVER_PROCESS_CRASHED));

            // Transiently tests that the callable is generated using the correct list of log paths
            verify(executorService).submit(eq(mockUploadGameSessionLogsCallable));
        }
    }

    @Test
    public void GIVEN_processUUIDDoesNotExist_WHEN_updateProcessOnGameSessionActivation_THEN_throwsNotFoundException() {
        // WHEN/THEN
        assertThrows(NotFoundException.class, ()
                -> processManager.updateProcessOnGameSessionActivation(TEST_PROCESS_UUID, TEST_GAME_SESSION_ID));
    }

    @Test
    public void GIVEN_childProcess_WHEN_terminateProcess_THEN_terminatesBothProcesses() throws AgentException {
        // GIVEN
        final GameProcessConfiguration processConfig = GameProcessConfiguration.builder()
                .launchPath("someexecutable")
                .concurrentExecutions(1)
                .build();

        try (MockedStatic<ProcessBuilderFactory> processBuilderFactory = mockStatic(ProcessBuilderFactory.class);
             MockedStatic<ProcessDestroyerFactory> processDestroyerFactory = mockStatic(ProcessDestroyerFactory.class)) {
            processDestroyerFactory.when(() -> ProcessDestroyerFactory.getProcessDestroyer(any()))
                    .thenReturn(new WindowsProcessDestroyer(OperatingSystem.WINDOWS_2019));
            processBuilderFactory.when(() -> ProcessBuilderFactory.getProcessBuilder(any(), any()))
                    .thenReturn(processBuilderWrapper);
            when(processBuilderWrapper.buildProcess(any())).thenReturn(mockProcess);
            when(mockProcess.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess.destroyForcibly()).thenAnswer(it -> {
                mockProcess.onExit().complete(mockProcess);
                return mockProcess;
            });
            when(mockProcess.descendants()).thenReturn(Stream.of(mockChildProcessHandle));

            processManager.startProcessFromConfiguration(processConfig);
            processManager.terminateProcessByUUID(processManager.getAllProcessUUIDs().iterator().next());

            verify(mockChildProcessHandle).destroyForcibly();
            verify(mockProcess).destroyForcibly();
        }
    }
}
