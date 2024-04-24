/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class to suppress all exceptions from a given Runnable. ScheduledExecutorServices have particularly bad
 * behavior where all future scheduled runs will be cancelled if an unexpected exception gets thrown in the runnable.
 * Using this helper avoids executors getting into a nasty unrecoverable state because of a single exception.
 */
@Slf4j
@RequiredArgsConstructor
public class ExecutorServiceSafeRunnable implements Runnable {

    private final Runnable childRunnable;

    @Override
    public void run() {
        try {
            this.childRunnable.run();
        } catch (final Exception e) {
            // Do not re-throw exceptions from this runnable. If an unhandled exception is thrown in the runnable
            // of a ScheduledExecutorService, all future executions will be silently stopped.
            log.error("Suppressing unexpected exception to prevent impact to future ExecutorService runs", e);
        }
    }
}
