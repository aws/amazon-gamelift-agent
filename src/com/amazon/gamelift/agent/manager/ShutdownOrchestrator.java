/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.client.AmazonGameLiftClientWrapper;
import com.amazon.gamelift.agent.logging.GameLiftAgentLogUploader;
import com.amazon.gamelift.agent.model.ComputeStatus;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.NotFinishedException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.amazon.gamelift.agent.process.GameProcessMonitor;
import com.amazon.gamelift.agent.utils.ExecutorServiceSafeRunnable;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.amazon.gamelift.agent.module.ThreadingModule;
import com.amazonaws.services.gamelift.model.DeregisterComputeRequest;
import com.amazonaws.services.gamelift.model.DeregisterComputeResult;
import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.amazon.gamelift.agent.module.ConfigModule.COMPUTE_NAME;
import static com.amazon.gamelift.agent.module.ConfigModule.FLEET_ID;
import static com.amazon.gamelift.agent.module.ConfigModule.ENABLED_COMPUTE_REGISTRATION_VIA_AGENT;
import static com.amazon.gamelift.agent.module.ThreadingModule.GAME_SESSION_LOGS_UPLOAD_EXECUTOR;
import static com.amazon.gamelift.agent.module.ThreadingModule.SHUTDOWN_ORCHESTRATOR_EXECUTOR;
import static com.amazon.gamelift.agent.module.ConfigModule.IS_CONTAINER_FLEET;

/**
 * Class responsible for shutting down GameLiftAgent. For cleanest shutdown this class should be used for all reasons.
 */
@Slf4j
public class ShutdownOrchestrator {
    private static final long CLEAN_SHUTDOWN_INITIAL_DELAY_SECONDS = 0L;
    private static final long CLEAN_SHUTDOWN_DELAY_AFTER_EXECUTE_SECONDS = 5L;
    private static final Long TOTAL_PROCESS_TERMINATION_WAIT_TIME_MS = 10000L;
    private static final Long PROCESS_TERMINATION_POLL_TIME_MS = 1000L;
    public static final Duration DEFAULT_TERMINATION_DEADLINE = Duration.ofMinutes(6);

    // Log upload occurs as processes terminate. In some cases, such as Spot interruption or slow process termination,
    // the full wait time configured below may not be available prior to instance termination.
    private static final Long LOG_UPLOAD_WAIT_TIME_MILLIS = 60000L;
    private static final String DEREGISTER_COMPUTE_SUCCESS_MESSAGE =
            "Compute Deregistered - DeregisterCompute Completed Successfully: {}";

    private final StateManager stateManager;
    private final HeartbeatSender heartbeatSender;
    private final GameProcessManager gameProcessManager;
    private final GameProcessMonitor gameProcessMonitor;
    private final WebSocketConnectionProvider webSocketConnectionProvider;
    private final GameLiftAgentLogUploader gameLiftAgentLogUploader;
    private final AmazonGameLiftClientWrapper amazonGameLift;
    private final ExecutorServiceManager executorServiceManager;
    private final ScheduledExecutorService executorService;
    private final boolean isContainerFleet;
    private final String fleetId;
    private final String computeName;
    private final boolean enableComputeRegistrationViaAgent;

    /**
     * Constructor for Shutdown Orchestrator
     * @param stateManager
     * @param heartbeatSender
     * @param gameProcessManager
     * @param gameProcessMonitor
     * @param webSocketConnectionProvider
     * @param gameLiftAgentLogUploader
     * @param amazonGameLift
     * @param executorService
     * @param executorServiceManager
     * @param isContainerFleet
     * @param fleetId
     * @param computeName
     */
    @Inject
    public ShutdownOrchestrator(
            final StateManager stateManager,
            final HeartbeatSender heartbeatSender,
            final GameProcessManager gameProcessManager,
            final GameProcessMonitor gameProcessMonitor,
            final WebSocketConnectionProvider webSocketConnectionProvider,
            final GameLiftAgentLogUploader gameLiftAgentLogUploader,
            final AmazonGameLiftClientWrapper amazonGameLift,
            @Named(SHUTDOWN_ORCHESTRATOR_EXECUTOR) final ScheduledExecutorService executorService,
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager,
            @Named(IS_CONTAINER_FLEET) final boolean isContainerFleet,
            @Named(FLEET_ID) final String fleetId,
            @Named(COMPUTE_NAME) final String computeName,
            @Named(ENABLED_COMPUTE_REGISTRATION_VIA_AGENT) final boolean enableComputeRegistrationViaAgent) {
        this.stateManager = stateManager;
        this.heartbeatSender = heartbeatSender;
        this.gameProcessManager = gameProcessManager;
        this.gameProcessMonitor = gameProcessMonitor;
        this.webSocketConnectionProvider = webSocketConnectionProvider;
        this.gameLiftAgentLogUploader = gameLiftAgentLogUploader;
        this.amazonGameLift = amazonGameLift;
        this.executorService = executorService;
        this.executorServiceManager = executorServiceManager;
        this.isContainerFleet = isContainerFleet;
        this.fleetId = fleetId;
        this.computeName = computeName;
        this.enableComputeRegistrationViaAgent = enableComputeRegistrationViaAgent;
    }

