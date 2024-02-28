/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Response for DescribeRuntimeConfiguration. This is the model for the immediate json output from the websocket.
 * Other classes should use RuntimeConfiguration (pared-down model)
 */
@Builder
@Jacksonized
@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DescribeRuntimeConfigurationResponse extends WebsocketResponse {
    @JsonProperty("GameSessionActivationTimeoutSeconds")
    private Integer gameSessionActivationTimeoutSeconds;
    @JsonProperty("MaxConcurrentGameSessionActivations")
    private Integer maxConcurrentGameSessionActivations;
    @NonNull
    @JsonProperty("ServerProcesses")
    private List<GameProcessConfiguration> serverProcesses;
}
