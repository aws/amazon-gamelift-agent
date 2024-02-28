/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class WebsocketRequest extends WebsocketMessage {
    @Setter
    @Getter
    @NonNull
    @JsonProperty(value = "RequestId")
    private String requestId;

    /**
     * Constructor for WebsocketRequest
     */
    public WebsocketRequest() {
        requestId = UUID.randomUUID().toString();
    }
}
