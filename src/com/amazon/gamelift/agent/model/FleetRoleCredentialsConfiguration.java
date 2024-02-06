/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Value
public class FleetRoleCredentialsConfiguration {
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
     * Custom toString for fleetRoleCredentialsConfiguration
     * @return
     */
    @Override
    public String toString() {
        return String.format("AssumedRoleUserArn: [%s] AssumedRoleId: [%s] Expiration: [%s]",
                getAssumedRoleUserArn(),
                getAssumedRoleId(),
                getExpiration());
    }
}
