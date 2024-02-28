/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import com.amazon.gamelift.agent.model.exception.ConflictException;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.AgentException;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

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
public class WebSocketConnectionManager {
    private static final Duration WEBSOCKET_CONNECT_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_REGISTER_COMPUTE_RETRIES = 8;

    private final AmazonGameLiftClientWrapper amazonGameLift;
    private final String fleetId;
    private final String computeName;
    private final String region;
    private final String location;
    private final String ipAddress;
    private final String certificatePath;
    private final String dnsName;
    private final String gameLiftAgentWebSocketEndpointOverride;
    private final WebSocketConnectionProvider webSocketConnectionProvider;
    private final SdkWebsocketEndpointProvider sdkWebsocketEndpointProvider;
    private final WebSocketExceptionProvider webSocketExceptionProvider;
    private final Map<String, MessageHandler<?>> messageHandlers;
    private final ObjectMapper objectMapper;
    private final WebSocket.Builder webSocketBuilder;
    private final ComputeAuthTokenManager computeAuthTokenManager;

    /**
     * Constructor for WebSocketConnectionManager
     * @param amazonGameLift
     * @param fleetId
     * @param computeName
     * @param region
     * @param location
     * @param ipAddress
     * @param certificatePath
     * @param dnsName
     * @param gameLiftAgentWebSocketEndpointOverride
     * @param webSocketConnectionProvider
     * @param sdkWebsocketEndpointProvider
     * @param messageHandlers
     * @param objectMapper
     * @param webSocketBuilder
     * @param computeAuthTokenManager
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
            @Named(ConfigModule.GAMELIFT_AGENT_WEBSOCKET_ENDPOINT_OVERRIDE) @Nullable final String
                    gameLiftAgentWebSocketEndpointOverride,
            final WebSocketConnectionProvider webSocketConnectionProvider,
            final SdkWebsocketEndpointProvider sdkWebsocketEndpointProvider,
            final WebSocketExceptionProvider webSocketExceptionProvider,
            final Map<String, MessageHandler<?>> messageHandlers,
            final ObjectMapper objectMapper,
            final WebSocket.Builder webSocketBuilder,
            final ComputeAuthTokenManager computeAuthTokenManager) {
        this.amazonGameLift = amazonGameLift;
        this.fleetId = fleetId;
        this.computeName = computeName;
        this.region = region;
        this.location = location;
        this.ipAddress = ipAddress;
        this.certificatePath = certificatePath;
        this.dnsName = dnsName;
        this.gameLiftAgentWebSocketEndpointOverride = gameLiftAgentWebSocketEndpointOverride;
        this.webSocketConnectionProvider = webSocketConnectionProvider;
        this.sdkWebsocketEndpointProvider = sdkWebsocketEndpointProvider;
        this.webSocketExceptionProvider = webSocketExceptionProvider;
        this.messageHandlers = messageHandlers;
        this.objectMapper = objectMapper;
        this.webSocketBuilder = webSocketBuilder;
        this.computeAuthTokenManager = computeAuthTokenManager;
    }

    /**
     * Connect to websocket
     */
    public void connect() {
        try {
            RetryHelper.runRetryable(MAX_REGISTER_COMPUTE_RETRIES, true, this::registerCompute);
        } catch (final AgentException e) {
            throw new RuntimeException(e);
        }

        final String webSocketEndpoint = buildWebSocketEndpoint();
        final String computeAuthToken = computeAuthTokenManager.getComputeAuthToken();
        final AgentWebSocket connection = connectToWebsocket(webSocketEndpoint, computeAuthToken);

        webSocketConnectionProvider.setCurrentAuthToken(computeAuthToken);
        webSocketConnectionProvider.updateConnection(connection);
    }

