/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process;

import com.amazon.gamelift.agent.logging.UploadGameSessionLogsCallableFactory;
import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.ProcessStatus;
import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.exception.BadExecutablePathException;
import com.amazon.gamelift.agent.model.exception.NotFinishedException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.logging.UploadGameSessionLogsCallable;
import com.amazon.gamelift.agent.manager.ProcessEnvironmentManager;
import com.amazon.gamelift.agent.model.constants.ProcessConstants;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static com.amazon.gamelift.agent.module.ConfigModule.OPERATING_SYSTEM;
import static com.amazon.gamelift.agent.module.ThreadingModule.GAME_SESSION_LOGS_UPLOAD_EXECUTOR;

/**
 * Singleton class for maintaining the list of active game server processes on the Compute.
 * Also provides interfaces for batch initialization/termination of processes.
 *
 * This class assumes it will only be instantiated once, otherwise the map of managed processes
 * will be duplicated/incorrect.
 */
@Slf4j
@Singleton
public class GameProcessManager {

    private final ProcessEnvironmentManager processEnvironmentManager;
    private final ProcessTerminationEventManager processTerminationEventManager;
    private final OperatingSystem operatingSystem;
    private final UploadGameSessionLogsCallableFactory uploadGameSessionLogsCallableFactory;
    private final ScheduledExecutorService executorService;

    private final Map<String, GameProcess> gameProcessByUUID = new ConcurrentHashMap<>();

    /**
     * Constructor for GameProcessManager
     * @param processEnvironmentManager
     * @param processTerminationEventManager
     * @param operatingSystem
     * @param uploadGameSessionLogsCallableFactory
     * @param executorService
     */
    @Inject
    public GameProcessManager(final ProcessEnvironmentManager processEnvironmentManager,
                          final ProcessTerminationEventManager processTerminationEventManager,
                          @Named(OPERATING_SYSTEM) final OperatingSystem operatingSystem,
                          final UploadGameSessionLogsCallableFactory uploadGameSessionLogsCallableFactory,
                          @Named(GAME_SESSION_LOGS_UPLOAD_EXECUTOR) final ScheduledExecutorService executorService) {
        this.processEnvironmentManager = processEnvironmentManager;
        this.processTerminationEventManager = processTerminationEventManager;
        this.operatingSystem = operatingSystem;
        this.uploadGameSessionLogsCallableFactory = uploadGameSessionLogsCallableFactory;
        this.executorService = executorService;
    }

    /**
     * Starts one GameProcess and adds the process UUID and GameProcess to the ServerState mapping
     * @param gameProcessConfiguration
     * @return UUID of the newly started process
     * @throws AgentException
     */
    public String startProcessFromConfiguration(final GameProcessConfiguration gameProcessConfiguration)
            throws AgentException {
        GameProcess gameProcess = new GameProcess(gameProcessConfiguration, processEnvironmentManager,
                operatingSystem);
        final String processUuid;
        try {
            processUuid = gameProcess.start();
        } catch (BadExecutablePathException e) {
            // Note: Since the process was not started, the process UUID will not be registered with GameLift.
            // The notify call is still made to report the launch failure here as a Fleet event.
            processTerminationEventManager.notifyServerProcessTermination(
                    gameProcess.getProcessUUID(),
                    ProcessConstants.INVALID_LAUNCH_PATH_PROCESS_EXIT_CODE,
                    ProcessTerminationReason.SERVER_PROCESS_INVALID_PATH);
            throw e;
        }

        gameProcessByUUID.put(processUuid, gameProcess);

        // Schedule to run handleProcessExit when the process terminates
        gameProcess.handleProcessExit(this::handleProcessExit);
        return processUuid;
    }

    /**
     * A BiConsumer method which will get invoked when a process exits by applying it after the internal Java process'
     * onExit() method gets invoked. This is primarily for reporting the process exit over the websocket, and removing
     * the process from GameProcessManager's set of managed processes.
     *
     * See {@link GameProcess#handleProcessExit} for more details
     */
    private void handleProcessExit(final Process internalProcess, final GameProcess gameProcess) {
        try {
            try {
                processTerminationEventManager.notifyServerProcessTermination(
                        gameProcess.getProcessUUID(),
                        internalProcess.exitValue(),
                        gameProcess.getTerminationReason());
            } catch (Exception e) {
                log.error("Encountered exception reporting process exit for process UUID {}",
                        gameProcess.getProcessUUID(), e);
            }

            try {
                final UploadGameSessionLogsCallable callable = uploadGameSessionLogsCallableFactory
                        .newUploadGameSessionLogsCallable(gameProcess.getProcessUUID(),
                                gameProcess.getProcessConfiguration().getLaunchPath(),
                                new ArrayList<String>(gameProcess.getLogPaths()),
                                gameProcess.getGameSessionId());
                executorService.submit(callable);
            } catch (Exception e) {
                log.error("Encountered exception during game session log upload for process UUID {}",
                        gameProcess.getProcessUUID(), e);
            }
        } finally {
            gameProcessByUUID.remove(gameProcess.getProcessUUID());
        }
    }

    /**
     * Gets the UUIDs for all processes currently managed by the GameLift agent
     * @return set of all process UUIDs for managed processes
     */
    public Set<String> getAllProcessUUIDs() {
        return gameProcessByUUID.keySet();
    }

