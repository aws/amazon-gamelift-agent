/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.cache.RuntimeConfigCacheLoader;
import com.amazon.gamelift.agent.model.RuntimeConfiguration;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This instance of the RuntimeConfigurationManager obtains runtime config from Amazon GameLift over the
 * websocket connection.  It is periodically refreshed in the background to be able to pick
 * up changes to process count, etc on the fly.
 */
@Slf4j
public class DynamicRuntimeConfigurationManager implements RuntimeConfigurationManager {
    private LoadingCache<String, RuntimeConfiguration> runtimeConfigCache;
    private static final String RUNTIME_CONFIG_CACHE_KEY = "runtimeConfiguration";
    private static final int MAX_CACHE_ENTRIES = 1;
    private static final Duration CACHE_EXPIRATION_TIME = Duration.ofMinutes(5);

    /**
     * Constructor for DynamicRuntimeConfigurationManager
     * @param webSocketConnectionProvider
     */
    public DynamicRuntimeConfigurationManager(final WebSocketConnectionProvider webSocketConnectionProvider) {
        this.runtimeConfigCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_CACHE_ENTRIES)
                .expireAfterWrite(CACHE_EXPIRATION_TIME.toMillis(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build(new RuntimeConfigCacheLoader(webSocketConnectionProvider));
    }

    @Override
    public synchronized RuntimeConfiguration getRuntimeConfiguration() {
        try {
            return runtimeConfigCache.get(RUNTIME_CONFIG_CACHE_KEY);
        } catch (ExecutionException e) {
            log.error("Caught an exception while loading runtimeConfiguration from cache", e);
            throw new RuntimeException("Caught an exception while loading runtimeConfiguration from cache", e);
        }
    }
}
