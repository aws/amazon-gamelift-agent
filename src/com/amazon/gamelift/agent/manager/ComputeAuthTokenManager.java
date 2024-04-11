/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.gamelift.GetComputeAuthTokenResponse;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ComputeAuthTokenManager {
    private final LoadingCache<String, GetComputeAuthTokenResponse> computeAuthTokenCache;
    /**
     *  When calling Amazon GameLift to get a ComputeAuthToken, if less than 2 minutes remain on expiration a new
     *  token is generated. If the cached value has less than 2 minutes remaining until expiration it should be
     *  invalidated to force a cache load.
     *
     *  Cache expiration time is set to 5 minutes so that a call is made to Amazon GameLift GetComputeAuthToken
     *  at least once every 5 minutes just as a precaution to check for any potential issue with the existing token.
     */
    private static final Duration CACHE_REFRESH_THRESHOLD = Duration.ofMinutes(2);
    private static final String COMPUTE_AUTH_TOKEN_CACHE_KEY = "computeAuthToken";

    /**
     * Constructor for ComputeAuthTokenManager
     * @param computeAuthTokenCache
     */
    @Inject
    public ComputeAuthTokenManager(final LoadingCache<String, GetComputeAuthTokenResponse> computeAuthTokenCache) {
        this.computeAuthTokenCache = computeAuthTokenCache;
    }

    /**
     * Load ComputeAuthToken
     * @return
     * @throws RuntimeException
     */
    public synchronized String getComputeAuthToken() throws RuntimeException {
        try {
            // See documentation at top of class for reason for these actions
            final GetComputeAuthTokenResponse authTokenResponse = computeAuthTokenCache.get(COMPUTE_AUTH_TOKEN_CACHE_KEY);
            final Instant refreshTime = authTokenResponse.getExpirationTimeEpochMillis().minus(CACHE_REFRESH_THRESHOLD);
            if (Instant.now().isAfter(refreshTime)) {
                computeAuthTokenCache.invalidateAll();
                return computeAuthTokenCache.get(COMPUTE_AUTH_TOKEN_CACHE_KEY).getAuthToken();
            } else {
                return authTokenResponse.getAuthToken();
            }
        } catch (final ExecutionException e) {
            log.error("Caught an exception while loading computeAuthToken from cache", e);
            throw new RuntimeException("Caught an exception while loading computeAuthToken from cache", e);
        }
    }
}
