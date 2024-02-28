/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.gamelift;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;

@Data
@Builder
public class GetComputeAuthTokenResponse {
    @NonNull private final String fleetId;
    @NonNull private final String computeName;
    @NonNull private final String authToken;
    @NonNull private final Instant expirationTimeEpochMillis;
}
