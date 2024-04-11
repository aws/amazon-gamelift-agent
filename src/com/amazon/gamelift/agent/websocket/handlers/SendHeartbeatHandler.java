/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.ComputeStatus;
import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.websocket.SendHeartbeatResponse;
import com.amazon.gamelift.agent.manager.ShutdownOrchestrator;
import com.amazon.gamelift.agent.manager.StateManager;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SendHeartbeatHandler extends MessageHandler<SendHeartbeatResponse> {
    @VisibleForTesting
    public static final int MAX_UNREGISTERED_PROCESS_HEARTBEAT_COUNT = 8;

    private final StateManager stateManager;
    private final ShutdownOrchestrator shutdownOrchestrator;
    private final GameProcessManager gameProcessManager;
    private final Map<String, Integer> unregisteredProcessCounter;

    /**
     * Constructor for SendHeartbeatHandler
     * See GameLiftAgentWebSocketListener for message routing by Action
     * @param objectMapper
     * @param stateManager
     * @param shutdownOrchestrator
     * @param gameProcessManager
     */
    @Inject
    public SendHeartbeatHandler(final ObjectMapper objectMapper,
                                final StateManager stateManager,
                                final ShutdownOrchestrator shutdownOrchestrator,
                                final GameProcessManager gameProcessManager) {
        super(SendHeartbeatResponse.class, objectMapper);
        this.stateManager = stateManager;
        this.shutdownOrchestrator = shutdownOrchestrator;
        this.gameProcessManager = gameProcessManager;
        unregisteredProcessCounter = new ConcurrentHashMap<>();
    }

    @Override
    public void handle(final SendHeartbeatResponse response) {
        final List<String> unhealthyProcesses = response.getUnhealthyProcesses();
        final List<String> unregisteredProcesses = response.getUnregisteredProcesses();

        log.info("Received Heartbeat response with Status: [{}]; Unhealthy processes: [{}]; "
                + "Unregistered processes: [{}]", response.getStatus(), unhealthyProcesses, unregisteredProcesses);

        processComputeStatus(response.getStatus());
        processUnhealthyProcesses(unhealthyProcesses);
        processUnregisteredProcesses(unregisteredProcesses);
    }

    private void processUnhealthyProcesses(final List<String> unhealthyProcesses) {
        for (final String processId : unhealthyProcesses) {
            gameProcessManager.terminateProcessByUUID(processId,
                    ProcessTerminationReason.SERVER_PROCESS_TERMINATED_UNHEALTHY);
        }
    }

    /**
     * This is a fallback mechanism to terminate processes if they've been unregistered for too long.
     * This logic will force terminate the unregistered processes after 8 heartbeats (8 minutes)
     */
    private void processUnregisteredProcesses(final List<String> unregisteredProcesses) {
        // If a process was previously unregistered, but is now registered, remove it
        unregisteredProcessCounter.keySet().removeIf(it -> !unregisteredProcesses.contains(it));

        // If a process was unregistered, increment its counter. If that counter is over a threshold,
        // terminate the process
        for (final String processId : unregisteredProcesses) {
            final Integer count = unregisteredProcessCounter.compute(processId, (key, value) ->
                    Optional.ofNullable(value).map(it -> it + 1).orElse(1));

            if (count >= MAX_UNREGISTERED_PROCESS_HEARTBEAT_COUNT) {
                unregisteredProcessCounter.remove(processId);
                gameProcessManager.terminateProcessByUUID(processId,
                        ProcessTerminationReason.SERVER_PROCESS_SDK_INITIALIZATION_TIMEOUT);
            }
        }
    }

    /**
     * Aligns the ProcessManager's internal status with whatever is returned in the Heartbeat response.
     * This allows the ProcessManager to stay in sync with updates to Compute status performed server-side.
     */
    private void processComputeStatus(final String computeStatusFromResponse) {
        if (stateManager.isComputeTerminatingOrTerminated()) {
            // If the Compute is terminating, do update the ProcessManager internal state.
            // State should get updated automatically during the normal shutdown flow.
            return;
        }

        final ComputeStatus currentComputeStatus = stateManager.getComputeStatus();
        if (!currentComputeStatus.name().equalsIgnoreCase(computeStatusFromResponse)) {
            if (ComputeStatus.Terminated.name().equalsIgnoreCase(computeStatusFromResponse)) {
                log.warn("Received Terminated status from Heartbeat. Initiating immediate Compute termination");
                shutdownOrchestrator.completeTermination();
            } else if (ComputeStatus.Terminating.name().equalsIgnoreCase(computeStatusFromResponse)) {
                log.warn("Received Terminating status from Heartbeat. Initiating normal Compute termination");
                shutdownOrchestrator.startTermination(
                        Instant.now().plus(ShutdownOrchestrator.DEFAULT_TERMINATION_DEADLINE), false);
            } else if (ComputeStatus.Active.name().equalsIgnoreCase(computeStatusFromResponse)) {
                log.info("Received Active status from Heartbeat. Updating internal status to Active");
                stateManager.reportComputeActive();
            }
        } else if (ComputeStatus.Initializing.name().equalsIgnoreCase(computeStatusFromResponse)) {
            // Wait to transition to Activating until Initializing is received from the server
            log.info("Received Initializing status from Heartbeat. Updating internal status to Activating");
            stateManager.reportComputeActivating();
        }
    }
}
