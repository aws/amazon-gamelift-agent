/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SendHeartbeatResponse extends WebsocketResponse {
    @JsonProperty(value = "Status")
    private String status;
    @JsonProperty(value = "UnhealthyProcesses")
    private List<String> unhealthyProcesses;
    @JsonProperty(value = "UnregisteredProcesses")
    private List<String> unregisteredProcesses;
}
