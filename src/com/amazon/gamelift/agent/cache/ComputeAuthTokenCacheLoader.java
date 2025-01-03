/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.cache;

import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.gamelift.GetComputeAuthTokenResponse;
import com.amazon.gamelift.agent.client.AmazonGameLiftClientWrapper;
import com.amazon.gamelift.agent.utils.RetryHelper;
import com.amazonaws.services.gamelift.model.GetComputeAuthTokenRequest;
import com.google.common.cache.CacheLoader;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ComputeAuthTokenCacheLoader extends CacheLoader<String, GetComputeAuthTokenResponse> {
    private static final int MAX_GET_COMPUTE_AUTH_TOKEN_RETRIES = 16;

    private final AmazonGameLiftClientWrapper amazonGameLift;
    private final String fleetId;
    private final String computeName;

    /**
     * Constructor for ComputeAuthTokenCacheLoader
     * @param amazonGameLift
     * @param fleetId
     * @param computeName
     */
    public ComputeAuthTokenCacheLoader(final AmazonGameLiftClientWrapper amazonGameLift,
                                       final String fleetId,
                                       final String computeName) {
        this.amazonGameLift = amazonGameLift;
        this.fleetId = fleetId;
        this.computeName = computeName;
    }

    @Override
    public @NonNull GetComputeAuthTokenResponse load(final @NonNull String key) throws RuntimeException {
        log.info("Loading ComputeAuthToken into cache via Amazon GameLift GetComputeAuthToken call.");
        try {
            final GetComputeAuthTokenRequest getComputeAuthTokenRequest = new GetComputeAuthTokenRequest()
                    .withFleetId(fleetId)
                    .withComputeName(computeName);

            return RetryHelper.runRetryable(MAX_GET_COMPUTE_AUTH_TOKEN_RETRIES, true, () -> amazonGameLift.getComputeAuthToken(getComputeAuthTokenRequest));
        } catch (final AgentException e) {
            log.error("Call to Amazon GameLift GetComputeAuthToken failed.", e);
            throw new RuntimeException(e);
        }
    }
}
