/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import com.amazon.gamelift.agent.manager.StateManager;
import com.amazon.gamelift.agent.model.exception.ConflictException;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.exception.NotReadyException;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import com.amazon.gamelift.agent.model.gamelift.RegisterComputeResponse;
import com.amazon.gamelift.agent.model.websocket.RefreshConnectionMessage;
import com.amazon.gamelift.agent.client.AmazonGameLiftClientWrapper;
import com.amazon.gamelift.agent.manager.ComputeAuthTokenManager;
import com.amazon.gamelift.agent.module.ConfigModule;
import com.amazon.gamelift.agent.utils.RetryHelper;
import com.amazon.gamelift.agent.websocket.handlers.MessageHandler;
import com.amazonaws.services.gamelift.model.RegisterComputeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Singleton
public class WebSocketConnectionManager {
    private static final Duration WEBSOCKET_CONNECT_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration WEBSOCKET_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_REGISTER_COMPUTE_RETRIES = 8;

    // A maximum number of times GameLift Agent will attempt to reconnect the WebSocket in the event of an unexpected
    // disconnection. Given the default configurations defined in RetryHelper, this will allow the manager to retry
    // for up to a total of 6.5 minutes of backoff time.
    private static final int WEBSOCKET_RECONNECT_RETRY_ATTEMPTS = 30;

    private final AmazonGameLiftClientWrapper amazonGameLift;
    private final String fleetId;
    private final String computeName;
    private final String region;
    private final String location;
    private final String ipAddress;
    private final String certificatePath;
    private final String dnsName;
    private final WebSocketConnectionProvider webSocketConnectionProvider;
    private final SdkWebsocketEndpointProvider sdkWebSocketEndpointProvider;
    private final WebSocketExceptionProvider webSocketExceptionProvider;
    private final Map<String, MessageHandler<?>> messageHandlers;
    private final ObjectMapper objectMapper;
    private final WebSocket.Builder webSocketBuilder;
    private final ComputeAuthTokenManager computeAuthTokenManager;
    private final StateManager stateManager;

    /**
     * Constructor for WebSocketConnectionManager
     */
    @Inject
    public WebSocketConnectionManager(
            final AmazonGameLiftClientWrapper amazonGameLift,
            @Named(ConfigModule.FLEET_ID) final String fleetId,
            @Named(ConfigModule.COMPUTE_NAME) final String computeName,
            @Named(ConfigModule.REGION) final String region,
            @Named(ConfigModule.LOCATION) @Nullable final String location,
            @Named(ConfigModule.IP_ADDRESS) @Nullable final String ipAddress,
            @Named(ConfigModule.CERTIFICATE_PATH) @Nullable final String certificatePath,
            @Named(ConfigModule.DNS_NAME) @Nullable final String dnsName,
            final WebSocketConnectionProvider webSocketConnectionProvider,
            final SdkWebsocketEndpointProvider sdkWebSocketEndpointProvider,
            final WebSocketExceptionProvider webSocketExceptionProvider,
            final Map<String, MessageHandler<?>> messageHandlers,
            final ObjectMapper objectMapper,
            final WebSocket.Builder webSocketBuilder,
            final ComputeAuthTokenManager computeAuthTokenManager,
            final StateManager stateManager) {
        this.amazonGameLift = amazonGameLift;
        this.fleetId = fleetId;
        this.computeName = computeName;
        this.region = region;
        this.location = location;
        this.ipAddress = ipAddress;
        this.certificatePath = certificatePath;
        this.dnsName = dnsName;
        this.webSocketConnectionProvider = webSocketConnectionProvider;
        this.sdkWebSocketEndpointProvider = sdkWebSocketEndpointProvider;
        this.webSocketExceptionProvider = webSocketExceptionProvider;
        this.messageHandlers = messageHandlers;
        this.objectMapper = objectMapper;
        this.webSocketBuilder = webSocketBuilder;
        this.computeAuthTokenManager = computeAuthTokenManager;
        this.stateManager = stateManager;
    }

    /**
     * Performs the initial WebSocket connection flow to register GameLift Agent
     * as a compute with GameLift and connecting to the WebSocket. This should only be
     * called once when GameLift Agent starts.
     */
    public void connect() {
        RegisterComputeResponse response;
        try {
            response = RetryHelper.runRetryable(MAX_REGISTER_COMPUTE_RETRIES, true, this::registerCompute);
        } catch (final AgentException e) {
            throw new RuntimeException(e);
        }

        // Save the SDK Websocket Endpoint from the response for use when spinning up processes,
        // and pass it via environment variables.
        sdkWebSocketEndpointProvider.setSdkWebsocketEndpoint(response.getSdkWebsocketEndpoint());

        final String agentWebSocketEndpoint = response.getAgentWebsocketEndpoint();
        final String computeAuthToken = computeAuthTokenManager.getComputeAuthToken();
        final AgentWebSocket connection = connectToWebSocket(agentWebSocketEndpoint, computeAuthToken);

        webSocketConnectionProvider.setCurrentAuthToken(computeAuthToken);
        webSocketConnectionProvider.updateConnection(connection);
    }

