/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketRequest;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.RandomStringUtils;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GetFleetRoleCredentialsRequest extends WebsocketRequest {
    @JsonProperty("RoleSessionName")
    private String roleSessionName;

    private static final String ROLE_SESSION_NAME_PREFIX = "GameLiftAgentSession";
    private static final String ROLE_SESSION_SUFFIX = RandomStringUtils.randomAlphanumeric(5);
    /**
     * Constructor for GetFleetRoleCredentialsRequest
     */
    public GetFleetRoleCredentialsRequest() {
        this.roleSessionName = String.format("%s-%s", ROLE_SESSION_NAME_PREFIX, ROLE_SESSION_SUFFIX);
        setAction(WebSocketActions.GetFleetRoleCredentials.name());
    }
}
