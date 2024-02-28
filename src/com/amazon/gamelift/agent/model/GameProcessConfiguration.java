/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *  Model that represents the ServerProcess field in the Amazon GameLift RuntimeConfiguration
 */
@Builder
@Jacksonized
@Value
public class GameProcessConfiguration {
    private static final int DEFAULT_CONCURRENT_EXECUTIONS = 1;

    @NonNull
    @JsonProperty("ConcurrentExecutions")
    private final Integer concurrentExecutions;
    @NonNull
    @JsonProperty("LaunchPath")
    private final String launchPath;
    @JsonProperty("Parameters")
    private final String parameters;
    public Integer getConcurrentExecutions() {
        return Objects.isNull(concurrentExecutions) ? DEFAULT_CONCURRENT_EXECUTIONS : concurrentExecutions;
    }

    public List<String> getParameters() {
        return (parameters == null) ? new ArrayList<>() : Arrays.asList(parameters.split(" "));
    }
}
