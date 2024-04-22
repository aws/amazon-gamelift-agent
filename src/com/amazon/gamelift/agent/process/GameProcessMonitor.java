/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.RuntimeConfiguration;
import com.amazon.gamelift.agent.model.exception.BadExecutablePathException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.utils.ExecutorServiceSafeRunnable;
import com.amazon.gamelift.agent.manager.ExecutorServiceManager;
import com.amazon.gamelift.agent.manager.RuntimeConfigurationManager;
import com.amazon.gamelift.agent.manager.StateManager;
import com.amazon.gamelift.agent.module.ThreadingModule;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.amazon.gamelift.agent.module.ThreadingModule.GAME_PROCESS_MONITOR_EXECUTOR;

/**
 * The asynchronous task which continuously runs and monitors the runtime configuration for the compute,
 * scaling up new processes as needed to match what is currently configured.
 */
@Slf4j
public class GameProcessMonitor {
    // This constant is the maximum delay between process launches. Process launch delays will decrease from this value
    // as processes are launched without error, and they will reset to this value if there are errors.
    public static final long MAX_SECONDS_BETWEEN_PROCESS_LAUNCHES = 3L;
    // This is the lower bound for how long a process delay can be reduced to.
    public static final long MIN_SECONDS_BETWEEN_PROCESS_LAUNCHES = 1L; // 1 second
    // Time to delay the first execution.
    public static final long EXECUTOR_INITIAL_DELAY_SECONDS = 0;
    // Determines how much the next process delay is reduced upon a successful process launch.
    public static final long SUCCESSFUL_LAUNCH_DELAY_REDUCTION_SECONDS = 1;

    private final StateManager stateManager;
    private final RuntimeConfigurationManager runtimeConfigurationManager;
    private final GameProcessManager gameProcessManager;
    private final ScheduledExecutorService executorService;
    private final ExecutorServiceManager executorServiceManager;
    @Getter
    private long processStartDelay;

    /**
     * Constructor for GameProcessMonitor
     * @param stateManager
     * @param runtimeConfigurationManager
     * @param gameProcessManager
     * @param executorService
     * @param executorServiceManager
     */
    @Inject
    public GameProcessMonitor(
            final StateManager stateManager,
            final RuntimeConfigurationManager runtimeConfigurationManager,
            final GameProcessManager gameProcessManager,
            @Named(GAME_PROCESS_MONITOR_EXECUTOR) final ScheduledExecutorService executorService,
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager) {
        this.stateManager = stateManager;
        this.runtimeConfigurationManager = runtimeConfigurationManager;
        this.gameProcessManager = gameProcessManager;
        this.executorService = executorService;
        this.executorServiceManager = executorServiceManager;
        processStartDelay = MAX_SECONDS_BETWEEN_PROCESS_LAUNCHES;
    }

