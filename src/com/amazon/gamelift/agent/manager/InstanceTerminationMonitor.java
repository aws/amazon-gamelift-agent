/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.utils.ExecutorServiceSafeRunnable;
import com.amazon.gamelift.agent.module.ThreadingModule;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InstanceTerminationMonitor {
    private static final int PROCESS_MANAGER_TERMINATION_LEEWAY_SECONDS = 30;
    private static final long INITIAL_DELAY_SECONDS = 5;
    private static final long INTERVAL_SECONDS = 5;

    private final AtomicBoolean signalDetected = new AtomicBoolean(false);

    private final TerminationNoticeReader terminationNoticeReader;
    private final ScheduledExecutorService executorService;
    private final ShutdownOrchestrator shutdownOrchestrator;

    /**
     * Constructor for InstanceTerminationMonitor
     * @param terminationNoticeReader
     * @param shutdownOrchestrator
     * @param executorService
     */
    @Inject
    public InstanceTerminationMonitor(
            final TerminationNoticeReader terminationNoticeReader,
            final ShutdownOrchestrator shutdownOrchestrator,
            @Named(ThreadingModule.INSTANCE_TERMINATION_EXECUTOR) final ScheduledExecutorService executorService) {
        this.terminationNoticeReader = terminationNoticeReader;
        this.executorService = executorService;
        this.shutdownOrchestrator = shutdownOrchestrator;
    }

    /**
     * Start InstanceTermination monitoring
     */
    public void start() {
        executorService.scheduleWithFixedDelay(new ExecutorServiceSafeRunnable(this::run),
                INITIAL_DELAY_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("Started a daemon to watch for spot instance termination notice every {} seconds.", INTERVAL_SECONDS);
    }

    @VisibleForTesting
    protected void run() {
        try {
            final Instant terminationTime = terminationNoticeReader.getTerminationNoticeFromLocalFile()
                    .orElse(terminationNoticeReader.getTerminationNoticeFromEC2Metadata().orElse(null));

            if (terminationTime == null) {
                return;
            }

            if (!signalDetected.compareAndSet(false, true)) {
                log.info("Termination notice already detected. Ignoring.");
                return;
            }

            log.info("Termination notice detected.");

            final Instant terminationDeadline =
                    terminationTime.minus(PROCESS_MANAGER_TERMINATION_LEEWAY_SECONDS, ChronoUnit.SECONDS);
            this.shutdownOrchestrator.startTermination(terminationDeadline, true);
        } catch (final Exception e) {
            log.error("Failed to check instance for spot termination", e);
        }
    }
}
