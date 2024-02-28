/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

import com.amazon.gamelift.agent.model.exception.AgentException;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryHelper {
    private static final int EXPONENTIAL_BACKOFF_FACTOR_MS = 500;
    private static final double MAX_JITTER_RANDOMIZATION_FACTOR = 0.25;

    private static boolean forceDisableBackoff = false;

    /**
     * Utility method to retry a Callable when it throws an exception
     *
     * @param numRetries Maximum attempts to retry
     * @param exponentialBackoff Whether to wait an exponentially longer time between retry attempts
     * @param func Callable to retry
     * @return Arbitrary return value from func
     * @throws Exception
     */
    public static <V> V runRetryable(int numRetries, boolean exponentialBackoff, Callable<V> func)
            throws AgentException {
        double retryAttempt = 0;
        boolean shouldRetry = true;
        Exception lastExceptionEncountered = null;
        while (retryAttempt <= numRetries) {
           try {
               return func.call();
           } catch (Exception e) {
                double jitterRandomizationFactor = ThreadLocalRandom.current()
                        .nextDouble(1 - MAX_JITTER_RANDOMIZATION_FACTOR, 1 + MAX_JITTER_RANDOMIZATION_FACTOR);
                long sleepIntervalMs = Math.round(Math.pow(2.0, retryAttempt) * EXPONENTIAL_BACKOFF_FACTOR_MS * jitterRandomizationFactor);
                lastExceptionEncountered = e;
                retryAttempt++;
                log.warn("Action failed attempt {} / {}", retryAttempt, numRetries + 1, e);

                // If the exception received is a modeled exception in the GameLiftAgent code, see if a retry should be
                // performed. If the exception isn't modeled in the GameLiftAgent code, retry by default.
                if (e instanceof AgentException) {
                    shouldRetry = ((AgentException) e).isRetryable();
                }

                if (exponentialBackoff && shouldRetry && !forceDisableBackoff && retryAttempt <= numRetries) {
                    log.info("Waiting {} milliseconds before retrying action.", sleepIntervalMs);
                    try {
                        Thread.sleep(sleepIntervalMs);
                    } catch (InterruptedException ex) {
                        log.error("Retryable action was interrupted.");
                        throw new RuntimeException(ex);
                    }
                } else if (!shouldRetry) {
                    log.warn("Exception type identified as not retryable, skipping retries. Exception was {}", e.getClass());
                    break;
                }
           }
        }

        log.error("Action failed after all retry attempts");
        if (lastExceptionEncountered instanceof AgentException) {
            throw (AgentException) lastExceptionEncountered;
        } else {
            throw new RuntimeException("Action failed after all retry attempts", lastExceptionEncountered);
        }
    }

    /**
     * Default number of retries with exponential backoff enabled
     *
     * @param func Callable to retry
     * @throws Exception
     */
    public static <V> V runRetryable(Callable<V> func) throws AgentException {
        final int defaultNumRetries = 2;
        return RetryHelper.runRetryable(defaultNumRetries, true, func);
    }

    /**
     * Disables exponential backoff for this class, useful for testing.
     */
    public static void disableBackoff() {
        forceDisableBackoff = true;
    }
}
