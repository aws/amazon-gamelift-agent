/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.gamelift;

import java.util.Date;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class RegisterComputeResponse {
    @NonNull private final String fleetId;
    @NonNull private final String computeName;
    @NonNull private final String sdkWebsocketEndpoint;
    private final String status;
    private final String location;
    private final Date creationTime;
}
