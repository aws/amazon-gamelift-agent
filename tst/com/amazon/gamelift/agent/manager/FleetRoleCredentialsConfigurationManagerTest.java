/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.FleetRoleCredentialsConfiguration;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.websocket.GetFleetRoleCredentialsResponse;
import com.amazon.gamelift.agent.websocket.AgentWebSocket;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FleetRoleCredentialsConfigurationManagerTest {

    @Mock private AgentWebSocket client;
    @Mock private WebSocketConnectionProvider webSocketConnectionProvider;
    @Mock private LoadingCache<String, FleetRoleCredentialsConfiguration> fleetRoleCredentialsConfigurationCache;

    private FleetRoleCredentialsConfigurationManager fleetRoleCredentialsConfigurationManager;
    private static final String TEST_ASSUMED_ROLE_USER_ARN = "TestAssumedRoleUserArn";
    private static final String TEST_ASSUMED_ROLE_ID = "TestAssumedRoleId";
    private static final String TEST_ACCESS_KEY_ID = "TestAccessKeyId";
    private static final String TEST_SECRET_ACCESS_KEY = "TestSecretAccessKey";
    private static final String TEST_SESSION_TOKEN = "TestSessionToken";
    private static final Long TEST_EXPIRATION = System.currentTimeMillis() + Duration.ofMinutes(60).toMillis();
    private static final Long TEST_SHORT_EXPIRATION = System.currentTimeMillis() + Duration.ofSeconds(75).toMillis();
    private static final GetFleetRoleCredentialsResponse RESPONSE = GetFleetRoleCredentialsResponse.builder()
            .assumedRoleUserArn(TEST_ASSUMED_ROLE_USER_ARN)
            .assumedRoleId(TEST_ASSUMED_ROLE_ID)
            .accessKeyId(TEST_ACCESS_KEY_ID)
            .secretAccessKey(TEST_SECRET_ACCESS_KEY)
            .sessionToken(TEST_SESSION_TOKEN)
            .expiration(TEST_EXPIRATION)
            .build();

    private static final FleetRoleCredentialsConfiguration FLEET_ROLE_CREDENTIALS_CONFIGURATION = FleetRoleCredentialsConfiguration.builder()
            .assumedRoleUserArn(TEST_ASSUMED_ROLE_USER_ARN)
            .assumedRoleId(TEST_ASSUMED_ROLE_ID)
            .accessKeyId(TEST_ACCESS_KEY_ID)
            .secretAccessKey(TEST_SECRET_ACCESS_KEY)
            .sessionToken(TEST_SESSION_TOKEN)
            .expiration(TEST_EXPIRATION)
            .build();

    private static final String EXPECTED_TO_STRING_OUTPUT = String.format("AssumedRoleUserArn: [%s] AssumedRoleId: [%s] Expiration: [%s]",
            TEST_ASSUMED_ROLE_USER_ARN,
            TEST_ASSUMED_ROLE_ID,
            TEST_EXPIRATION);

    @BeforeEach
    public void setup() throws AgentException {
        // GIVEN
        when(webSocketConnectionProvider.getCurrentConnection()).thenReturn(client);
        fleetRoleCredentialsConfigurationManager = new FleetRoleCredentialsConfigurationManager(webSocketConnectionProvider);
    }

    @Test
    public void GIVEN_FleetRoleCredentialsConfiguration_WHEN_created_THEN_returnFleetRoleCredentialsConfiguration()
            throws AgentException {
        // GIVEN
        when(client.sendRequest(any(), any(), any())).thenReturn(RESPONSE);

        // WHEN
        FleetRoleCredentialsConfiguration fleetRoleCredentialsConfiguration = fleetRoleCredentialsConfigurationManager
                .getFleetRoleCredentialsConfiguration();

        // THEN
        verify(client).sendRequest(any(), any(), any());
        assertEquals(fleetRoleCredentialsConfiguration, FLEET_ROLE_CREDENTIALS_CONFIGURATION);
        assertEquals(fleetRoleCredentialsConfiguration.toString(), EXPECTED_TO_STRING_OUTPUT);
    }

    @Test
    public void GIVEN_FleetRoleCredentialsConfiguration_WHEN_beforeExpiration_THEN_returnFleetRoleCredentialsConfiguration()
            throws Exception {
        // GIVEN
        when(client.sendRequest(any(), any(), any())).thenReturn(RESPONSE);

        // WHEN
        FleetRoleCredentialsConfiguration fleetRoleCredentialsConfiguration = fleetRoleCredentialsConfigurationManager
                .getFleetRoleCredentialsConfiguration();
        fleetRoleCredentialsConfiguration = fleetRoleCredentialsConfigurationManager.getFleetRoleCredentialsConfiguration();

        // THEN
        verify(client).sendRequest(any(), any(), any());
        assertEquals(fleetRoleCredentialsConfiguration, FLEET_ROLE_CREDENTIALS_CONFIGURATION);
    }

    @Test
    public void GIVEN_FleetRoleCredentialsConfiguration_WHEN_afterExpiration_THEN_returnFleetRoleCredentialsConfiguration()
            throws Exception {
        // GIVEN
        GetFleetRoleCredentialsResponse shortExpirationResponse = GetFleetRoleCredentialsResponse.builder()
                .assumedRoleUserArn(TEST_ASSUMED_ROLE_USER_ARN)
                .assumedRoleId(TEST_ASSUMED_ROLE_ID)
                .accessKeyId(TEST_ACCESS_KEY_ID)
                .secretAccessKey(TEST_SECRET_ACCESS_KEY)
                .sessionToken(TEST_SESSION_TOKEN)
                .expiration(TEST_SHORT_EXPIRATION)
                .build();

        when(client.sendRequest(any(), any(), any()))
                .thenReturn(shortExpirationResponse) // Return the short expiration first
                .thenReturn(RESPONSE); // Second time the client is called, return the long expiration response

        // WHEN
        FleetRoleCredentialsConfiguration fleetRoleCredentialsConfiguration = fleetRoleCredentialsConfigurationManager
                .getFleetRoleCredentialsConfiguration();
        // Perform some calls right away while the value is still cached
        fleetRoleCredentialsConfiguration = fleetRoleCredentialsConfigurationManager.getFleetRoleCredentialsConfiguration();
        fleetRoleCredentialsConfiguration = fleetRoleCredentialsConfigurationManager.getFleetRoleCredentialsConfiguration();
        // Sleep until the expiration has passed, but still within cached time
        Thread.sleep(Duration.ofSeconds(16).toMillis());
        // Now we're past the expiration time, this will invalidate and update the cache with a new value.
        fleetRoleCredentialsConfiguration = fleetRoleCredentialsConfigurationManager.getFleetRoleCredentialsConfiguration();

        // THEN
        // The client should be called once to prime the cache and a second time to refresh the cache due to expiration.
        // All other calls should read from the cache and not reach the client.
        verify(client, times(2)).sendRequest(any(), any(), any());
    }

    @Test
    public void GIVEN_fleetRoleCredentialsConfiguration_WHEN_getFleetRoleCredentials_THEN_convertAndReturn()
            throws AgentException {
        // GIVEN
        when(client.sendRequest(any(), any(), any())).thenReturn(RESPONSE);

        // WHEN
        final AWSCredentialsProvider awsCredentialsProvider = fleetRoleCredentialsConfigurationManager.getFleetRoleCredentials();

        // THEN
        verify(client).sendRequest(any(), any(), any());
        assertEquals(TEST_ACCESS_KEY_ID, awsCredentialsProvider.getCredentials().getAWSAccessKeyId());
        assertEquals(TEST_SECRET_ACCESS_KEY, awsCredentialsProvider.getCredentials().getAWSSecretKey());
    }
}
