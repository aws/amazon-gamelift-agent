/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class should always be injected to ensure a single instance is provided. Do not create new instances of this
 * class when using in other places.
 */
@Slf4j
@RequiredArgsConstructor
public class ExecutorServiceManager {
    @VisibleForTesting
    final Map<String, ScheduledExecutorService> scheduledThreadPoolExecutorServiceMap = new ConcurrentHashMap<>();
    @VisibleForTesting
    final Map<String, ExecutorService> fixedThreadPoolExecutorServiceMap = new ConcurrentHashMap<>();

    private static final String THREAD_NAME_SUFFIX = "-thread-%d";

    /**
     * Creates a ScheduledThreadPoolExecutorService. If it doesn't exist, create one
     * @param threadCount
     * @param name
     * @param setDaemon
     * @return ScheduledThreadPoolExecuterService
     */
    public ScheduledExecutorService getOrCreateScheduledThreadPoolExecutorService(final int threadCount, final String name,
                                                                                  final boolean setDaemon) {
        return scheduledThreadPoolExecutorServiceMap.computeIfAbsent(name, (key) -> {
            final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(threadCount,
                    new ThreadFactoryBuilder()
                            .setNameFormat(name + THREAD_NAME_SUFFIX)
                            .setDaemon(setDaemon)
                            .build());
            return executorService;
        });
    }

    /**
     * Creates a FixedThreadPoolExecutorService. If it doesn't exist, create one
     * @param threadCount
     * @param name
     * @param setDaemon
     * @return FixedThreadPoolExecuterService
     */
    public ExecutorService getOrCreateFixedThreadPoolExecutorService(final int threadCount, final String name,
                                                                     final boolean setDaemon) {
        return fixedThreadPoolExecutorServiceMap.computeIfAbsent(name, (key) -> {
            final ExecutorService executorService = Executors.newFixedThreadPool(threadCount,
                    new ThreadFactoryBuilder()
                            .setNameFormat(name + THREAD_NAME_SUFFIX)
                            .setDaemon(setDaemon)
                            .build());
            return executorService;
        });
    }

    /**
     * Shutdown a ScheduledThreadPoolExecutorService
     * @param name
     */
    public void shutdownScheduledThreadPoolExecutorServiceByName(final String name) {
        final ScheduledExecutorService executorService = scheduledThreadPoolExecutorServiceMap.get(name);
        if (executorService == null) {
            log.warn("ScheduledThreadPoolExecutorService with name {} not found. Taking no action.", name);
            return;
        }
        log.info("Shutting down ScheduledThreadPoolExecutorService by name: {}", name);
        executorService.shutdown();
        scheduledThreadPoolExecutorServiceMap.remove(name);
    }

    /**
     * Wait for ScheduledThreadPoolExecutor to complete with a deadline.
     * Return value true indicates shutdown succeeded or was no-op and false indicates ongoing tasks had not completed
     * at the deadline. This will not throw an exception but execution will be allowed to proceed.
     * @param name
     * @param deadlineMillis
     * @return
     */
    public boolean shutdownScheduledThreadPoolExecutorServiceByName(final String name, final Long deadlineMillis) {
        final ScheduledExecutorService executorService = scheduledThreadPoolExecutorServiceMap.get(name);
        boolean completed = false;
        if (executorService == null) {
            log.warn("ScheduledThreadPoolExecuterService with name {} not found. Taking no action.", name);
            return true;
        }
        log.info("Shutting down ScheduledThreadPoolExecutorService by name: {}", name);
        executorService.shutdown();

        try {
            completed = executorService.awaitTermination(deadlineMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Do Nothing
        }

        if (!completed) {
            log.warn("ScheduledThreadPoolExecutorService {} has not completed in-progress tasks at deadline of {} millis. "
                    + "Removing from map and proceeding.", name, deadlineMillis);
        }
        scheduledThreadPoolExecutorServiceMap.remove(name);
        return completed;
    }

    /**
     * Shutdown all tracked executor services
     */
    public void shutdownExecutorServices() {
        scheduledThreadPoolExecutorServiceMap.entrySet().stream()
                .forEach(entry -> {
                    log.info("Shutting down ScheduledThreadPoolExecutorService: {}", entry.getKey());
                    entry.getValue().shutdown();
                });
        fixedThreadPoolExecutorServiceMap.entrySet().stream()
                .forEach(entry -> {
                    log.info("Shutting down FixedThreadPoolExecutorService: {}", entry.getKey());
                    entry.getValue().shutdown();
                });
        scheduledThreadPoolExecutorServiceMap.clear();
        fixedThreadPoolExecutorServiceMap.clear();
    }
}
