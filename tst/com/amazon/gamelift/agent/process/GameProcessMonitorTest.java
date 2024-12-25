/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.RuntimeConfiguration;
import com.amazon.gamelift.agent.model.exception.BadExecutablePathException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.manager.ExecutorServiceManager;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazon.gamelift.agent.manager.RuntimeConfigurationManager;
import com.amazon.gamelift.agent.manager.StateManager;

@ExtendWith(MockitoExtension.class)
public class GameProcessMonitorTest {

    private static final String INVALID_LAUNCH_PATH = "invalidPath";
    public static final long TEST_INITIAL_PROCESS_LAUNCH_DELAY_SECONDS = 3L; // Three seconds
    @Mock private StateManager mockStateManager;
    @Mock private RuntimeConfigurationManager mockRuntimeConfigurationManager;
    @Mock private GameProcessManager mockGameProcessManager;
    @Mock private ScheduledExecutorService mockExecutorService;
    @Mock private ExecutorServiceManager mockExecutorServiceManager;

    @Spy
    @InjectMocks private GameProcessMonitor gameProcessMonitor;

    @Test
    public void GIVEN_nothing_WHEN_start_THEN_taskSubmittedToExecutor() {
        // GIVEN / WHEN
        gameProcessMonitor.start();

        // THEN
        verify(mockExecutorService).scheduleWithFixedDelay(
                any(Runnable.class), eq(0L), eq(1L), any());
    }

    @Test
    public void GIVEN_nothing_WHEN_shutdown_THEN_executorServiceManagerShutdownByName() {
        // GIVEN / WHEN
        gameProcessMonitor.shutdown();

        // THEN
        verify(mockExecutorServiceManager).shutdownScheduledThreadPoolExecutorServiceByName(GameProcessMonitor.class.getSimpleName());
    }

    @Test
    public void GIVEN_multipleProcessConfigsToLaunch_WHEN_runProcessMonitor_THEN_launchesAllProcesses()
            throws AgentException {
        // GIVEN
        int concurrentExecutionsForConfig1 = 5;
        int concurrentExecutionsForConfig2 = 10;
        long currentRunningProcessesForConfig1 = 3L;
        long currentRunningProcessesForConfig2 = 6L;
        int expectedNewProcessesForConfig1 = (int) (concurrentExecutionsForConfig1 - currentRunningProcessesForConfig1);
        int expectedNewProcessesForConfig2 = (int) (concurrentExecutionsForConfig2 - currentRunningProcessesForConfig2);

        GameProcessConfiguration testProcessConfig1 = GameProcessConfiguration.builder()
                .launchPath("testExecutable1")
                .concurrentExecutions(concurrentExecutionsForConfig1)
                .build();
        GameProcessConfiguration testProcessConfig2 = GameProcessConfiguration.builder()
                .launchPath("testExecutable2")
                .concurrentExecutions(concurrentExecutionsForConfig2)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
            .serverProcesses(List.of(testProcessConfig1, testProcessConfig2))
            .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(testProcessConfig1, currentRunningProcessesForConfig1,
                                   testProcessConfig2, currentRunningProcessesForConfig2));
        doReturn(RandomStringUtils.random(10)).when(mockGameProcessManager).startProcessFromConfiguration(any());
        // all processes are alive
        doReturn(true).when(mockGameProcessManager).isProcessAlive(anyString());


        // WHEN
        gameProcessMonitor.runProcessMonitor();

