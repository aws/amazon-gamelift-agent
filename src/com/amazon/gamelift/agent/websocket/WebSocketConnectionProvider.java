/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import com.amazon.gamelift.agent.utils.ExecutorServiceSafeRunnable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.amazon.gamelift.agent.module.ThreadingModule.WEBSOCKET_CONNECTION_PROVIDER_EXECUTOR;

@Slf4j
@Singleton
public class WebSocketConnectionProvider {
    private static final int OLD_CONNECTION_EXPIRATION_MINUTES = 5;
    private static final Duration CLOSE_CONNECTION_TIMEOUT = Duration.ofMinutes(1);

    private final ScheduledExecutorService connectionCloserService;

    @Getter(AccessLevel.NONE)
    private final Object authTokenLock = new Object();

    @GuardedBy("authTokenLock")
    @Getter(onMethod = @__(@Synchronized("authTokenLock")))
    @Setter(onMethod = @__(@Synchronized("authTokenLock")))
    private String currentAuthToken = null;

    @Getter(AccessLevel.NONE)
    private final Object connectionLock = new Object();

    @GuardedBy("connectionLock")
    @Getter(onMethod = @__(@Synchronized("connectionLock")))
    private AgentWebSocket currentConnection;

    /**
     * Constructor for WebSocketConnectionProvider
     * @param connectionCloserService
     */
    @Inject
    public WebSocketConnectionProvider(
            @Named(WEBSOCKET_CONNECTION_PROVIDER_EXECUTOR) final ScheduledExecutorService connectionCloserService) {
        this.connectionCloserService = connectionCloserService;
    }

    /**
     * Saves a new WebSocket connection as the active WebSocket connection that the GameLift Agent will use for
     * calling GameLift. The previous connection will be kept alive for a short period of time after the new connection
     * is saved in the event that there are in-flight messages being processed, but all outgoing messages will still
     * be sent on the new connection.
     *
     * @param newConnection the instance of AgentWebSocket for the new active WebSocket connection
     */
    @Synchronized("connectionLock")
    public void updateConnection(final AgentWebSocket newConnection) {
        log.info("Updating the current WebSocket connection: webSocketId={}", newConnection.getWebSocketIdentifier());

        // Store current connection into a local variable so Java captures it in the lambda to avoid it getting
        // garbage collected early.
        final AgentWebSocket oldConnection = this.currentConnection;
        // Important to not close oldConnection immediately. Some communication may still occur via this connection.
        connectionCloserService.schedule(new ExecutorServiceSafeRunnable(() -> closeConnection(oldConnection)),
                OLD_CONNECTION_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        this.currentConnection = newConnection;
    }

    /**
     * Close all open connections
     */
    @Synchronized("connectionLock")
    public void closeAllConnections() {
        final List<Runnable> remainingTasks = connectionCloserService.shutdownNow();
        remainingTasks.forEach(Runnable::run);
        closeConnection(currentConnection);
        currentConnection = null;
    }

    private void closeConnection(final AgentWebSocket connection) {
        if (connection != null) {
            try {
                connection.closeConnection(CLOSE_CONNECTION_TIMEOUT);
            } catch (final Exception e) {
                log.warn("Failed to close websocket connection", e);
            }
        }
    }
}
