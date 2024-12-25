/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.gamelift.GetComputeAuthTokenResponse;
import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComputeAuthTokenManagerTest {
    @Mock LoadingCache<String, GetComputeAuthTokenResponse> mockComputeAuthTokenCache;

    private static final String FLEET_ID = "fleet-multipleBoats";
    private static final String COMPUTE_NAME = "computeName";
    private static final String COMPUTE_AUTH_TOKEN = "authToken";
    private static final String COMPUTE_AUTH_TOKEN_CACHE_KEY = "computeAuthToken";

    private ComputeAuthTokenManager computeAuthTokenManager;

    @BeforeEach
    public void setup() throws Exception {
        final GetComputeAuthTokenResponse authTokenResponse = GetComputeAuthTokenResponse.builder()
                .authToken(COMPUTE_AUTH_TOKEN)
                .computeName(COMPUTE_NAME)
                .fleetId(FLEET_ID)
                .expirationTimeEpochMillis(Instant.now().plus(Duration.ofMinutes(20)))
                .build();
        lenient().when(mockComputeAuthTokenCache.get(eq(COMPUTE_AUTH_TOKEN_CACHE_KEY))).thenReturn(authTokenResponse);
        computeAuthTokenManager = new ComputeAuthTokenManager(mockComputeAuthTokenCache);
    }

    @Test
    public void GIVEN_computeAuthToken_WHEN_created_THEN_computeAuthToken() throws Exception {
        // WHEN
        final String authToken = computeAuthTokenManager.getComputeAuthToken();

        // THEN
        assertEquals(authToken, COMPUTE_AUTH_TOKEN);
        verify(mockComputeAuthTokenCache, times(1)).get(eq(COMPUTE_AUTH_TOKEN_CACHE_KEY));
    }

    @Test
    public void GIVEN_expiringSoonAuthToken_WHEN_created_THEN_computeAuthToken() throws Exception {
        // GIVEN
        final GetComputeAuthTokenResponse response = GetComputeAuthTokenResponse.builder()
                .authToken(COMPUTE_AUTH_TOKEN)
                .fleetId(FLEET_ID)
                .computeName(COMPUTE_NAME)
                .expirationTimeEpochMillis(Instant.now().plus(Duration.ofMinutes(1)))
                .build();
        when(mockComputeAuthTokenCache.get(eq(COMPUTE_AUTH_TOKEN_CACHE_KEY))).thenReturn(response);

        // WHEN
        final String authToken = computeAuthTokenManager.getComputeAuthToken();

        // THEN
        assertEquals(authToken, COMPUTE_AUTH_TOKEN);
        verify(mockComputeAuthTokenCache, times(2)).get(eq(COMPUTE_AUTH_TOKEN_CACHE_KEY));
    }

    @Test
    public void GIVEN_exception_WHEN_cacheGet_THEN_runtimeException() throws Exception {
        // GIVEN
        doThrow(ExecutionException.class).when(mockComputeAuthTokenCache).get(eq(COMPUTE_AUTH_TOKEN_CACHE_KEY));

        // WHEN-THEN
        assertThrows(RuntimeException.class, () -> computeAuthTokenManager.getComputeAuthToken());
    }
}
