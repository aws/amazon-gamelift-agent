/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SdkWebsocketEndpointProviderTest {

    @Test
    public void GIVEN_noEndpointSet_WHEN_getSdkWebsocketEndpoint_THEN_throwsException() {
        assertThrows(IllegalStateException.class, () -> new SdkWebsocketEndpointProvider().getSdkWebsocketEndpoint());
    }

    @Test
    public void GIVEN_endpointSet_WHEN_getSdkWebsocketEndpoint_THEN_returnsException() {
        SdkWebsocketEndpointProvider provider = new SdkWebsocketEndpointProvider();
        provider.setSdkWebsocketEndpoint("Test");
        assertEquals("Test", provider.getSdkWebsocketEndpoint());
    }
}
