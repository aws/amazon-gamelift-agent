/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.constants;

public enum WebSocketActions {
    Default,
    DescribeRuntimeConfiguration,
    ForceExitProcess,
    GetFleetRoleCredentials,
    NotifyGameSessionActivated,
    NotifyProcessRegistered,
    NotifyProcessTermination,
    RefreshConnection,
    SendHeartbeat,
    StartComputeTermination
}