        // THEN
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig1)).startProcessWithDelay(eq(testProcessConfig1), anyLong());
        verify(mockGameProcessManager, times(expectedNewProcessesForConfig2)).startProcessFromConfiguration(testProcessConfig2);
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig2)).startProcessWithDelay(eq(testProcessConfig2), anyLong());
        verifyNoMoreInteractions(mockGameProcessManager);
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig1 + expectedNewProcessesForConfig2)).getNextDelay(anyLong(), eq(true));
        //        verifyNoMoreInteractions(mockGameProcessManager);
    }

    @Test
    public void GIVEN_onlyOneConfigNeedsProcesses_WHEN_runTask_THEN_launchesProcessesForConfig()
            throws AgentException {
        // GIVEN
        int concurrentExecutionsForConfig1 = 5;
        int concurrentExecutionsForConfig2 = 10;
        Long currentRunningProcessesForConfig1 = 5L;
        long currentRunningProcessesForConfig2 = 6L;
        int expectedNewProcessesForConfig2 = (int) (concurrentExecutionsForConfig2 - currentRunningProcessesForConfig2);

        GameProcessConfiguration testProcessConfig1 = GameProcessConfiguration.builder()
                .launchPath("testExecutable1")
                .concurrentExecutions(concurrentExecutionsForConfig1)
                .build();
        GameProcessConfiguration testProcessConfig2 = GameProcessConfiguration.builder()
                .launchPath("testExecutable2")
                .concurrentExecutions(concurrentExecutionsForConfig2)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
            .serverProcesses(List.of(testProcessConfig1, testProcessConfig2))
            .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(testProcessConfig1, currentRunningProcessesForConfig1,
                                   testProcessConfig2, currentRunningProcessesForConfig2));
        doReturn(RandomStringUtils.random(10)).when(mockGameProcessManager).startProcessFromConfiguration(any());
        // all processes are alive
        doReturn(true).when(mockGameProcessManager).isProcessAlive(anyString());


        // WHEN
        gameProcessMonitor.runProcessMonitor();

        // THEN
        verify(gameProcessMonitor, never()).startProcessWithDelay(eq(testProcessConfig1), anyLong());
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig2))
                .startProcessWithDelay(eq(testProcessConfig2),
                        anyLong());
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig2)).getNextDelay(anyLong(), eq(true));
    }

    @Test
    public void GIVEN_notEnoughTotalProcessVacancies_WHEN_runTask_THEN_onlyScalesUpToVacantProcesses()
            throws AgentException {
        // GIVEN
        int concurrentExecutionsForCurrentConfig = 9;
        int concurrentExecutionsForNewConfig = 10;
        long currentRunningProcessesForCurrentConfig = 8L;
        int expectedNewProcesses = (int) (concurrentExecutionsForNewConfig - currentRunningProcessesForCurrentConfig);

        GameProcessConfiguration currentProcessConfig = GameProcessConfiguration.builder()
                .launchPath("currentExecutable")
                .concurrentExecutions(concurrentExecutionsForCurrentConfig)
                .build();
        GameProcessConfiguration newProcessConfig = GameProcessConfiguration.builder()
                .launchPath("newExecutable")
                .concurrentExecutions(concurrentExecutionsForNewConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
            .serverProcesses(List.of(newProcessConfig))
            .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(currentProcessConfig, currentRunningProcessesForCurrentConfig));
        doReturn(RandomStringUtils.random(10)).when(mockGameProcessManager).startProcessFromConfiguration(any());
        // all processes are alive
        doReturn(true).when(mockGameProcessManager).isProcessAlive(anyString());

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        // THEN
        verify(gameProcessMonitor, times(expectedNewProcesses))
                .startProcessWithDelay(eq(newProcessConfig), anyLong());
        verify(gameProcessMonitor, times(expectedNewProcesses)).getNextDelay(anyLong(), anyBoolean());
    }

    @Test
    public void GIVEN_noProcessVacancies_WHEN_runTask_THEN_doesNotScaleUpProcesses() {
        // GIVEN
        int concurrentExecutionsForCurrentConfig = 10;
        int concurrentExecutionsForNewConfig = 9;
        Long currentRunningProcessesForCurrentConfig = 10L;

        GameProcessConfiguration currentProcessConfig = GameProcessConfiguration.builder()
                .launchPath("currentExecutable")
                .concurrentExecutions(concurrentExecutionsForCurrentConfig)
                .build();
        GameProcessConfiguration newProcessConfig = GameProcessConfiguration.builder()
                .launchPath("newExecutable")
                .concurrentExecutions(concurrentExecutionsForNewConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
            .serverProcesses(List.of(newProcessConfig))
            .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(currentProcessConfig, currentRunningProcessesForCurrentConfig));

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        // THEN
        verifyNoMoreInteractions(mockGameProcessManager);
    }

    @Test
    public void GIVEN_terminatingCompute_WHEN_runTask_THEN_noOp() {
        // GIVEN
        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(true);

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        // THEN
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }

    @Test
    public void GIVEN_initializingCompute_WHEN_runTask_THEN_noOp() {
        // GIVEN
        when(mockStateManager.isComputeInitializing()).thenReturn(true);

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        // THEN
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }

    @Test
    public void GIVEN_zeroConcurrentExecutions_WHEN_runProcessMonitor_THEN_throwIllegalArgumentException() {
        // GIVEN
        int concurrentExecutionsForConfig = 0;

        GameProcessConfiguration testProcessConfig = GameProcessConfiguration.builder()
                .launchPath("testExecutable1")
                .concurrentExecutions(concurrentExecutionsForConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
                .serverProcesses(List.of(testProcessConfig))
                .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockGameProcessManager.getProcessCountsByConfiguration()).thenReturn(ImmutableMap.of());
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        // THEN
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }

    @Test
    public void GIVEN_InvalidLaunchPath_WHEN_runProcessMonitor_THEN_handleException() throws AgentException {
        // GIVEN
        int concurrentExecutionsForConfig = 5;
        long currentRunningProcessesForConfig = 3L;
        int expectedNewProcessesForConfig = (int) (concurrentExecutionsForConfig - currentRunningProcessesForConfig);

        GameProcessConfiguration testProcessConfig = GameProcessConfiguration.builder()
                .launchPath(INVALID_LAUNCH_PATH)
                .concurrentExecutions(concurrentExecutionsForConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
                .serverProcesses(List.of(testProcessConfig))
                .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(testProcessConfig, currentRunningProcessesForConfig));
        doThrow(RuntimeException.class).when(mockGameProcessManager)
                .startProcessFromConfiguration(testProcessConfig);

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        //THEN
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig))
                .startProcessWithDelay(eq(testProcessConfig), anyLong());
        verify(mockGameProcessManager, times(expectedNewProcessesForConfig)).startProcessFromConfiguration(testProcessConfig);
        verify(gameProcessMonitor, never()).getNextDelay(anyLong(), anyBoolean());
        verify(mockGameProcessManager, never()).isProcessAlive(anyString());
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }

    @Test
    public void GIVEN_processThrowsRuntimeException_WHEN_runProcessMonitor_THEN_handleException() throws AgentException {
        // GIVEN
        int concurrentExecutionsForConfig = 5;
        long currentRunningProcessesForConfig = 3L;
        int expectedNewProcessesForConfig = (int) (concurrentExecutionsForConfig - currentRunningProcessesForConfig);

        GameProcessConfiguration testProcessConfig = GameProcessConfiguration.builder()
                .launchPath("testExecutable")
                .concurrentExecutions(concurrentExecutionsForConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
                .serverProcesses(List.of(testProcessConfig))
                .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(testProcessConfig, currentRunningProcessesForConfig));
        doThrow(RuntimeException.class).when(mockGameProcessManager).
                startProcessFromConfiguration(testProcessConfig);

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        //THEN
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig))
                .startProcessWithDelay(eq(testProcessConfig), anyLong());
        verify(mockGameProcessManager, times(expectedNewProcessesForConfig)).startProcessFromConfiguration(any());
        // next 2 calls are never invoked because a successful execution was never received
        verify(gameProcessMonitor, never()).getNextDelay(anyLong(), anyBoolean());
        verify(mockGameProcessManager, never()).isProcessAlive(anyString());
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }

    @Test
    public void GIVEN_processThrowsBadExecutablePathException_WHEN_runProcessMonitor_THEN_handleException() throws AgentException {
        // GIVEN
        int concurrentExecutionsForConfig = 5;
        long currentRunningProcessesForConfig = 3L;
        int expectedNewProcessesForConfig = (int) (concurrentExecutionsForConfig - currentRunningProcessesForConfig); //2

        GameProcessConfiguration testProcessConfig = GameProcessConfiguration.builder()
                .launchPath("testExecutable")
                .concurrentExecutions(concurrentExecutionsForConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
                .serverProcesses(List.of(testProcessConfig))
                .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockStateManager.isComputeInitializing()).thenReturn(false);
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(testProcessConfig, currentRunningProcessesForConfig));
        doThrow(BadExecutablePathException.class).when(mockGameProcessManager)
                .startProcessFromConfiguration(testProcessConfig);

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        //THEN
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig))
                .startProcessWithDelay(eq(testProcessConfig), anyLong());
        verify(mockGameProcessManager, times(expectedNewProcessesForConfig)).startProcessFromConfiguration(any());
        // next 2 calls are never invoked because a successful execution was never received
        verify(gameProcessMonitor, never()).getNextDelay(anyLong(), anyBoolean());
        verify(mockGameProcessManager, never()).isProcessAlive(anyString());
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }

    @Test
    public void GIVEN_processfailsToStartSilently_WHEN_runProcessMonitor_THEN_handleException() throws
            AgentException {
        // GIVEN
        final String testProcessId = "testProcessId";
        int concurrentExecutionsForConfig = 5;
        long currentRunningProcessesForConfig = 3L;
        int expectedNewProcessesForConfig = (int) (concurrentExecutionsForConfig - currentRunningProcessesForConfig);

        GameProcessConfiguration testProcessConfig = GameProcessConfiguration.builder()
                .launchPath("testExecutable")
                .concurrentExecutions(concurrentExecutionsForConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
                .serverProcesses(List.of(testProcessConfig))
                .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(testProcessConfig, currentRunningProcessesForConfig));
        doReturn(testProcessId).when(mockGameProcessManager).startProcessFromConfiguration(any());
        // nmumber of processes stays at 0
        doReturn(false).when(mockGameProcessManager).isProcessAlive(testProcessId);

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        //THEN
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig))
                .startProcessWithDelay(eq(testProcessConfig), eq(TEST_INITIAL_PROCESS_LAUNCH_DELAY_SECONDS));
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig)).getNextDelay(eq(TEST_INITIAL_PROCESS_LAUNCH_DELAY_SECONDS), anyBoolean());
        verify(mockGameProcessManager, times(expectedNewProcessesForConfig)).isProcessAlive(anyString());
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }

    @Test
    public void GIVEN_processfailsToStartWithNfe_WHEN_runProcessMonitor_THEN_handleException() throws
            AgentException {
        // GIVEN
        final String testProcessId = "testProcessId";
        int concurrentExecutionsForConfig = 5;
        long currentRunningProcessesForConfig = 3L;
        int expectedNewProcessesForConfig = (int) (concurrentExecutionsForConfig - currentRunningProcessesForConfig);

        GameProcessConfiguration testProcessConfig = GameProcessConfiguration.builder()
                .launchPath("testExecutable")
                .concurrentExecutions(concurrentExecutionsForConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
                .serverProcesses(List.of(testProcessConfig))
                .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs()).thenReturn(ImmutableSet.of());
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(testProcessConfig, currentRunningProcessesForConfig));
        doReturn(testProcessId).when(mockGameProcessManager).startProcessFromConfiguration(any());
        // number of processes stays at 0
        doThrow(NotFoundException.class).when(mockGameProcessManager).isProcessAlive(testProcessId);

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        //THEN
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig))
                .startProcessWithDelay(eq(testProcessConfig), eq(TEST_INITIAL_PROCESS_LAUNCH_DELAY_SECONDS));
        verify(gameProcessMonitor, times(expectedNewProcessesForConfig)).getNextDelay(eq(TEST_INITIAL_PROCESS_LAUNCH_DELAY_SECONDS), anyBoolean());
        verify(mockGameProcessManager, times(expectedNewProcessesForConfig)).isProcessAlive(anyString());
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }

    @Test
    public void GIVEN_processesTimedOutForInitialization_WHEN_runProcessMonitor_THEN_terminatesProcesses() {
        // GIVEN
        final String testProcessId1 = "test1";
        final String testProcessId2 = "test2";
        int concurrentExecutionsForConfig = 5;
        Long currentRunningProcessesForConfig = 5L;

        GameProcessConfiguration testProcessConfig = GameProcessConfiguration.builder()
                .launchPath("testExecutable")
                .concurrentExecutions(concurrentExecutionsForConfig)
                .build();
        RuntimeConfiguration testConfig = RuntimeConfiguration.builder()
                .serverProcesses(List.of(testProcessConfig))
                .build();

        when(mockStateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        when(mockRuntimeConfigurationManager.getRuntimeConfiguration()).thenReturn(testConfig);
        when(mockGameProcessManager.getInitializationTimedOutProcessUUIDs())
                .thenReturn(ImmutableSet.of(testProcessId1, testProcessId2));
        when(mockGameProcessManager.getProcessCountsByConfiguration())
                .thenReturn(Map.of(testProcessConfig, currentRunningProcessesForConfig));

        // WHEN
        gameProcessMonitor.runProcessMonitor();

        //THEN
        verify(mockGameProcessManager).terminateProcessByUUID(testProcessId1, ProcessTerminationReason.SERVER_PROCESS_SDK_INITIALIZATION_TIMEOUT);
        verify(mockGameProcessManager).terminateProcessByUUID(testProcessId2, ProcessTerminationReason.SERVER_PROCESS_SDK_INITIALIZATION_TIMEOUT);
        verifyNoMoreInteractions(mockGameProcessManager, mockRuntimeConfigurationManager);
    }
}
