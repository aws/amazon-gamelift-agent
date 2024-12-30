/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.websocket.SendHeartbeatRequest;
import com.amazon.gamelift.agent.module.ThreadingModule;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.amazon.gamelift.agent.utils.ExecutorServiceSafeRunnable;
import com.amazon.gamelift.agent.websocket.AgentWebSocket;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.amazon.gamelift.agent.module.ConfigModule.HEARTBEAT_TIMEOUT_TIME;
import static com.amazon.gamelift.agent.module.ThreadingModule.HEARTBEAT_SENDER_EXECUTOR;

@Slf4j
public class HeartbeatSender {
    private static final long INITIAL_HEARTBEAT_DELAY_SECONDS = 0;
    private static final long HEARTBEAT_INTERVAL_SECONDS = Duration.ofMinutes(1).toSeconds();

    private final StateManager stateManager;
    private final WebSocketConnectionProvider webSocketConnectionProvider;
    private final GameProcessManager gameProcessManager;
    private final Instant heartbeatTimeoutTime;
    private final ScheduledExecutorService executorService;
    private final ExecutorServiceManager executorServiceManager;

    /**
     * Constructor for HeartbeatSender
     * @param stateManager
     * @param webSocketConnectionProvider
     * @param gameProcessManager
     * @param executorService
     */
    @Inject
    public HeartbeatSender(
            final StateManager stateManager,
            final WebSocketConnectionProvider webSocketConnectionProvider,
            final GameProcessManager gameProcessManager,
            @Named(HEARTBEAT_TIMEOUT_TIME) @Nullable final Instant heartbeatTimeoutTime,
            @Named(HEARTBEAT_SENDER_EXECUTOR) final ScheduledExecutorService executorService,
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        this.stateManager = stateManager;
        this.webSocketConnectionProvider = webSocketConnectionProvider;
        this.gameProcessManager = gameProcessManager;
        this.heartbeatTimeoutTime = heartbeatTimeoutTime;
        this.executorService = executorService;
        this.executorServiceManager = executorServiceManager;
    }

    /**
     * Start heartbeat sending
     */
    public void start() {
        executorService.scheduleWithFixedDelay(new ExecutorServiceSafeRunnable(this::sendHeartbeat),
                INITIAL_HEARTBEAT_DELAY_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("Started a daemon to send a heartbeat {} seconds.", HEARTBEAT_INTERVAL_SECONDS);
    }

    /**
     * Send a heartbeat
     */
    public void sendHeartbeat() {
        try {
            final List<String> processList = new ArrayList<>(gameProcessManager.getAllProcessUUIDs());
            final SendHeartbeatRequest request = SendHeartbeatRequest.builder()
                    .status(stateManager.getComputeStatus().toString())
                    .processList(processList)
                    .heartbeatTimeMillis(Instant.now().toEpochMilli())
                    .build();

            final AgentWebSocket client = webSocketConnectionProvider.getCurrentConnection();

            log.info("Sending heartbeat: {}", request);
            client.sendRequestAsync(request);
        } catch (final Exception e) {
            log.error("Failed to send heartbeat", e);
        }
        cancelSendingNewHeartbeatIfTimeout();
    }

    /**
     * Terminates the scheduled heartbeat thread if heartbeatTimeout is set
     */
    private void cancelSendingNewHeartbeatIfTimeout() {
        if (heartbeatTimeoutTime != null && Instant.now().isAfter(heartbeatTimeoutTime)) {
            log.info("Shutting down the heartbeat daemon due to timeout. Heartbeat timeout time: {}",
                    heartbeatTimeoutTime.toEpochMilli());
            executorServiceManager.shutdownScheduledThreadPoolExecutorServiceByName(this.getClass().getSimpleName());
        }
    }
}
