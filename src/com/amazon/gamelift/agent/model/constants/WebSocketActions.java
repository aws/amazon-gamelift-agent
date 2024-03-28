/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.constants;

public enum WebSocketActions {
    Default,
    DescribeRuntimeConfiguration,
    // @deprecated
    ForceExitProcess,
    ForceExitServerProcess,
    GetFleetRoleCredentials,
    NotifyGameSessionActivated,
    NotifyProcessRegistered,
    NotifyServerProcessTermination,
    RefreshConnection,
    SendHeartbeat,
    StartComputeTermination
}
