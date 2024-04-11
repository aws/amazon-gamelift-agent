/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.cache;

import com.amazon.gamelift.agent.model.RuntimeConfiguration;
import com.amazon.gamelift.agent.model.websocket.DescribeRuntimeConfigurationRequest;
import com.amazon.gamelift.agent.model.websocket.DescribeRuntimeConfigurationResponse;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.google.common.cache.CacheLoader;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Duration;

@Slf4j
public class RuntimeConfigCacheLoader extends CacheLoader<String, RuntimeConfiguration> {
    private final WebSocketConnectionProvider webSocketConnectionProvider;
    private static final Duration REFRESH_TIMEOUT = Duration.ofMinutes(1);

    /**
     * Constructor for RuntimeConfigCacheLoader
     * @param webSocketConnectionProvider
     */
    @Inject
    public RuntimeConfigCacheLoader(final WebSocketConnectionProvider webSocketConnectionProvider) {
        this.webSocketConnectionProvider = webSocketConnectionProvider;
    }

    @Override
    public @NonNull RuntimeConfiguration load(final @NonNull String key) throws AgentException {
        log.info("Sending DescribeRuntimeConfigurationRequest.");
        final DescribeRuntimeConfigurationResponse response = webSocketConnectionProvider.getCurrentConnection().sendRequest(
                new DescribeRuntimeConfigurationRequest(), DescribeRuntimeConfigurationResponse.class, REFRESH_TIMEOUT);
        if (response != null) {
            final RuntimeConfiguration loadedRuntimeConfiguration = RuntimeConfiguration.builder()
                    .gameSessionActivationTimeoutSeconds(response.getGameSessionActivationTimeoutSeconds())
                    .maxConcurrentGameSessionActivations(response.getMaxConcurrentGameSessionActivations())
                    .serverProcesses(response.getServerProcesses())
                    .build();
            log.info("DescribeRuntimeConfigurationResponse received. Updating runtime config to: {}",
                    loadedRuntimeConfiguration);
            return loadedRuntimeConfiguration;
        }
        throw new NotFoundException("Failed to load non-empty runtime configuration from Amazon GameLift. "
                + "Runtime configuration cannot be null");
    }
}
