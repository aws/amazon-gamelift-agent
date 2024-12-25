/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Response for GetFleetRoleCredentialsResponse. This is the model for the immediate json output from the websocket.
 * Other classes should use FleetRoleCredentialsConfiguration (pared-down model)
 */
@Builder
@Jacksonized
@Value
@EqualsAndHashCode(callSuper = true)
public class GetFleetRoleCredentialsResponse extends WebsocketResponse {
    @JsonProperty("AssumedRoleUserArn")
    private String assumedRoleUserArn;
    @JsonProperty("AssumedRoleId")
    private String assumedRoleId;
    @JsonProperty("AccessKeyId")
    private String accessKeyId;
    @JsonProperty("SecretAccessKey")
    private String secretAccessKey;
    @JsonProperty("SessionToken")
    private String sessionToken;
    @JsonProperty("Expiration")
    private Long expiration;

    /**
     * Custom toString for GetFleetRoleCredentialsResponse
     * @return
     */
    @Override
    public String toString() {
        final String baseString = super.toString();
        return String.format(baseString + " AssumedRoleUserArn: [%s] AssumedRoleId: [%s] Expiration: [%s]",
                getAssumedRoleUserArn(),
                getAssumedRoleId(),
                getExpiration());
    }
}
