/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ExecutorServiceManagerTest {
    private static final String EXECUTOR_SERVICE_NAME_1 = "executorServiceOne";
    private static final String EXECUTOR_SERVICE_NAME_2 = "executorServiceTwo";
    private static final int DEFAULT_THREAD_COUNT = 1;

    @Test
    public void GIVEN_validInput_WHEN_getOrCreateScheduledThreadPoolExecutorService_THEN_executorCreated() {
        // GIVEN
        final ExecutorServiceManager executorServiceManager = new ExecutorServiceManager();

        // WHEN
        final ScheduledExecutorService executorService =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_1, false);

        // THEN
        assertNotNull(executorService);
    }

    @Test
    public void GIVEN_sameInputTwice_WHEN_getOrCreateScheduledThreadPoolExecutorService_THEN_singleExecutorServiceCreated() {
        // GIVEN
        final ExecutorServiceManager executorServiceManager = new ExecutorServiceManager();

        // WHEN
        final ScheduledExecutorService createCallOne =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_1, false);
        final ScheduledExecutorService createCallTwo =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_1, false);

        // THEN
        assertEquals(createCallOne, createCallTwo);
    }

    @Test
    public void GIVEN_executorServices_WHEN_shutdownExecutorServices_THEN_executorServicesShutDown()
            throws InterruptedException {
        // GIVEN
        final ExecutorServiceManager executorServiceManager = new ExecutorServiceManager();

        // WHEN
        final ScheduledExecutorService executorServiceOne =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_1, false);
        final ScheduledExecutorService executorServiceTwo =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_2, false);
        final ExecutorService executorServiceThree =
                executorServiceManager.getOrCreateFixedThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_2, false);

        executorServiceManager.shutdownExecutorServices();
        Thread.sleep(2000);

        // THEN
        assertTrue(executorServiceOne.isShutdown());
        assertTrue(executorServiceTwo.isShutdown());
        assertTrue(executorServiceThree.isShutdown());
    }

    @Test
    public void GIVEN_validExecutorServiceName_WHEN_shutdownScheduledThreadPoolExecutorServiceByName_THEN_executorServiceShutDown()
            throws InterruptedException {
        // GIVEN
        final ExecutorServiceManager executorServiceManager = new ExecutorServiceManager();

        // WHEN
        final ScheduledExecutorService executorServiceOne =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_1, false);
        final ScheduledExecutorService executorServiceTwo =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_2, false);

        executorServiceManager.shutdownScheduledThreadPoolExecutorServiceByName(EXECUTOR_SERVICE_NAME_1);
        Thread.sleep(2000);

        // THEN
        assertTrue(executorServiceOne.isShutdown());
        assertFalse(executorServiceTwo.isShutdown());
        assertEquals(executorServiceManager.scheduledThreadPoolExecutorServiceMap.size(), 1);
    }

    @Test
    public void GIVEN_executorServiceNameAndDelay_WHEN_shutdownScheduledThreadPoolExecutorServiceByName_THEN_executorServiceShutDown()
            throws InterruptedException {
        // GIVEN
        final ExecutorServiceManager executorServiceManager = new ExecutorServiceManager();

        // WHEN
        final ScheduledExecutorService executorServiceOne =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_1, false);

        executorServiceOne.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                // Do Nothing
            }
        });
        final boolean completed =
                executorServiceManager.shutdownScheduledThreadPoolExecutorServiceByName(EXECUTOR_SERVICE_NAME_1, 200L);
        Thread.sleep(250);

        // THEN
        assertTrue(completed);
        assertTrue(executorServiceOne.isShutdown());
        assertEquals(0, executorServiceManager.scheduledThreadPoolExecutorServiceMap.size());
    }

    @Test
    public void GIVEN_executorNotTerminatedWithinDelay_WHEN_shutdownScheduledThreadPoolExecutorServiceByNameWithDelay_THEN_returnsFalse() {
        // GIVEN
        final ExecutorServiceManager executorServiceManager = new ExecutorServiceManager();

        // WHEN
        final ScheduledExecutorService executorServiceOne =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_1, false);

        executorServiceOne.execute(() -> {
            try {
                Thread.sleep(500);
            } catch (final InterruptedException e) {
                // Do Nothing
            }
        });

        final boolean completed =
                executorServiceManager.shutdownScheduledThreadPoolExecutorServiceByName(EXECUTOR_SERVICE_NAME_1, 100L);

        // THEN
        assertFalse(completed);
        assertEquals(0, executorServiceManager.scheduledThreadPoolExecutorServiceMap.size());
    }

    @Test
    public void GIVEN_invalidExecutorServiceName_WHEN_shutdownExecutorServiceByName_THEN_noOp()
            throws InterruptedException {
        // GIVEN
        final ExecutorServiceManager executorServiceManager = new ExecutorServiceManager();

        // WHEN
        final ScheduledExecutorService executorServiceOne =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_1, false);
        final ScheduledExecutorService executorServiceTwo =
                executorServiceManager.getOrCreateScheduledThreadPoolExecutorService(DEFAULT_THREAD_COUNT,
                        EXECUTOR_SERVICE_NAME_2, false);

        executorServiceManager.shutdownScheduledThreadPoolExecutorServiceByName("invalidName");
        Thread.sleep(2000);

        // THEN
        assertFalse(executorServiceOne.isShutdown());
        assertFalse(executorServiceTwo.isShutdown());
        assertEquals(executorServiceManager.scheduledThreadPoolExecutorServiceMap.size(), 2);
    }

    @Test
    public void GIVEN_validInput_WHEN_getOrCreateFixedThreadPoolExecutorService_THEN_createsFixedThreadES() {
        // GIVEN
        final ExecutorServiceManager executorServiceManager = new ExecutorServiceManager();
        final int threadCount = 1;
        final String namePrefix = "testThreadPool";
        // WHEN
        final ExecutorService actual1 =
                executorServiceManager.getOrCreateFixedThreadPoolExecutorService(threadCount, namePrefix, false);
        final ExecutorService actual2 =
                executorServiceManager.getOrCreateFixedThreadPoolExecutorService(threadCount, namePrefix, false);
        // THEN
        assertEquals(actual1, actual2);
    }
}
