/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import com.amazon.gamelift.agent.model.constants.GameLiftCredentials;

import com.amazon.gamelift.agent.model.constants.LogCredentials;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * Class to encapsulate the commandline arguments for the GameLift agent.
 */
@Builder
@Value
public class AgentArgs {
    private RuntimeConfiguration runtimeConfiguration;
    @NonNull private String fleetId;
    private String computeName;
    @NonNull private String region;
    private String gameLiftEndpointOverride;
    private String gameLiftAgentWebsocketEndpoint;
    private String gameLiftSdkWebsocketEndpoint;
    private String ipAddress;
    private String certificatePath;
    private String dnsName;
    private String location;
    private String concurrentExecutions;
    private GameLiftCredentials gameLiftCredentials;
    private String gameSessionLogBucket;
    private String agentLogBucket;
    private String agentLogPath;
    private LogCredentials logCredentials;
    private Boolean isContainerFleet;
    private Boolean enableComputeRegistrationViaAgent;
    private Instant heartbeatTimeoutTime;
}
