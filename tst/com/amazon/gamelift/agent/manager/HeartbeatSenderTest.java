/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.ComputeStatus;
import com.amazon.gamelift.agent.model.websocket.SendHeartbeatRequest;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketRequest;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.amazon.gamelift.agent.websocket.AgentWebSocket;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HeartbeatSenderTest {
    private static final ComputeStatus COMPUTE_STATUS = ComputeStatus.Activating;
    private static final String PROCESS_ID = "1234-5678-abcd-efgh";

    @Mock
    private StateManager stateManager;
    @Mock
    private WebSocketConnectionProvider webSocketConnectionProvider;
    @Mock
    private GameProcessManager gameProcessManager;
    @Mock
    private AgentWebSocket client;
    @Mock
    private ScheduledExecutorService executorService;

    @InjectMocks
    private HeartbeatSender heartbeatSender;

    @BeforeEach
    public void setup() {
        lenient().when(stateManager.getComputeStatus()).thenReturn(COMPUTE_STATUS);
        lenient().when(webSocketConnectionProvider.getCurrentConnection()).thenReturn(client);
        lenient().when(gameProcessManager.getAllProcessUUIDs()).thenReturn(ImmutableSet.of(PROCESS_ID));
    }

    @Test
    public void GIVEN_executor_WHEN_start_THEN_schedulesHeartbeats() {
        // WHEN
        heartbeatSender.start();

        // THEN
        verify(executorService).scheduleWithFixedDelay(any(), eq(0L), anyLong(), any());
    }

    @Test
    public void GIVEN_processes_WHEN_heartbeat_THEN_sendsMessage() {
        // WHEN
        heartbeatSender.sendHeartbeat();

        // THEN
        verify(client).sendRequestAsync(expectedHeartbeatRequest(false));
    }

    @Test
    public void GIVEN_sendingMessageThrows_WHEN_heartbeat_THEN_swallowsException() {
        // GIVEN
        doThrow(RuntimeException.class).when(client).sendRequestAsync(any());

        // WHEN
        heartbeatSender.sendHeartbeat();

        // THEN
        verify(client).sendRequestAsync(expectedHeartbeatRequest(false));
    }

    @Test
    public void GIVEN_spotInterruption_WHEN_sendHeartbeatSpotInterrupted_THEN_sendsHeartbeat() {
        // GIVEN
        when(stateManager.getComputeStatus()).thenReturn(ComputeStatus.Interrupted);

        // WHEN
        heartbeatSender.sendHeartbeat();

        // THEN
        verify(client).sendRequestAsync(expectedHeartbeatRequest(true));
    }

    private WebsocketRequest expectedHeartbeatRequest(final boolean spotInterrupted) {
        return argThat(it -> {
            final SendHeartbeatRequest request = (SendHeartbeatRequest) it;
            if (spotInterrupted) {
                return request.getProcessList().equals(ImmutableList.of(PROCESS_ID)) &&
                        request.getStatus().equals(ComputeStatus.Interrupted.toString()) &&
                        request.getHeartbeatTimeMillis() > 1;
            } else {
                return request.getProcessList().equals(ImmutableList.of(PROCESS_ID)) &&
                        request.getStatus().equals(COMPUTE_STATUS.toString()) &&
                        request.getHeartbeatTimeMillis() > 1;
            }
        });
    }
}
