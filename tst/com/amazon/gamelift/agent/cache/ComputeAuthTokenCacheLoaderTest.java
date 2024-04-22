/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.cache;

import com.amazon.gamelift.agent.model.gamelift.GetComputeAuthTokenResponse;
import com.amazon.gamelift.agent.client.AmazonGameLiftClientWrapper;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ComputeAuthTokenCacheLoaderTest {
    private static final String COMPUTE_AUTH_TOKEN_CACHE_KEY = "computeAuthToken";
    private static final String COMPUTE_AUTH_TOKEN = "AuthToken";
    private static final String FLEET_ID = "fleet-definitelyRealFleet";
    private static final String COMPUTE_NAME = "computeName";

    @Mock private AmazonGameLiftClientWrapper mockGameLift;

    private ComputeAuthTokenCacheLoader cacheLoader;

    @BeforeEach
    public void setup() throws Exception {
        final GetComputeAuthTokenResponse response = GetComputeAuthTokenResponse.builder()
                .authToken(COMPUTE_AUTH_TOKEN)
                .fleetId(FLEET_ID)
                .computeName(COMPUTE_NAME)
                .expirationTimeEpochMillis(Instant.now().plus(Duration.ofMinutes(15)))
                .build();
        cacheLoader = new ComputeAuthTokenCacheLoader(mockGameLift, FLEET_ID, COMPUTE_NAME);
        lenient().when(mockGameLift.getComputeAuthToken(any())).thenReturn(response);
    }

    @Test
    public void GIVEN_exception_WHEN_load_THEN_throwException() throws Exception {
        doThrow(UnauthorizedException.class).when(mockGameLift).getComputeAuthToken(any());

        assertThrows(RuntimeException.class, () -> cacheLoader.load(COMPUTE_AUTH_TOKEN_CACHE_KEY));
    }

    @Test
    public void GIVEN_validInput_WHEN_load_THEN_validOutput() {
        final String authToken = cacheLoader.load(COMPUTE_AUTH_TOKEN_CACHE_KEY).getAuthToken();

        assertEquals(COMPUTE_AUTH_TOKEN, authToken);
    }
}
