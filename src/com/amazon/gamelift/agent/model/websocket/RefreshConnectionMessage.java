/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RefreshConnectionMessage extends WebsocketMessage {
    @JsonProperty("RefreshConnectionEndpoint")
    private String refreshConnectionEndpoint;
    @ToString.Exclude
    @JsonProperty("AuthToken")
    private String authToken;
    @JsonProperty(value = "ExpirationTime")
    private Instant expirationTime;
}
