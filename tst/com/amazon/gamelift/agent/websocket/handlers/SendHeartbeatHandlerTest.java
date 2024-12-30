/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.ComputeStatus;
import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.websocket.SendHeartbeatResponse;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.amazon.gamelift.agent.manager.ShutdownOrchestrator;
import com.amazon.gamelift.agent.manager.StateManager;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SendHeartbeatHandlerTest {
    private static final String UNHEALTHY_PROCESS = "1234";
    private static final String UNREGISTERED_PROCESS = "5678";
    private static final String ALWAYS_UNREGISTERED_PROCESS = "0012";

    @Mock private StateManager stateManager;
    @Mock private ShutdownOrchestrator shutdownOrchestrator;
    @Mock private GameProcessManager gameProcessManager;

    @InjectMocks private SendHeartbeatHandler sendHeartbeatHandler;

    private final SendHeartbeatResponse response = new SendHeartbeatResponse();

    @BeforeEach
    public void setup() {
        when(stateManager.isComputeTerminatingOrTerminated()).thenReturn(false);
        response.setStatus(ComputeStatus.Active.name());
        response.setUnhealthyProcesses(Collections.emptyList());
        response.setUnregisteredProcesses(Collections.emptyList());
    }

    @Test
    public void GIVEN_unhealthyProcess_WHEN_handle_THEN_terminatesProcesses() {
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);
        response.setUnhealthyProcesses(ImmutableList.of(UNHEALTHY_PROCESS));

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verify(gameProcessManager).terminateProcessByUUID(UNHEALTHY_PROCESS,
                ProcessTerminationReason.SERVER_PROCESS_TERMINATED_UNHEALTHY);
    }

    @Test
    public void GIVEN_nullUnhealthyProcess_WHEN_handle_THEN_doNothing() {
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);
        response.setUnhealthyProcesses(null);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verify(gameProcessManager, never()).terminateProcessByUUID(any(), any());
    }

    @Test
    public void GIVEN_unregisteredProcessForOneIteration_WHEN_handle_THEN_doesNotTerminateProcess() {
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);
        response.setUnregisteredProcesses(ImmutableList.of(UNREGISTERED_PROCESS));

        // WHEN
        sendHeartbeatHandler.handle(response);

        // GIVEN
        response.setUnregisteredProcesses(Collections.emptyList());

        // WHEN
        for (int i = 0; i < SendHeartbeatHandler.MAX_UNREGISTERED_PROCESS_HEARTBEAT_COUNT; i++) {
            sendHeartbeatHandler.handle(response);
        }

        // THEN
        verify(gameProcessManager, never()).terminateProcessByUUID(any(), any());
    }

    @Test
    public void GIVEN_unregisteredProcessForAllIterations_WHEN_handle_THEN_terminatesProcess() {
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);
        response.setUnregisteredProcesses(ImmutableList.of(ALWAYS_UNREGISTERED_PROCESS));

        // WHEN
        for (int i = 0; i < SendHeartbeatHandler.MAX_UNREGISTERED_PROCESS_HEARTBEAT_COUNT; i++) {
            sendHeartbeatHandler.handle(response);
        }

        // THEN
        verify(gameProcessManager).terminateProcessByUUID(ALWAYS_UNREGISTERED_PROCESS,
                ProcessTerminationReason.SERVER_PROCESS_SDK_INITIALIZATION_TIMEOUT);
    }

    @Test
    public void GIVEN_internalStateTerminatingAndWWStateActive_WHEN_handle_THEN_doesNotUpdateState() {
        // GIVEN
        response.setStatus(ComputeStatus.Active.name());
        when(stateManager.isComputeTerminatingOrTerminated()).thenReturn(true);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verifyNoMoreInteractions(stateManager, shutdownOrchestrator, gameProcessManager);
    }

    @Test
    public void GIVEN_bothStatusesActivating_WHEN_handle_THEN_doesNotUpdateState() {
        // GIVEN
        response.setStatus(ComputeStatus.Activating.name());
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Activating);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verifyNoMoreInteractions(stateManager, shutdownOrchestrator, gameProcessManager);
    }

    @Test
    public void GIVEN_bothStatusesInitializing_WHEN_handle_THEN_updatesToActivating() {
        // GIVEN
        response.setStatus(ComputeStatus.Initializing.name());
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Initializing);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verify(stateManager).reportComputeActivating();
        verifyNoMoreInteractions(stateManager, shutdownOrchestrator, gameProcessManager);
    }

    @Test
    public void GIVEN_internalStateActiveAndWWStateTerminated_WHEN_handle_THEN_attemptsImmediateTermination() {
        // GIVEN
        response.setStatus(ComputeStatus.Terminated.name());
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verify(shutdownOrchestrator).completeTermination();
        verifyNoMoreInteractions(stateManager, shutdownOrchestrator, gameProcessManager);
    }

    @Test
    public void GIVEN_internalStateActiveAndWWStateTerminating_WHEN_handle_THEN_schedulesTermination() {
        // GIVEN
        response.setStatus(ComputeStatus.Terminating.name());
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Active);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verify(shutdownOrchestrator).startTermination(any(Instant.class), eq(false));
        verifyNoMoreInteractions(stateManager, shutdownOrchestrator, gameProcessManager);
    }

    @Test
    public void GIVEN_internalStateInitializingAndWWStateActive_WHEN_handle_THEN_updatesToActive() {
        // GIVEN
        response.setStatus(ComputeStatus.Active.name());
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Initializing);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verify(stateManager).reportComputeActive();
        verifyNoMoreInteractions(stateManager, shutdownOrchestrator, gameProcessManager);
    }

    @Test
    public void GIVEN_internalStateActivatingAndWWStateActive_WHEN_handle_THEN_updatesToActive() {
        // GIVEN
        response.setStatus(ComputeStatus.Active.name());
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Activating);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verify(stateManager).reportComputeActive();
        verifyNoMoreInteractions(stateManager, shutdownOrchestrator, gameProcessManager);
    }

    @Test
    public void GIVEN_emptyComputeStatus_WHEN_handle_THEN_doesNothing() {
        // GIVEN
        response.setStatus(null);
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Initializing);

        // WHEN
        sendHeartbeatHandler.handle(response);

        // THEN
        verifyNoMoreInteractions(stateManager, shutdownOrchestrator, gameProcessManager);
    }
}
