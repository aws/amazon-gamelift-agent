/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.cache.FleetRoleCredentialsCacheLoader;
import com.amazon.gamelift.agent.model.FleetRoleCredentialsConfiguration;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

@Slf4j
public class FleetRoleCredentialsConfigurationManager {

    private static final String FLEET_ROLE_CREDENTIALS_CONFIGURATION_CACHE_KEY = "fleetRoleCredentialsConfiguration";
    private static final int MAX_CACHE_ENTRIES = 1;
    private static final Duration CACHE_EXPIRATION_MINUTES = Duration.ofMinutes(60);
    private static final Duration CACHE_REFRESH_MINUTES = Duration.ofMinutes(45);
    private static final long CACHE_FORCE_REFRESH_MILLIS = Duration.ofMinutes(1).toMillis();

    private final LoadingCache<String, FleetRoleCredentialsConfiguration> fleetRoleCredentialsConfigurationCache;

    /**
     * Constructor for FleetRoleCredentialsConfigurationManager
     * @param webSocketConnectionProvider
     */
    public FleetRoleCredentialsConfigurationManager(final WebSocketConnectionProvider webSocketConnectionProvider) {
        this.fleetRoleCredentialsConfigurationCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_CACHE_ENTRIES)
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES)
                .refreshAfterWrite(CACHE_REFRESH_MINUTES)
                .recordStats()
                .build(new FleetRoleCredentialsCacheLoader(webSocketConnectionProvider));
    }

    /**
     * Gets FleetRoleCredentialsConfiguration stored in cache
     * @return
     */
    public synchronized FleetRoleCredentialsConfiguration getFleetRoleCredentialsConfiguration() {
        try {
            FleetRoleCredentialsConfiguration inCache = fleetRoleCredentialsConfigurationCache
                    .get(FLEET_ROLE_CREDENTIALS_CONFIGURATION_CACHE_KEY);
            if (System.currentTimeMillis() + CACHE_FORCE_REFRESH_MILLIS >= inCache.getExpiration()) {
                // Force-reload cached credentials are within a minute or passed expired
                log.info("Fleet Role credentials are about to expire, forcing refresh.");
                fleetRoleCredentialsConfigurationCache.invalidate(FLEET_ROLE_CREDENTIALS_CONFIGURATION_CACHE_KEY);
                return fleetRoleCredentialsConfigurationCache.get(FLEET_ROLE_CREDENTIALS_CONFIGURATION_CACHE_KEY);
            }
            return inCache;

        } catch (ExecutionException e) {
            log.error("Caught an exception while loading FleetRoleCredentials from cache", e);
            throw new RuntimeException("Caught an exception while loading FleetRoleCredentials from cache", e);
        }
    }

    /**
     * Retrieve a static credential provider with short-lived credentials
     * @return
     */
    public AWSCredentialsProvider getFleetRoleCredentials() {
        final FleetRoleCredentialsConfiguration configuration = getFleetRoleCredentialsConfiguration();
        final AWSSessionCredentials sessionCredentials = new BasicSessionCredentials(
                configuration.getAccessKeyId(), configuration.getSecretAccessKey(), configuration.getSessionToken());
        return new AWSStaticCredentialsProvider(sessionCredentials);
    }
}