    /**
     * Performs a WebSocket connection refresh. Refreshes occur when a new connection needs to be created
     * in order to persist the WebSocket connection for an extended period of time. A fresh ComputeAuthToken is also
     * received through this message, which will be saved for later use.
     *
     * @param reconnectMessage message from the WebSocket that's received when GameLift Agent needs to refresh
     */
    public void refreshWebSocketConnection(final RefreshConnectionMessage reconnectMessage)
            throws InternalServiceException {
        final String refreshConnectionEndpoint = reconnectMessage.getRefreshConnectionEndpoint();
        final String refreshConnectionAuthToken = reconnectMessage.getAuthToken();
        log.info("Starting refresh of WebSocket connection to endpoint {}", refreshConnectionEndpoint);

        final AgentWebSocket newConnection = connectToWebSocket(refreshConnectionEndpoint,
                refreshConnectionAuthToken);
        webSocketConnectionProvider.setCurrentAuthToken(refreshConnectionAuthToken);
        webSocketConnectionProvider.updateConnection(newConnection);
    }

    /**
     * Processes the business logic that is needed when a WebSocket disconnection occurs. A unique ID is utilized
     * to identify each WebSocket connection, and this logic uses that to determine if the disconnection occurred
     * on the active/primary WebSocket connection. If so, GameLift Agent will attempt to reconnect to the WebSocket
     * using the previous endpoint/AuthToken used.
     *
     * @param disconnectedWebSocketIdentifier the unique ID used for the WebSocket connection that disconnected
     * @throws InternalServiceException thrown if this method is called before ever establishing a connection
     */
    public void handleWebSocketDisconnect(final String disconnectedWebSocketIdentifier)
            throws AgentException {
        final AgentWebSocket currentConnection = webSocketConnectionProvider.getCurrentConnection();
        if (stateManager.isComputeTerminated()) {
            log.info("Received WebSocket disconnect while in the Terminated status; not performing a WebSocket reconnect");
            return;
        } else if (!disconnectedWebSocketIdentifier.equals(currentConnection.getWebSocketIdentifier())) {
            log.info("Disconnected WebSocket connection with ID {} is not the current WebSocket connection - "
                    + "not performing a WebSocket reconnect", disconnectedWebSocketIdentifier);
            return;
        }

        log.error("Encountered an unexpected disconnection for the primary WebSocket connection with ID: {}; "
                        + "Attemping to reconnect to endpoint: {}", currentConnection.getWebSocketIdentifier(),
                currentConnection.getWebSocketEndpoint());

        final AgentWebSocket newConnection = RetryHelper.runRetryable(WEBSOCKET_RECONNECT_RETRY_ATTEMPTS,
                () -> connectToWebSocket(currentConnection.getWebSocketEndpoint(),
                        computeAuthTokenManager.getComputeAuthToken()));
        webSocketConnectionProvider.updateConnection(newConnection);
    }

    private RegisterComputeResponse registerCompute() throws AgentException {
        final RegisterComputeRequest registerComputeRequest = new RegisterComputeRequest()
                .withFleetId(fleetId)
                .withComputeName(computeName)
                .withIpAddress(ipAddress)
                .withLocation(location)
                .withDnsName(dnsName)
                .withCertificatePath(certificatePath);

        log.info("Registering compute: {}", registerComputeRequest);
        try {
            final RegisterComputeResponse response = amazonGameLift.registerCompute(registerComputeRequest);
            log.info("Successfully registered compute: {}", response);
            return response;
        } catch (final NotReadyException e) {
            log.info("Compute is not ready to be registered yet", e);
            throw e;
        } catch (final ConflictException e) {
            log.error("Attempted to register a compute that is already registered", e);
            throw e;
        } catch (final UnauthorizedException | InvalidRequestException
                       | InternalServiceException e) {
            log.error("Failed to register compute", e);
            throw e;
        }
    }

    private AgentWebSocket connectToWebSocket(final String webSocketEndpoint,
                                              final String authToken) {
        log.info("Creating WebSocket connection to: {}", webSocketEndpoint);

        final URI permanentConnectionUri = buildConnectionUri(webSocketEndpoint, authToken);
        final GameLiftAgentWebSocketListener webSocketListener = new GameLiftAgentWebSocketListener(
                this, messageHandlers, objectMapper);
        try {
            final WebSocket connectedWebsocket =
                    webSocketBuilder.buildAsync(permanentConnectionUri, webSocketListener)
                                    .get(WEBSOCKET_CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return new AgentWebSocket(connectedWebsocket, webSocketListener,
                    webSocketExceptionProvider, webSocketEndpoint, objectMapper);
        } catch (final ExecutionException e) {
            log.error("Failed to open the GameLiftAgent WebSocket connection", e);
            throw new RuntimeException("Failed to open the GameLiftAgent WebSocket connection", e);
        } catch (final TimeoutException e) {
            log.error("Timed out after {} seconds when opening the GameLiftAgent WebSocket connection",
                    WEBSOCKET_CONNECT_TIMEOUT.toSeconds(), e);
            throw new RuntimeException("Timed out when opening the GameLiftAgent WebSocket connection", e);
        } catch (final InterruptedException | CancellationException e) {
            log.error("Interrupted while attempting to connect to GameLiftAgent WebSocket", e);
            throw new RuntimeException("Interrupted while attempting to connect to GameLiftAgent WebSocket", e);
        }
    }

    private URI buildConnectionUri(final String webSocketEndpoint, final String authToken) {
        try {
            return new URIBuilder(webSocketEndpoint)
                    .addParameter("FleetId", fleetId)
                    .addParameter("ComputeName", computeName)
                    .addParameter("Authorization", authToken)
                    .build();
        } catch (final URISyntaxException e) {
            log.error("Failed to construct the GameLiftAgent's web socket URI", e);
            throw new RuntimeException("'" + webSocketEndpoint + "' endpoint is not a valid URL", e);
        }
    }
}
