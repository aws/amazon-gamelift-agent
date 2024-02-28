/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.cache;

import com.amazon.gamelift.agent.model.FleetRoleCredentialsConfiguration;
import com.amazon.gamelift.agent.model.websocket.GetFleetRoleCredentialsRequest;
import com.amazon.gamelift.agent.model.websocket.GetFleetRoleCredentialsResponse;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.google.common.cache.CacheLoader;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Duration;

@Slf4j
public class FleetRoleCredentialsCacheLoader extends CacheLoader<String, FleetRoleCredentialsConfiguration> {

    private final WebSocketConnectionProvider webSocketConnectionProvider;
    private static final Duration REFRESH_TIMEOUT = Duration.ofMinutes(1);

    /**
     * Constructor for FleetRoleCredentialsCacheLoader
     * @param webSocketConnectionProvider
     */
    @Inject
    public FleetRoleCredentialsCacheLoader(final WebSocketConnectionProvider webSocketConnectionProvider) {
        this.webSocketConnectionProvider = webSocketConnectionProvider;
    }

    @Override
    public FleetRoleCredentialsConfiguration load(final String key) throws AgentException {
        log.info("Sending GetFleetRoleCredentials Request.");
        final GetFleetRoleCredentialsResponse response = webSocketConnectionProvider.getCurrentConnection()
                .sendRequest(new GetFleetRoleCredentialsRequest(), GetFleetRoleCredentialsResponse.class, REFRESH_TIMEOUT);
        if (response != null) {
            final FleetRoleCredentialsConfiguration loadedFleetRoleCredentialsConfiguration = FleetRoleCredentialsConfiguration.builder()
                    .assumedRoleUserArn(response.getAssumedRoleUserArn())
                    .assumedRoleId(response.getAssumedRoleId())
                    .accessKeyId(response.getAccessKeyId())
                    .secretAccessKey(response.getSecretAccessKey())
                    .sessionToken(response.getSessionToken())
                    .expiration(response.getExpiration())
                    .build();
            log.info("GetFleetRoleCredentialsResponse received. Updating FleetRoleCredentials- {}",
                    loadedFleetRoleCredentialsConfiguration);
            return loadedFleetRoleCredentialsConfiguration;
        }
        throw new NotFoundException("Failed to load non-empty FleetRoleCredentials configuration from Amazon GameLift. "
            + "FleetRoleCredentials cannot be null");
    }
}