    /**
     * Reconnect to websocket
     * @param reconnectMessage
     */
    public void reconnect(final RefreshConnectionMessage reconnectMessage) {
        final String refreshConnectionEndpoint = reconnectMessage.getRefreshConnectionEndpoint();
        final String refreshConnectionAuthToken = reconnectMessage.getAuthToken();
        log.info("Starting refresh of Websocket connection to endpoint {}", refreshConnectionEndpoint);

        final AgentWebSocket newConnection = connectToWebsocket(refreshConnectionEndpoint,
                refreshConnectionAuthToken);
        webSocketConnectionProvider.setCurrentAuthToken(refreshConnectionAuthToken);
        webSocketConnectionProvider.updateConnection(newConnection);
    }

    private Void registerCompute() throws AgentException {
        final RegisterComputeRequest registerComputeRequest = new RegisterComputeRequest()
                .withFleetId(fleetId)
                .withComputeName(computeName)
                .withIpAddress(ipAddress)
                .withLocation(location)
                .withDnsName(dnsName)
                .withCertificatePath(certificatePath);

        log.info("Registering compute {}", registerComputeRequest);
        try {
            // Save the SDK Websocket Endpoint from the response for use when we spin up processes
            // and pass it via environment variables
            RegisterComputeResponse response = amazonGameLift.registerCompute(registerComputeRequest);
            sdkWebsocketEndpointProvider.setSdkWebsocketEndpoint(response.getSdkWebsocketEndpoint());
            log.info("Successfully registered compute: {}", response);
        } catch (ConflictException e) {
            log.info("Compute already registered. Treating as success due to likely occurrence of Cold Start.");
        } catch (final UnauthorizedException | InvalidRequestException
                       | InternalServiceException e) {
            log.error("Failed to register compute", e);
            throw e;
        }
        return null;
    }

    private AgentWebSocket connectToWebsocket(final String webSocketEndpoint,
                                              final String authToken) {
        log.info("Creating Websocket connection to {}", webSocketEndpoint);

        final URI permanentConnectionUri = buildConnectionUri(webSocketEndpoint, authToken);
        GameLiftAgentWebSocketListener webSocketListener = new GameLiftAgentWebSocketListener(messageHandlers,
                objectMapper);
        try {
            WebSocket connectedWebsocket =
                    webSocketBuilder.buildAsync(permanentConnectionUri, webSocketListener)
                                    .get(WEBSOCKET_CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return new AgentWebSocket(connectedWebsocket, webSocketListener,
                    webSocketExceptionProvider, objectMapper);
        } catch (final ExecutionException e) {
            log.error("Failed to open the GameLiftAgent websocket connection", e);
            throw new RuntimeException("Failed to open the GameLiftAgent websocket connection", e);
        } catch (final TimeoutException e) {
            log.error("Timed out after {} seconds when opening the GameLiftAgent websocket connection",
                    WEBSOCKET_CONNECT_TIMEOUT.toSeconds(), e);
            throw new RuntimeException("Timed out when opening the GameLiftAgent websocket connection", e);
        } catch (final InterruptedException | CancellationException e) {
            log.error("Interrupted while attempting to connect to GameLiftAgent websocket", e);
            throw new RuntimeException("Interrupted while attempting to connect to GameLiftAgent websocket", e);
        }
    }

    private URI buildConnectionUri(final String webSocketEndpoint, final String authToken) {
        try {
            return new URIBuilder(webSocketEndpoint)
                    .addParameter("FleetId", fleetId)
                    .addParameter("ComputeName", computeName)
                    .addParameter("Authorization", authToken)
                    .build();
        } catch (URISyntaxException e) {
            log.error("Failed to construct the GameLiftAgent's web socket URI", e);
            throw new RuntimeException("'" + webSocketEndpoint + "' endpoint is not a valid URL", e);
        }
    }

    private String buildWebSocketEndpoint() {
        final String webSocketEndpoint;
        if (StringUtils.isBlank(gameLiftAgentWebSocketEndpointOverride)) {
            webSocketEndpoint = String.format("wss://%s.process-manager-api.amazongamelift.com", region);
        } else {
            webSocketEndpoint = gameLiftAgentWebSocketEndpointOverride;
        }
        return webSocketEndpoint;
    }
}