    /**
     * Start GameProcess monitoring with delay
     */
    public void start() {
        executorService.scheduleWithFixedDelay(new ExecutorServiceSafeRunnable(this::runProcessMonitor),
                EXECUTOR_INITIAL_DELAY_SECONDS, SUCCESSFUL_LAUNCH_DELAY_REDUCTION_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Shutdown GameProcess monitoring
     */
    public void shutdown() {
        log.info("Shutting down GameProcessMonitor; no new processes will be spun up");
        executorServiceManager.shutdownScheduledThreadPoolExecutorServiceByName(GameProcessMonitor.class.getSimpleName());
    }

    /**
     * Run Process monitor
     */
    public void runProcessMonitor() {
        if (stateManager.isComputeTerminatingOrTerminated()) {
            log.debug("Compute is in state {}, GameProcessMonitor task will no longer spin up processes",
                    stateManager.getComputeStatus());
            return;
        }
        if (stateManager.isComputeInitializing()) {
            log.debug("Compute is in state {}, GameProcessMonitor task will not spin up processes until Activating",
                    stateManager.getComputeStatus());
            return;
        }

        // 1. Check if any processes have timed out while waiting for the process to register through the GameLift SDK.
        //    Any timed out processes will be terminated forcefully to make room for new processes.
        final Set<String> processesToTerminate = gameProcessManager.getInitializationTimedOutProcessUUIDs();
        if (!processesToTerminate.isEmpty()) {
            log.info("Terminating processes due to reaching the SDK Initialization timeout: {}", processesToTerminate);

            for (final String processUUID : processesToTerminate) {
                gameProcessManager.terminateProcessByUUID(processUUID,
                        ProcessTerminationReason.SERVER_PROCESS_SDK_INITIALIZATION_TIMEOUT);
            }
        }

        final RuntimeConfiguration config = runtimeConfigurationManager.getRuntimeConfiguration();
        final Map<GameProcessConfiguration, Long> activeProcessesByConfig =
                gameProcessManager.getProcessCountsByConfiguration();

        // 2. Calculate the number of new processes to spin up.
        //    Don't allow more processes to spin up on the Compute than the total number defined by the current runtime
        //    config. This ensures that changes to the configuration do not overwhelm the compute by spinning up more
        //    processes than configured due to pre-existing processes.
        final int currentConfigTotalProcessCount = config.getServerProcesses().stream()
                .mapToInt(GameProcessConfiguration::getConcurrentExecutions).sum();
        if (currentConfigTotalProcessCount < 1) {
            log.debug("Current runtime configuration has no processes configured, no new processes will be started");
            return;
        }

        final int currentRunningProcessCount = activeProcessesByConfig.values().stream()
                .mapToInt(Long::intValue).sum();
        int totalProcessVacancies = currentConfigTotalProcessCount - currentRunningProcessCount;

        // 3. For each process configuration, spin up new processes to match the concurrent execution count in the config
        for (final GameProcessConfiguration processConfig : config.getServerProcesses()) {
            if (totalProcessVacancies <= 0) {
                log.debug("Active process count is greater or equal to total number of processes "
                        + "defined in Runtime Config - not launching any more processes");
                break;
            }

            final int maxNumberProcessesToLaunch =
                    processConfig.getConcurrentExecutions() - activeProcessesByConfig.getOrDefault(processConfig, 0L).intValue();
            if (maxNumberProcessesToLaunch <= 0) {
                log.debug("No additional processes needed for configuration: {}", processConfig);
                continue;
            }

            final int numberOfProcessesToLaunch = Math.min(totalProcessVacancies, maxNumberProcessesToLaunch);
            for (int i = 0; i < numberOfProcessesToLaunch; i++) {
                // A process start delay is introduced here to ensure processes do not crash before they are managed.
                // If successive processes start nominally, the delay will be reduced. Otherwise, the delay will be reset.
                try {
                    final String launchedProcessUuid = startProcessWithDelay(processConfig, processStartDelay);
                    processStartDelay = getNextDelay(processStartDelay, isProcessAlive(launchedProcessUuid));
                } catch (final RuntimeException e) {
                    processStartDelay = MAX_SECONDS_BETWEEN_PROCESS_LAUNCHES;
                    log.error("Failed to launch game server process; "
                            + "resetting delay between process launches to {} seconds", processStartDelay, e);
                }
            }

            log.info("Launched {} new processes for configuration: {}", numberOfProcessesToLaunch, processConfig);
            totalProcessVacancies -= numberOfProcessesToLaunch;
        }
    }

    /**
     * Return boolean indicating whether a process is active or not.
     */
    private boolean isProcessAlive(final String launchedProcessUuid) {
        try {
            return gameProcessManager.isProcessAlive(launchedProcessUuid);
        } catch (final NotFoundException e) {
            // process is no longer alive
            return false;
        }
    }

    /**
     * For each task that this method executes, it makes sure a time delay has passed since the launch of the last task.
     * The intent here is to make sure processes do not abruptly crash before they are managed.
     * If a process exists before the delay has passed, this class will throw a RuntimeException giving
     * the system an opportunity to properly handle errors.
     */
    @VisibleForTesting
    protected String startProcessWithDelay(final GameProcessConfiguration gameProcessConfiguration,
                                 final long processLaunchDelaySeconds) {
        String processUuid = null;
        Exception exceptionReceivedDuringCall = null;
        final long startTime = System.currentTimeMillis();
        try {
            processUuid = gameProcessManager.startProcessFromConfiguration(gameProcessConfiguration);
        } catch (final BadExecutablePathException e) {
            log.error("Caught exception attempting to run {}", gameProcessConfiguration, e);
            exceptionReceivedDuringCall = e;
        } catch (final Exception e) {
            log.error("Unexpected error starting process", e);
            exceptionReceivedDuringCall = new RuntimeException(e);
        }

        try {
            Thread.sleep(Math.max(0, TimeUnit.SECONDS.toMillis(processLaunchDelaySeconds)
                    - (System.currentTimeMillis() - startTime)));
        } catch (final InterruptedException e) {
            log.error("Process launch job interrupted");
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        // Throw caught exceptions if any are received.
        if (exceptionReceivedDuringCall != null) {
            throw new RuntimeException(exceptionReceivedDuringCall);
        }
        return processUuid;
    }

    /**
     * If the process is alive, reduce the next delay until the min delay is reached.
     * If the process is not alive, reset the delay back to the max delay.
     */
    @VisibleForTesting
    long getNextDelay(long delay, final boolean isProcessAlive) {
        if (isProcessAlive) {
            return Math.max(delay - SUCCESSFUL_LAUNCH_DELAY_REDUCTION_SECONDS, MIN_SECONDS_BETWEEN_PROCESS_LAUNCHES);
        } else {
            return MAX_SECONDS_BETWEEN_PROCESS_LAUNCHES;
        }
    }
}
