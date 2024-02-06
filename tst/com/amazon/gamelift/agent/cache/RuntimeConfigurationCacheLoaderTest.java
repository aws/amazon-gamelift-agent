/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.cache;

import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.websocket.AgentWebSocket;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RuntimeConfigurationCacheLoaderTest {

    private static final String RUNTIME_CONFIG_CACHE_KEY = "runtimeConfiguration";
    @Mock
    private AgentWebSocket client;
    @Mock
    private WebSocketConnectionProvider webSocketConnectionProvider;
    @InjectMocks
    private RuntimeConfigCacheLoader cacheLoader;

    @BeforeEach
    public void setup() {
        when(webSocketConnectionProvider.getCurrentConnection()).thenReturn(client);
    }

    @Test
    public void GIVEN_nullConfig_WHEN_load_THEN_exception() throws AgentException {
        when(client.sendRequest(any(), any(), any())).thenReturn(null);

        assertThrows(NotFoundException.class, () ->
                cacheLoader.load(RUNTIME_CONFIG_CACHE_KEY));
    }

    @Test
    public void GIVEN_exception_WHEN_load_THEN_throwException() throws AgentException {
        given(client.sendRequest(any(), any(), any())).willAnswer( invocation -> { throw mock(TimeoutException.class); });

        assertThrows(TimeoutException.class, () ->
                cacheLoader.load(RUNTIME_CONFIG_CACHE_KEY));
    }
}
