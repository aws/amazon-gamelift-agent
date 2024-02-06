/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketRequest;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SendHeartbeatRequest extends WebsocketRequest {
    @JsonProperty(value = "Status")
    @NonNull
    private final String status;
    @JsonProperty(value = "ProcessList")
    @NonNull
    private final List<String> processList;
    @JsonProperty(value = "HeartbeatTimeMillis")
    private final long heartbeatTimeMillis;

    /**
     * Constructor for SendHeartbeatRequest
     * @param status
     * @param processList
     * @param heartbeatTimeMillis
     */
    public SendHeartbeatRequest(final String status, final List<String> processList, final long heartbeatTimeMillis) {
        this.status = status;
        this.processList = processList;
        this.heartbeatTimeMillis = heartbeatTimeMillis;
        setAction(WebSocketActions.SendHeartbeat.name());
    }
}
