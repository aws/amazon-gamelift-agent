/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WebSocketConnectionProviderTest {
    @Mock
    private ScheduledExecutorService connectionCloserService;

    private WebSocketConnectionProvider webSocketConnectionProvider;

    @BeforeEach
    public void setup() {
        webSocketConnectionProvider = new WebSocketConnectionProvider(connectionCloserService);
    }

    @Test
    public void GIVEN_connection_WHEN_updateConnection_THEN_storesConnection() {
        // GIVEN
        final AgentWebSocket client = mock(AgentWebSocket.class);

        // WHEN
        webSocketConnectionProvider.updateConnection(client);

        // THEN
        assertEquals(webSocketConnectionProvider.getCurrentConnection(), client);
    }

    @Test
    public void GIVEN_existingConnection_WHEN_updateConnection_THEN_storesNewConnectionAndSchedulesClosingOldOne() {
        // GIVEN
        final AgentWebSocket oldConnection = mock(AgentWebSocket.class);
        final AgentWebSocket newConnection = mock(AgentWebSocket.class);
        webSocketConnectionProvider.updateConnection(oldConnection);

        // WHEN
        webSocketConnectionProvider.updateConnection(newConnection);

        // THEN
        assertEquals(webSocketConnectionProvider.getCurrentConnection(), newConnection);
        verify(connectionCloserService, times(2)).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    public void GIVEN_agentShutsDown_WHEN_updateConnection_THEN_closesAllConnections() throws InterruptedException {
        // GIVEN
        final AgentWebSocket oldConnection = mock(AgentWebSocket.class);
        final AgentWebSocket newConnection = mock(AgentWebSocket.class);
        webSocketConnectionProvider.updateConnection(oldConnection);
        webSocketConnectionProvider.updateConnection(newConnection);
        when(connectionCloserService.shutdownNow()).thenReturn(ImmutableList.of());

        // WHEN
        webSocketConnectionProvider.closeAllConnections();

        // THEN
        assertNull(webSocketConnectionProvider.getCurrentConnection());
        verify(connectionCloserService).shutdownNow();
        verify(newConnection).closeConnection(Duration.ofMinutes(1));
    }
}
