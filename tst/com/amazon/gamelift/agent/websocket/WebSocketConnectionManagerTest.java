/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import com.amazon.gamelift.agent.manager.StateManager;
import com.amazon.gamelift.agent.model.exception.ConflictException;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import com.amazon.gamelift.agent.model.gamelift.GetComputeAuthTokenResponse;
import com.amazon.gamelift.agent.model.gamelift.RegisterComputeResponse;
import com.amazon.gamelift.agent.model.websocket.RefreshConnectionMessage;
import com.amazon.gamelift.agent.utils.RetryHelper;
import com.amazon.gamelift.agent.websocket.handlers.MessageHandler;
import com.amazon.gamelift.agent.client.AmazonGameLiftClientWrapper;
import com.amazon.gamelift.agent.manager.ComputeAuthTokenManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WebSocketConnectionManagerTest {
    private static final String FLEET_ID = "fleetId";
    private static final String COMPUTE_NAME = "computeName";
    private static final String WEBSOCKET_IDENTIFIER = "webSocketId";
    private static final String REFRESH_WEB_SOCKET_ENDPOINT = "http://newlocalhost:8080";
    private static final String RECONNECT_WEB_SOCKET_ENDPOINT = "http://previous.localhost:8080";
    private static final String SDK_WEB_SOCKET_ENDPOINT = "http://some.endpoint:8080";
    private static final String AGENT_WEB_SOCKET_ENDPOINT = "wss://us-west-2.process-manager-api.amazongamelift.com";
    private static final String AGENT_WEB_SOCKET_ENDPOINT_OVERRIDE = "wss://alpha.us-west-2.process-manager-api.amazongamelift.com";
    private static final String REFRESH_WEB_SOCKET_AUTH_TOKEN = "newAuthToken";
    private static final String RECONNECT_WEB_SOCKET_AUTH_TOKEN = "newAuthToken";
    private static final String REGION = "us-west-2";
    private static final String LOCATION = "ap-south-1";
    private static final String IP_ADDRESS = "1.2.3.4";
    private static final String CERTIFICATE_PATH = "/game/cert";
    private static final String DNS_NAME = "abc.aws";
    private static final String COMPUTE_AUTH_TOKEN = "computeAuthToken";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private AmazonGameLiftClientWrapper gameLift;
    @Mock private WebSocketConnectionProvider webSocketConnectionProvider;
    @Mock private WebSocketExceptionProvider mockWebSocketExceptionProvider;
    @Mock private Map<String, MessageHandler<?>> messageHandlers;
    @Mock private WebSocket.Builder mockWebSocketBuilder;
    @Mock private WebSocket mockJavaWebSocket;
    @Mock private AgentWebSocket mockAgentWebSocket;
    @Mock private ComputeAuthTokenManager computeAuthTokenManager;
    @Mock private SdkWebsocketEndpointProvider sdkWebsocketEndpointProvider;
    @Mock private StateManager stateManager;
    @Captor private ArgumentCaptor<AgentWebSocket> connectionCaptor;
    @Captor private ArgumentCaptor<URI> uriCaptor;

    private WebSocketConnectionManager connectionManager;

    @BeforeEach
    public void setup() throws UnauthorizedException, NotFoundException, InvalidRequestException,
            InternalServiceException, ConflictException {
        lenient().when(gameLift.getComputeAuthToken(any())).thenReturn(GetComputeAuthTokenResponse.builder()
                .fleetId(FLEET_ID)
                .computeName(COMPUTE_NAME)
                .authToken(COMPUTE_AUTH_TOKEN)
                .expirationTimeEpochMillis(Instant.now())
                .build());
        lenient().when(gameLift.registerCompute(any())).thenReturn(RegisterComputeResponse.builder()
                .fleetId(FLEET_ID)
                .computeName(COMPUTE_NAME)
                .sdkWebsocketEndpoint(SDK_WEB_SOCKET_ENDPOINT)
                .build());
        connectionManager = new WebSocketConnectionManager(gameLift, FLEET_ID, COMPUTE_NAME,
                REGION, LOCATION, IP_ADDRESS, CERTIFICATE_PATH, DNS_NAME, AGENT_WEB_SOCKET_ENDPOINT_OVERRIDE,
                webSocketConnectionProvider, sdkWebsocketEndpointProvider, mockWebSocketExceptionProvider,
                messageHandlers, OBJECT_MAPPER, mockWebSocketBuilder, computeAuthTokenManager, stateManager);
        lenient().when(mockWebSocketBuilder.buildAsync(any(URI.class), any(GameLiftAgentWebSocketListener.class)))
                .thenReturn(CompletableFuture.completedFuture(mockJavaWebSocket));
        RetryHelper.disableBackoff();
    }

    @Test
    public void GIVEN_validInputWithWebSocketEndPointOverride_WHEN_connect_THEN_handshakesAndConnects() throws RuntimeException, AgentException {
        // GIVEN
        when(computeAuthTokenManager.getComputeAuthToken()).thenReturn(COMPUTE_AUTH_TOKEN);

        // WHEN
        connectionManager.connect();

        // THEN
        verify(gameLift).registerCompute(any());
        verify(sdkWebsocketEndpointProvider).setSdkWebsocketEndpoint(SDK_WEB_SOCKET_ENDPOINT);
        verify(computeAuthTokenManager).getComputeAuthToken();

        verify(mockWebSocketBuilder).buildAsync(uriCaptor.capture(), any(GameLiftAgentWebSocketListener.class));

        String agentClientUri = uriCaptor.getAllValues().get(0).toString();
        assertTrue(agentClientUri.contains(AGENT_WEB_SOCKET_ENDPOINT_OVERRIDE));
        assertTrue(agentClientUri.contains(FLEET_ID));
        assertTrue(agentClientUri.contains(COMPUTE_NAME));
        assertTrue(agentClientUri.contains(COMPUTE_AUTH_TOKEN));

        verify(webSocketConnectionProvider).updateConnection(connectionCaptor.capture());
    }

    @Test
    public void GIVEN_validInputWithoutWebSocketEndPointOverride_WHEN_connect_THEN_handshakesAndConnects() throws RuntimeException, AgentException {
        // GIVEN
        final WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(gameLift, FLEET_ID, COMPUTE_NAME,
                REGION, LOCATION, IP_ADDRESS, CERTIFICATE_PATH, DNS_NAME, null,
                webSocketConnectionProvider, sdkWebsocketEndpointProvider, mockWebSocketExceptionProvider,
                messageHandlers, OBJECT_MAPPER, mockWebSocketBuilder, computeAuthTokenManager, stateManager);

        when(computeAuthTokenManager.getComputeAuthToken()).thenReturn(COMPUTE_AUTH_TOKEN);

        // WHEN
        connectionManager.connect();

        // THEN
        verify(gameLift).registerCompute(any());
        verify(sdkWebsocketEndpointProvider).setSdkWebsocketEndpoint(SDK_WEB_SOCKET_ENDPOINT);
        verify(computeAuthTokenManager).getComputeAuthToken();

        verify(mockWebSocketBuilder).buildAsync(uriCaptor.capture(), any(GameLiftAgentWebSocketListener.class));

        String agentClientUri = uriCaptor.getAllValues().get(0).toString();
        assertTrue(agentClientUri.contains(AGENT_WEB_SOCKET_ENDPOINT));
        assertTrue(agentClientUri.contains(FLEET_ID));
        assertTrue(agentClientUri.contains(COMPUTE_NAME));
        assertTrue(agentClientUri.contains(COMPUTE_AUTH_TOKEN));

        verify(webSocketConnectionProvider).updateConnection(connectionCaptor.capture());
    }

    @Test
    public void GIVEN_registerComputeThrowsRetryableException_WHEN_connect_THEN_retries() throws AgentException {
        // GIVEN
        when(gameLift.registerCompute(any()))
                .thenThrow(RuntimeException.class)
                .thenReturn(RegisterComputeResponse.builder()
                        .fleetId(FLEET_ID)
                        .computeName(COMPUTE_NAME)
                        .sdkWebsocketEndpoint(SDK_WEB_SOCKET_ENDPOINT)
                        .build());
        when(computeAuthTokenManager.getComputeAuthToken())
                .thenReturn(COMPUTE_AUTH_TOKEN);

        // WHEN
        connectionManager.connect();

        // THEN
        verify(gameLift, times(2)).registerCompute(any());
        verify(sdkWebsocketEndpointProvider).setSdkWebsocketEndpoint(SDK_WEB_SOCKET_ENDPOINT);
        verify(computeAuthTokenManager).getComputeAuthToken();
        verify(webSocketConnectionProvider).updateConnection(any());
    }

    @Test
    public void GIVEN_registerComputeThrowsNonRetryableException_WHEN_connect_THEN_doesNotRetry() throws AgentException {
        // GIVEN
        when(gameLift.registerCompute(any()))
                .thenThrow(InvalidRequestException.class);

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> connectionManager.connect());
        verify(gameLift).registerCompute(any());
        verify(sdkWebsocketEndpointProvider, never()).setSdkWebsocketEndpoint(SDK_WEB_SOCKET_ENDPOINT);
        verify(computeAuthTokenManager, never()).getComputeAuthToken();
        verify(webSocketConnectionProvider, never()).updateConnection(any());
    }

    @Test
    public void GIVEN_allRetriesExhausted_WHEN_connect_THEN_throws() throws AgentException {
        // GIVEN
        when(gameLift.registerCompute(any()))
                .thenThrow(RuntimeException.class);

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> connectionManager.connect());
        verify(gameLift, times(9)).registerCompute(any());
    }

    @Test
    public void GIVEN_getComputeAuthTokenFails_WHEN_connect_THEN_throws() throws RuntimeException {
        // GIVEN
        doThrow(RuntimeException.class).when(computeAuthTokenManager).getComputeAuthToken();

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> connectionManager.connect());
    }

    @Test
    public void GIVEN_badUri_WHEN_connect_THEN_throws() {
        // GIVEN
        try (final MockedConstruction<URIBuilder> ignored = mockConstruction(
                URIBuilder.class,
                (mock, context) -> when(mock.addParameter(any(), any())).thenThrow(URISyntaxException.class)
        )) {
            // WHEN / THEN
            assertThrows(RuntimeException.class, () -> connectionManager.connect());
        }
    }

    @Test
    public void GIVEN_webSocketConnectInterrupted_WHEN_connect_THEN_throws() throws Exception {
        // GIVEN
        CompletableFuture<WebSocket> mockFuture = mock(CompletableFuture.class);
        when(mockWebSocketBuilder.buildAsync(any(), any())).thenReturn(mockFuture);
        when(mockFuture.get(anyLong(), any())).thenThrow(InterruptedException.class);

        // WHEN / THEN
        final RuntimeException e = assertThrows(RuntimeException.class, () -> connectionManager.connect());
        assertInstanceOf(InterruptedException.class, e.getCause());
    }

    @Test
    public void GIVEN_webSocketConnectTimesOut_WHEN_connect_THEN_throws() throws Exception {
        // GIVEN
        CompletableFuture<WebSocket> mockFuture = mock(CompletableFuture.class);
        when(mockWebSocketBuilder.buildAsync(any(), any())).thenReturn(mockFuture);
        when(mockFuture.get(anyLong(), any())).thenThrow(TimeoutException.class);
        // WHEN / THEN
        final RuntimeException e = assertThrows(RuntimeException.class, () -> connectionManager.connect());
        assertInstanceOf(TimeoutException.class, e.getCause());
    }

    @Test
    public void GIVEN_webSocketConnectFails_WHEN_connect_THEN_throws() throws Exception {
        // GIVEN
        CompletableFuture<WebSocket> mockFuture = mock(CompletableFuture.class);
        when(mockWebSocketBuilder.buildAsync(any(), any())).thenReturn(mockFuture);
        when(mockFuture.get(anyLong(), any())).thenThrow(ExecutionException.class);

        // WHEN / THEN
        final RuntimeException e = assertThrows(RuntimeException.class, () -> connectionManager.connect());
        assertInstanceOf(ExecutionException.class, e.getCause());
    }

    @Test
    public void GIVEN_validInput_WHEN_reconnect_THEN_handshakesAndConnects() throws ConflictException,
            UnauthorizedException, InvalidRequestException, InternalServiceException, NotFoundException {
        // GIVEN
        final RefreshConnectionMessage refreshConnectionMessage = new RefreshConnectionMessage();
        refreshConnectionMessage.setAuthToken(REFRESH_WEB_SOCKET_AUTH_TOKEN);
        refreshConnectionMessage.setRefreshConnectionEndpoint(REFRESH_WEB_SOCKET_ENDPOINT);

        // WHEN
        connectionManager.refreshWebSocketConnection(refreshConnectionMessage);

        // THEN
        verify(gameLift, never()).registerCompute(any());
        verify(gameLift, never()).getComputeAuthToken(any());

        verify(mockWebSocketBuilder).buildAsync(uriCaptor.capture(), any(GameLiftAgentWebSocketListener.class));
        final String newUri = uriCaptor.getValue().toString();
        assertTrue(newUri.contains(REFRESH_WEB_SOCKET_ENDPOINT));
        assertTrue(newUri.contains(FLEET_ID));
        assertTrue(newUri.contains(COMPUTE_NAME));
        assertTrue(newUri.contains(REFRESH_WEB_SOCKET_AUTH_TOKEN));

        verify(webSocketConnectionProvider).updateConnection(connectionCaptor.capture());
    }

}
