/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazon.gamelift.agent.websocket.SdkWebsocketEndpointProvider;

import java.util.Map;

import static com.amazon.gamelift.agent.manager.ProcessEnvironmentManager.ENV_VAR_COMPUTE_AUTH_TOKEN;
import static com.amazon.gamelift.agent.manager.ProcessEnvironmentManager.ENV_VAR_FLEET_ID;
import static com.amazon.gamelift.agent.manager.ProcessEnvironmentManager.ENV_VAR_HOST_ID;
import static com.amazon.gamelift.agent.manager.ProcessEnvironmentManager.ENV_VAR_PROCESS_ID;
import static com.amazon.gamelift.agent.manager.ProcessEnvironmentManager.ENV_VAR_WEBSOCKET_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ProcessEnvironmentManagerTest {

    private static final String TEST_WEBSOCKET_URL = "testWebSocketUrl";
    private static final String TEST_AUTH_TOKEN = "testAuthToken";
    private static final String TEST_FLEET_ID = "testFleetId";
    private static final String TEST_COMPUTE_ID = "testComputeId";
    private static final String TEST_PROCESS_ID = "testProcessId";

    @Mock private ComputeAuthTokenManager mockComputeAuthTokenManager;
    @Mock private SdkWebsocketEndpointProvider mockSdkWebsocketEndpointProvider;

    @BeforeEach
    public void setup() {
        when(mockComputeAuthTokenManager.getComputeAuthToken()).thenReturn(TEST_AUTH_TOKEN);
        when(mockSdkWebsocketEndpointProvider.getSdkWebsocketEndpoint()).thenReturn(TEST_WEBSOCKET_URL);
    }

    @Test
    public void GIVEN_noNullMembers_WHEN_getProcessEnvironmentVariables_THEN_success() {
        // GIVEN
        ProcessEnvironmentManager processEnvironmentManager = new ProcessEnvironmentManager(
                TEST_FLEET_ID, TEST_COMPUTE_ID, mockComputeAuthTokenManager, mockSdkWebsocketEndpointProvider);

        // WHEN
        final Map<String, String> envVars = processEnvironmentManager.getProcessEnvironmentVariables(TEST_PROCESS_ID);

        // THEN
        assertEquals(5, envVars.size());
        assertTrue(envVars.containsKey(ENV_VAR_WEBSOCKET_URL));
        assertEquals(TEST_WEBSOCKET_URL, envVars.get(ENV_VAR_WEBSOCKET_URL));
        assertTrue(envVars.containsKey(ENV_VAR_COMPUTE_AUTH_TOKEN));
        assertEquals(TEST_AUTH_TOKEN, envVars.get(ENV_VAR_COMPUTE_AUTH_TOKEN));
        assertTrue(envVars.containsKey(ENV_VAR_FLEET_ID));
        assertEquals(TEST_FLEET_ID, envVars.get(ENV_VAR_FLEET_ID));
        assertTrue(envVars.containsKey(ENV_VAR_HOST_ID));
        assertEquals(TEST_COMPUTE_ID, envVars.get(ENV_VAR_HOST_ID));
        assertTrue(envVars.containsKey(ENV_VAR_PROCESS_ID));
        assertEquals(TEST_PROCESS_ID, envVars.get(ENV_VAR_PROCESS_ID));
    }

    @Test
    public void GIVEN_nullMembers_WHEN_getProcessEnvironmentVariables_THEN_success() {
        // GIVEN
        when(mockComputeAuthTokenManager.getComputeAuthToken()).thenReturn(null);
        when(mockSdkWebsocketEndpointProvider.getSdkWebsocketEndpoint()).thenReturn(null);
        ProcessEnvironmentManager processEnvironmentManager = new ProcessEnvironmentManager(
                null, null, mockComputeAuthTokenManager, mockSdkWebsocketEndpointProvider);

        // WHEN
        final Map<String, String> envVars = processEnvironmentManager.getProcessEnvironmentVariables(null);

        // THEN
        assertEquals(5, envVars.size());
        assertTrue(envVars.containsKey(ENV_VAR_WEBSOCKET_URL));
        assertEquals("", envVars.get(ENV_VAR_WEBSOCKET_URL));
        assertTrue(envVars.containsKey(ENV_VAR_COMPUTE_AUTH_TOKEN));
        assertEquals("", envVars.get(ENV_VAR_COMPUTE_AUTH_TOKEN));
        assertTrue(envVars.containsKey(ENV_VAR_FLEET_ID));
        assertEquals("", envVars.get(ENV_VAR_FLEET_ID));
        assertTrue(envVars.containsKey(ENV_VAR_HOST_ID));
        assertEquals("", envVars.get(ENV_VAR_HOST_ID));
        assertTrue(envVars.containsKey(ENV_VAR_PROCESS_ID));
        assertEquals("", envVars.get(ENV_VAR_PROCESS_ID));
    }

    @Test
    public void GIVEN_matchingValue_WHEN_getPrintableEnvVars_THEN_redactAuthItem() {
        // GIVEN
        ProcessEnvironmentManager processEnvironmentManager = new ProcessEnvironmentManager(
                TEST_FLEET_ID, TEST_COMPUTE_ID, mockComputeAuthTokenManager, mockSdkWebsocketEndpointProvider);
        // WHEN
        final String output = processEnvironmentManager.getPrintableEnvironmentVariables(TEST_PROCESS_ID);
        // THEN
        final String expectedValue = ENV_VAR_WEBSOCKET_URL + "=\"" + TEST_WEBSOCKET_URL + "\", "
                + ENV_VAR_FLEET_ID + "=\"" + TEST_FLEET_ID + "\", "
                + ENV_VAR_HOST_ID + "=\"" + TEST_COMPUTE_ID + "\", "
                + ENV_VAR_PROCESS_ID + "=\"" + TEST_PROCESS_ID + "\"";
        assertEquals(expectedValue, output);
    }
}
