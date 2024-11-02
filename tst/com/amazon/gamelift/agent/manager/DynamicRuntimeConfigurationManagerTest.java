/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.RuntimeConfiguration;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.websocket.DescribeRuntimeConfigurationResponse;
import com.amazon.gamelift.agent.websocket.AgentWebSocket;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DynamicRuntimeConfigurationManagerTest {

    private static final int TIMEOUT_SECS = 60;
    private static final int CONCURRENT_ACTIVATIONS = 5;
    private static final RuntimeConfiguration RUNTIME_CONFIG = RuntimeConfiguration.builder()
            .gameSessionActivationTimeoutSeconds(TIMEOUT_SECS)
            .maxConcurrentGameSessionActivations(CONCURRENT_ACTIVATIONS)
            .serverProcesses(Lists.newArrayList()).build();

    private static final DescribeRuntimeConfigurationResponse RESPONSE = DescribeRuntimeConfigurationResponse.builder()
            .gameSessionActivationTimeoutSeconds(TIMEOUT_SECS)
            .maxConcurrentGameSessionActivations(CONCURRENT_ACTIVATIONS)
            .serverProcesses(Lists.newArrayList()).build();
    @Mock
    private AgentWebSocket client;
    @Mock
    private WebSocketConnectionProvider webSocketConnectionProvider;
    private DynamicRuntimeConfigurationManager dynamicRuntimeConfigManager;

    @BeforeEach
    public void setup() throws AgentException {
        when(webSocketConnectionProvider.getCurrentConnection()).thenReturn(client);
        when(client.sendRequest(any(), any(), any())).thenReturn(RESPONSE);
        dynamicRuntimeConfigManager = new DynamicRuntimeConfigurationManager(webSocketConnectionProvider);
    }

    @Test
    public void GIVEN_runtimeConfig_WHEN_created_THEN_runtimeConfigReturned() throws AgentException {
        RuntimeConfiguration config = dynamicRuntimeConfigManager.getRuntimeConfiguration();
        assertEquals(config, RUNTIME_CONFIG);
        verify(client, times(1)).sendRequest(any(), any(), any());
    }

    @Test
    public void GIVEN_runtimeConfig_WHEN_beforeExpiration_THEN_cacheReturned() throws Exception {
        RuntimeConfiguration config = dynamicRuntimeConfigManager.getRuntimeConfiguration();
        dynamicRuntimeConfigManager.getRuntimeConfiguration();
        assertEquals(config, RUNTIME_CONFIG);
        verify(client, times(1)).sendRequest(any(), any(), any());
    }
}
