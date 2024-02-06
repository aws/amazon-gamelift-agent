/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.websocket.RefreshConnectionMessage;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Lazy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RefreshConnectionHandlerTest {
    private static final String REFRESH_WEB_SOCKET_ENDPOINT = "endpoint";
    private static final String REFRESH_WEB_SOCKET_AUTH_TOKEN = "newAuthToken";

    @Mock
    private WebSocketConnectionManager connectionManager;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Lazy<WebSocketConnectionManager> lazyConnectionManager;

    @InjectMocks
    private RefreshConnectionHandler connectionHandler;

    private final RefreshConnectionMessage input = new RefreshConnectionMessage();

    @BeforeEach
    public void setup() {
        when(lazyConnectionManager.get()).thenReturn(connectionManager);
        input.setAuthToken(REFRESH_WEB_SOCKET_AUTH_TOKEN);
        input.setRefreshConnectionEndpoint(REFRESH_WEB_SOCKET_ENDPOINT);
    }

    @Test
    public void GIVEN_input_WHEN_handle_THEN_refreshConnection() {
        // WHEN
        connectionHandler.handle(input);

        // THEN
        verify(connectionManager).reconnect(input);
    }

    @Test
    public void GIVEN_input_WHEN_handleFails_THEN_doesntThrow() {
        // GIVEN
        doThrow(RuntimeException.class).when(connectionManager).reconnect(input);

        // WHEN
        connectionHandler.handle(input);

        // THEN
        verify(connectionManager).reconnect(input);
    }
}
