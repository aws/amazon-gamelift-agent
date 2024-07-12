/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.client.AmazonGameLiftClientWrapper;
import com.amazon.gamelift.agent.logging.GameLiftAgentLogUploader;
import com.amazon.gamelift.agent.model.ComputeStatus;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.NotFinishedException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.amazon.gamelift.agent.process.GameProcessMonitor;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;

import com.amazonaws.services.gamelift.model.DeregisterComputeRequest;
import com.amazonaws.services.gamelift.model.DeregisterComputeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShutdownOrchestratorTest {

    @Mock
    private StateManager stateManager;
    @Mock
    private HeartbeatSender heartbeatSender;
    @Mock
    private GameProcessManager gameProcessManager;
    @Mock
    private GameProcessMonitor gameProcessMonitor;
    @Mock
    private WebSocketConnectionProvider webSocketConnectionProvider;
    @Mock
    private GameLiftAgentLogUploader gameLiftAgentLogUploader;
    @Mock
    private AmazonGameLiftClientWrapper amazonGameLift;
    @Mock
    private ScheduledExecutorService executorService;
    @Mock
    private ExecutorServiceManager mockExecutorServiceManager;

    private static final boolean IS_NOT_CONTAINER_FLEET = false;
    private static final boolean ENABLED_REGISTRATION_FALSE = false;
    private static final boolean ENABLED_REGISTRATION = true;
    private static final boolean IS_CONTAINER_FLEET = true;
    private static final String FLEET_ID = "testFleetId";
    private static final String COMPUTE_ID = "testComputeName";
    private static final DeregisterComputeResult testDeregisterComputeResult
            = new DeregisterComputeResult();
    private static final DeregisterComputeRequest expectedDeregisterComputeRequest
            = new DeregisterComputeRequest().withFleetId(FLEET_ID).withComputeName(COMPUTE_ID);
    private ShutdownOrchestrator shutdownOrchestrator;

    @Test
    public void GIVEN_computeActive_WHEN_startTermination_THEN_startsDelayedTermination() throws Exception{
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), false);

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verify(stateManager).reportComputeTerminating();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any());
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_computeActive_WHEN_startTerminationWithInterrupt_THEN_sendsHeartbeatStartsDelayedTermination()
            throws Exception{
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), true);

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verify(stateManager).reportComputeInterrupted();
        verify(heartbeatSender).sendHeartbeat();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any());
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_computeActive_WHEN_startTerminationInPast_THEN_startsImmediateTermination()
            throws Exception{
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.startTermination(Instant.now().minus(Duration.ofMinutes(1)), false);

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verify(stateManager).reportComputeTerminating();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).execute(any());
    }

    @Test
    public void GIVEN_computeTerminated_WHEN_startTermination_THEN_noop() throws Exception{
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Terminated);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), false);

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verify(stateManager, never()).reportComputeTerminating();
        verifyNoInteractions(executorService, gameProcessMonitor);
    }

    @Test
    public void GIVEN_processesActive_WHEN_validateSafeTermination_THEN_noOp() throws Exception{
        // GIVEN
        when(gameProcessManager.getAllProcessUUIDs()).thenReturn(Set.of("FakeProcessUUID"));

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.validateSafeTermination();

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verifyNoMoreInteractions(gameProcessManager, stateManager, executorService,
                heartbeatSender, webSocketConnectionProvider);
    }

    @Test
    public void GIVEN_noProcessesAndComputeActive_WHEN_validateSafeTermination_THEN_terminatesProcessManager()
            throws Exception {
        // GIVEN
        when(gameProcessManager.getAllProcessUUIDs()).thenReturn(Set.of());
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.validateSafeTermination();

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_computeActive_WHEN_completeTermination_THEN_terminatesProcessManager()
            throws Exception {
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.completeTermination();

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_incompleteProcessTermination_WHEN_completeTermination_THEN_terminatesProcessManager()
            throws Exception {
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);
        doThrow(NotFinishedException.class).when(gameProcessManager)
                .terminateAllProcessesForShutdown(anyLong(), anyLong());

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.completeTermination();

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_computeTerminated_WHEN_completeTermination_THEN_terminatesProcessManager()
            throws Exception {
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Terminated);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_NOT_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.completeTermination();

        // THEN
        verifyNoInteractions(amazonGameLift);
        verify(amazonGameLift, times(0)).deregisterCompute(any());
        verify(stateManager, never()).reportComputeTerminated();
        verify(gameProcessManager, never()).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender, never()).sendHeartbeat();
        verify(webSocketConnectionProvider, never()).closeAllConnections();
        verify(mockExecutorServiceManager, never()).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader, never()).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_startTermination_THEN_success() throws Exception{
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), false);

        // THEN
        verify(amazonGameLift, never()).deregisterCompute(any());
        verify(stateManager).reportComputeTerminating();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any());
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_ContainerFleetShouldEnableRegistration_WHEN_startTermination_THEN_enableDeregistration() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(expectedDeregisterComputeRequest)).thenReturn(testDeregisterComputeResult);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), false);

        // THEN
        verify(amazonGameLift).deregisterCompute(expectedDeregisterComputeRequest);
        verify(stateManager).reportComputeTerminating();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any());
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_completeTermination_THEN_success() throws Exception{
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION_FALSE);
        shutdownOrchestrator.completeTermination();

        // THEN
        verify(amazonGameLift, never()).deregisterCompute(any());
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_ContainerFleetShouldEnableRegistration_WHEN_completeTermination_THEN_success() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(expectedDeregisterComputeRequest)).thenReturn(testDeregisterComputeResult);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.completeTermination();

        // THEN
        verify(amazonGameLift).deregisterCompute(expectedDeregisterComputeRequest);
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_startTermination_THEN_NotFoundException() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(any())).thenThrow(NotFoundException.class);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), false);

        // THEN
        verify(amazonGameLift).deregisterCompute(any());
        verify(stateManager).reportComputeTerminating();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any());
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_completeTermination_THEN_NotFoundException() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(any())).thenThrow(NotFoundException.class);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.completeTermination();

        // THEN
        verify(amazonGameLift).deregisterCompute(any());
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_startTermination_THEN_UnauthorizedException() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(any())).thenThrow(UnauthorizedException.class);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), false);

        // THEN
        verify(amazonGameLift).deregisterCompute(any());
        verify(stateManager).reportComputeTerminating();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any());
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_completeTermination_THEN_UnauthorizedException() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(any())).thenThrow(UnauthorizedException.class);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.completeTermination();

        // THEN
        verify(amazonGameLift).deregisterCompute(any());
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_startTermination_THEN_InvalidRequestException() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(any())).thenThrow(InvalidRequestException.class);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), false);

        // THEN
        verify(amazonGameLift).deregisterCompute(any());
        verify(stateManager).reportComputeTerminating();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any());
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_completeTermination_THEN_InvalidRequestException() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(any())).thenThrow(InvalidRequestException.class);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.completeTermination();

        // THEN
        verify(amazonGameLift).deregisterCompute(any());
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_startTermination_THEN_InternalServiceException() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(any())).thenThrow(InternalServiceException.class);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.startTermination(Instant.now().plus(Duration.ofMinutes(1)), false);

        // THEN
        verify(amazonGameLift).deregisterCompute(any());
        verify(stateManager).reportComputeTerminating();
        verify(gameProcessMonitor).shutdown();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any());
        verify(executorService).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_ContainerFleet_WHEN_completeTermination_THEN_InternalServiceException() throws Exception{
        // GIVEN
        when(amazonGameLift.deregisterCompute(any())).thenThrow(InternalServiceException.class);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        this.shutdownOrchestrator = new ShutdownOrchestrator(
                stateManager, heartbeatSender, gameProcessManager, gameProcessMonitor, webSocketConnectionProvider,
                gameLiftAgentLogUploader, amazonGameLift, executorService, mockExecutorServiceManager, IS_CONTAINER_FLEET,
                FLEET_ID, COMPUTE_ID, ENABLED_REGISTRATION);
        shutdownOrchestrator.completeTermination();

        // THEN
        verify(amazonGameLift).deregisterCompute(any());
        verify(stateManager).reportComputeTerminated();
        verify(gameProcessManager).terminateAllProcessesForShutdown(anyLong(), anyLong());
        verify(heartbeatSender).sendHeartbeat();
        verify(webSocketConnectionProvider).closeAllConnections();
        verify(mockExecutorServiceManager).shutdownExecutorServices();
        verify(gameLiftAgentLogUploader).shutdownAndUploadLogs();
    }
}