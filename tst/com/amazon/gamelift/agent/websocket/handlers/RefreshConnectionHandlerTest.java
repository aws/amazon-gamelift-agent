/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.websocket.RefreshConnectionMessage;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dagger.Lazy;

@ExtendWith(MockitoExtension.class)
public class RefreshConnectionHandlerTest {
    private static final String REFRESH_WEB_SOCKET_ENDPOINT = "endpoint";
    private static final String REFRESH_WEB_SOCKET_AUTH_TOKEN = "newAuthToken";

    @Mock
    private WebSocketConnectionManager connectionManager;
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
    public void GIVEN_input_WHEN_handle_THEN_refreshConnection() throws InternalServiceException {
        // WHEN
        connectionHandler.handle(input);

        // THEN
        verify(connectionManager).refreshWebSocketConnection(input);
    }

    @Test
    public void GIVEN_input_WHEN_handleFails_THEN_doesntThrow() throws InternalServiceException {
        // GIVEN
        doThrow(RuntimeException.class).when(connectionManager).refreshWebSocketConnection(input);

        // WHEN
        connectionHandler.handle(input);

        // THEN
        verify(connectionManager).refreshWebSocketConnection(input);
    }
}
