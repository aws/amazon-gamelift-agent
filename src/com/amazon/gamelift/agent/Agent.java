/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent;

import com.amazon.gamelift.agent.logging.GameLiftAgentLogUploader;
import com.amazon.gamelift.agent.manager.HeartbeatSender;
import com.amazon.gamelift.agent.manager.InstanceTerminationMonitor;
import com.amazon.gamelift.agent.manager.ShutdownOrchestrator;
import com.amazon.gamelift.agent.manager.StateManager;
import com.amazon.gamelift.agent.process.GameProcessMonitor;
import com.amazon.gamelift.agent.utils.EnvironmentHelper;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionManager;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class Agent {
    private final WebSocketConnectionManager connectionManager;
    private final GameProcessMonitor gameProcessMonitor;
    private final StateManager stateManager;
    private final HeartbeatSender heartbeatSender;
    private final InstanceTerminationMonitor instanceTerminationMonitor;
    private final ShutdownOrchestrator shutdownOrchestrator;
    private final GameLiftAgentLogUploader gameLiftAgentLogUploader;

    /**
     * GameLiftAgent constructor
     * @param connectionManager
     * @param gameProcessMonitor
     * @param stateManager
     * @param heartbeatSender
     * @param instanceTerminationMonitor
     * @param shutdownOrchestrator
     * @param gameLiftAgentLogUploader
     */
    @Inject
    public Agent(
            final WebSocketConnectionManager connectionManager,
            final GameProcessMonitor gameProcessMonitor,
            final StateManager stateManager,
            final HeartbeatSender heartbeatSender,
            final InstanceTerminationMonitor instanceTerminationMonitor,
            final ShutdownOrchestrator shutdownOrchestrator,
            final GameLiftAgentLogUploader gameLiftAgentLogUploader) {
        this.connectionManager = connectionManager;
        this.gameProcessMonitor = gameProcessMonitor;
        this.stateManager = stateManager;
        this.heartbeatSender = heartbeatSender;
        this.instanceTerminationMonitor = instanceTerminationMonitor;
        this.shutdownOrchestrator = shutdownOrchestrator;
        this.gameLiftAgentLogUploader = gameLiftAgentLogUploader;
    }

    /**
     * The main thread for starting the GameLift agent. Initializes the classes which need to run asynchronously,
     * and then awaits termination.
     * @throws Exception
     */
    public void start() throws Exception {
        try {
            // Get a snaphshot of the host metadata to assist with debugging
            EnvironmentHelper.logEC2Metadata();

            log.info("Initializing Websocket connection and starting async threads");
            connectionManager.connect();

            // Compute status is currently Initializing. SendHeartbeat will update status to Activating once server-side
            // status is also Initializing.
            heartbeatSender.start();
            gameLiftAgentLogUploader.start();
            instanceTerminationMonitor.start();
            gameProcessMonitor.start();

            log.info("Finished initializing Websocket connection and async threads");
        } catch (Exception e) {
            log.error("Encountered exception when starting up the GameLiftAgent", e);
            throw e;
        }
    }

    /**
     * Method for invoking any necessary shutdown logic. This is mostly a passthrough helper method for the
     * shutdown hook in the Application main thread
     */
    public void shutdown() {
        try {
            shutdownOrchestrator.completeTermination();
        } catch (Exception e) {
            log.error("Encountered exception when shutting down the GameLiftAgent", e);
            throw e;
        }
    }
}