    /**
     * Starts Agent shutdown
     *
     * @param terminationDeadline The deadline by which Agent must be shut down
     */
    public synchronized void startTermination(final Instant terminationDeadline, final boolean spotInterrupted) {
        final ComputeStatus computeStatus = stateManager.getComputeStatus();
        // If ComputeStatus is Terminating, it's possible there was a spot interruption during normal termination.
        // In this case, schedule another "completeTermination" callback, so Agent will terminate early enough.
        if (computeStatus == ComputeStatus.Terminated) {
            log.info("Compute is already terminated");
            return;
        }

        final Duration timeUntilTermination = Duration.between(Instant.now(), terminationDeadline);
        log.info("Scheduling compute termination to be completed in {} seconds", timeUntilTermination.toSeconds());

        if (spotInterrupted) {
            // Update Compute status to "Interrupted" and send a heartbeat.
            stateManager.reportComputeInterrupted();
            heartbeatSender.sendHeartbeat();
        } else {
            stateManager.reportComputeTerminating();
        }

        gameProcessMonitor.shutdown();

        // Schedule Compute termination shutdown
        if (timeUntilTermination.isNegative()) {
            executorService.execute(this::completeTermination);
        } else {
            // Schedule a validation that will continuously check if all game processes are spun down. If so, it will
            // complete the termination early
            executorService.scheduleWithFixedDelay(new ExecutorServiceSafeRunnable(this::validateSafeTermination),
                    CLEAN_SHUTDOWN_INITIAL_DELAY_SECONDS, CLEAN_SHUTDOWN_DELAY_AFTER_EXECUTE_SECONDS, TimeUnit.SECONDS);

            // Schedule a separate task that will force the termination to be completed after the deadline is reached
            executorService.schedule(new ExecutorServiceSafeRunnable(this::completeTermination),
                    timeUntilTermination.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * A validation that is intended to be run periodically to check if the Compute can terminate early if all processes
     * have shut down. In the happy-case termination path, all the processes will get notifications through the
     * GameLift SDK to terminate and should cleanly shut themselves down. If all processes do that, then
     * the Agent can skip the rest of the wait time for termination.
     */
    @VisibleForTesting synchronized void validateSafeTermination() {
        final int numProcessesActive = gameProcessManager.getAllProcessUUIDs().size();
        if (numProcessesActive > 0) {
            log.info("Compute still has {} processes active - waiting for processes to exit cleanly",
                    numProcessesActive);
        } else {
            log.info("Compute has no more active processes - proceeding with compute termination");
            completeTermination();
        }
    }

    /**
     * Finishes the Agent shutdown process. After this method gets called, the Agent should exit.
     */
    public synchronized void completeTermination() {
        if (this.isContainerFleet && this.enableComputeRegistrationViaAgent) {
            deregisterCompute();
        }

        if (stateManager.getComputeStatus() == ComputeStatus.Terminated) {
            log.info("Compute is already terminated");
            return;
        }

        stateManager.reportComputeTerminated();

        try {
            gameProcessManager.terminateAllProcessesForShutdown(
                    TOTAL_PROCESS_TERMINATION_WAIT_TIME_MS, PROCESS_TERMINATION_POLL_TIME_MS);
        } catch (final NotFinishedException e) {
            log.warn("Some processes didn't complete termination after waiting; continuing with instance shutdown", e);
        }

        // Force send one last heartbeat with status=TERMINATED
        heartbeatSender.sendHeartbeat();

        // Wait for game session log upload to complete prior to closing web socket connection
        executorServiceManager.shutdownScheduledThreadPoolExecutorServiceByName(GAME_SESSION_LOGS_UPLOAD_EXECUTOR,
                LOG_UPLOAD_WAIT_TIME_MILLIS);

        // Upload any remaining logs (must be done before closing the websocket)
        log.info("Compute terminated");
        gameLiftAgentLogUploader.shutdownAndUploadLogs();

        // Close all websocket connections & threads
        webSocketConnectionProvider.closeAllConnections();
        executorServiceManager.shutdownExecutorServices();
    }

    private synchronized void deregisterCompute() {
        final DeregisterComputeRequest deregisterComputeRequest = new DeregisterComputeRequest()
                .withFleetId(fleetId)
                .withComputeName(computeName);
        try {
            DeregisterComputeResult deregisterComputeResult = amazonGameLift.deregisterCompute(deregisterComputeRequest);
            log.info(DEREGISTER_COMPUTE_SUCCESS_MESSAGE, deregisterComputeResult.toString());
        } catch (final NotFoundException e) {
            log.error("Resource not found", e);
        } catch (final UnauthorizedException
                       | InvalidRequestException
                       | InternalServiceException e) {
            log.error("Failed to deregister compute", e);
        }
    }
}
