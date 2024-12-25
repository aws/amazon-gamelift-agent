/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.gamelift;

import java.util.Date;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class RegisterComputeResponse extends WebsocketResponse {
    @NonNull private final String fleetId;
    @NonNull private final String computeName;
    @NonNull private final String sdkWebsocketEndpoint;
    @NonNull private final String agentWebsocketEndpoint;
    private final String status;
    private final String location;
    private final Date creationTime;
}
