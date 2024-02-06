/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.inject.Singleton;

/**
 * Simple singleton provider class for the SDK Websocket Endpoint.
 * This should only ever be set once when the compute is registered when the GameLiftAgent starts,
 * so an exception should ideally never be thrown by the getter.
 */
@Singleton
@NoArgsConstructor
public class SdkWebsocketEndpointProvider {

    @Setter(AccessLevel.PACKAGE)
    private String sdkWebsocketEndpoint = null;

    /**
     * Getter for sdkWebsocketEndpoint. IllegalStateException if null
     * @return
     */
    public String getSdkWebsocketEndpoint() {
        if (sdkWebsocketEndpoint == null) {
            throw new IllegalStateException("Attempted to retrieve SDK Websocket Endpoint "
                    + "before it was set by RegisterCompute");
        }

        return sdkWebsocketEndpoint;
    }
}
