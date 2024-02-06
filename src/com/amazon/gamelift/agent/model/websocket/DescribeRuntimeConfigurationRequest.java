/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketRequest;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;

/**
 * Websocket Request for DescribeRuntimeConfiguration
 */
public class DescribeRuntimeConfigurationRequest extends WebsocketRequest {
    /**
     * Constructor for DescribeRuntimeConfigurationRequest
     */
    public DescribeRuntimeConfigurationRequest() {
        setAction(WebSocketActions.DescribeRuntimeConfiguration.name());
    }
}