    /**
     * Gets the set of UUIDs for processes that have stayed in the Initializing state too long
     * and surpassed their timeout
     * @return set of timed out process UUIDs
     */
    public Set<String> getInitializationTimedOutProcessUUIDs() {
        return gameProcessByUUID.entrySet().stream()
                .filter(entry -> entry.getValue().hasTimedOutForInitialization())
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet());
    }

    /**
     * Helper method for getting active processes per config
     * @return a map of all process configurations to the number of active processes for that configuration
     */
    public Map<GameProcessConfiguration, Long> getProcessCountsByConfiguration() {
        return gameProcessByUUID.values().stream()
                .collect(Collectors.groupingBy(GameProcess::getProcessConfiguration, Collectors.counting()));
    }

    /**
     * Find process by its UUID and return a boolean representing if the process is still active
     * @throws NotFoundException when no process exists with the provided UUID
     */
    public boolean isProcessAlive(final String processUuid) throws NotFoundException {
        if (processUuid == null) {
            throw new NotFoundException("ProcessUUID provided is null");
        }
        if (!gameProcessByUUID.containsKey(processUuid)) {
            throw new NotFoundException(String.format("No Process found with UUID: %s", processUuid));
        }

        GameProcess processToCheck = gameProcessByUUID.get(processUuid);
        return processToCheck != null && processToCheck.isAlive();
    }

    /**
     * Terminates a process by its UUID used within GameLift agent & GameLift services.
     * If the process does not exist, then this should no-op.
     * @param processUuid UUID of the process to terminate
     */
    public void terminateProcessByUUID(final String processUuid, final ProcessTerminationReason terminationReason) {
        final GameProcess processToTerminate = gameProcessByUUID.get(processUuid);

        if (processToTerminate != null) {
            if (terminationReason != null) {
                processToTerminate.setTerminationReason(terminationReason);
            }

            processToTerminate.terminate();
        } else {
            log.warn(String.format(
                    "Skipping process termination; no managed process found with process UUID %s", processUuid));
        }
    }

    /**
     * Terminates a process by its UUID used within GameLift agent & GameLift services.
     * If the process does not exist, then this should no-op.
     * @param processUuid UUID of the process to terminate
     */
    public void terminateProcessByUUID(final String processUuid) {
        terminateProcessByUUID(processUuid, ProcessTerminationReason.SERVER_PROCESS_FORCE_TERMINATED);
    }

    /**
     * Terminates all processes forcibly on the Compute. Should only be used when the Compute is terminating.
     * @throws NotFinishedException
     */
    public void terminateAllProcessesForShutdown(
            final long totalWaitTimeMillis, final long pollWaitTimeMillis) throws NotFinishedException {
        for (String processUuid : gameProcessByUUID.keySet()) {
            try {
                terminateProcessByUUID(processUuid, ProcessTerminationReason.COMPUTE_SHUTTING_DOWN);
            } catch (Exception e) {
                log.warn("Ignoring exception caught while terminating process UUID {} for instance shut down",
                        processUuid, e);
            }
        }

        // Poll the GameProcessManager until all processes have been removed from the GameLift agent websocket connection.
        // This allows processes to exit using their normal termination hook and send their exit information via the GameLiftAgent Websocket connection
        Instant processWaitTimeStart = Instant.now();
        Instant processWaitTimeEnd = processWaitTimeStart.plusMillis(totalWaitTimeMillis);
        int processesLeft = gameProcessByUUID.keySet().size();
        while (processesLeft > 0 && processWaitTimeEnd.isAfter(Instant.now())) {
            log.info("GameLift agent still waiting for {} more processes to complete termination",
                    processesLeft);

            try {
                Thread.sleep(pollWaitTimeMillis);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for processes to spin down");
            }

            processesLeft = gameProcessByUUID.keySet().size();
        }

        if (processesLeft > 0) {
            throw new NotFinishedException(String.format("After waiting %d milliseconds, there were %d"
                    + " processes that had not terminated cleanly", totalWaitTimeMillis, processesLeft));
        }
    }

    /**
     * Updates the GameProcess object for the specified process ID for when the GameLift agent receives the message
     * indicating that the process has connected with the GameLift SDK.
     *
     * @param processUuid UUID of the process to save log paths for
     * @param logPaths logPaths of the process to save
     * @throws NotFoundException if the given process ID is not currently managed by the GameLift agent
     */
    public void updateProcessOnRegistration(final String processUuid, final List<String> logPaths)
            throws NotFoundException {
        GameProcess gameProcess = gameProcessByUUID.get(processUuid);
        if (gameProcess != null) {
            gameProcess.setProcessStatus(ProcessStatus.Active);
            gameProcess.setLogPaths(logPaths);
        } else {
            throw new NotFoundException(String.format("Attempted to save log paths for process with UUID [%s],"
                    + " but no such process exists", processUuid));
        }
    }

    /**
     * Updates the GameProcess object for the specified process UUID for when the GameLift agent receives the message
     * indicating that a game session has been activated on the process via the GameLift SDK.
     *
     * @param processUuid UUID of the process to save log paths for
     * @param gameSessionId game session ID placed on the process to save
     * @throws NotFoundException if the given process ID is not currently managed by the GameLift agent
     */
    public void updateProcessOnGameSessionActivation(final String processUuid, final String gameSessionId)
            throws NotFoundException {
        GameProcess gameProcess = gameProcessByUUID.get(processUuid);
        if (gameProcess != null) {
            gameProcess.setGameSessionId(gameSessionId);
        } else {
            throw new NotFoundException(String.format("Attempted to save game session ID for process with UUID [%s],"
                    + " but no such process exists", processUuid));
        }
    }
}
