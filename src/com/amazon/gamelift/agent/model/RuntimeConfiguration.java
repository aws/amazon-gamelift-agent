/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
@Value
public class RuntimeConfiguration {
    @JsonProperty("GameSessionActivationTimeoutSeconds")
    private final Integer gameSessionActivationTimeoutSeconds;
    @JsonProperty("MaxConcurrentGameSessionActivations")
    private final Integer maxConcurrentGameSessionActivations;
    @NonNull
    @JsonProperty("ServerProcesses")
    private final List<GameProcessConfiguration> serverProcesses;
}

