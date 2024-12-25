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
import lombok.ToString;

@Data
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NotifyServerProcessTerminationRequest extends WebsocketRequest {
    @JsonProperty(value = "ProcessId")
    private final String processId;
    @JsonProperty(value = "EventCode")
    private final String eventCode;
    @JsonProperty(value = "TerminationReason")
    private final String terminationReason;

    /**
     * Constructor for NotifyServerProcessTerminationRequest
     * @param processId
     * @param eventCode
     * @param terminationReason
     */
    public NotifyServerProcessTerminationRequest(final String processId,
                                                 final String eventCode,
                                                 final String terminationReason) {
        this.processId = processId;
        this.eventCode = eventCode;
        this.terminationReason = terminationReason;
        setAction(WebSocketActions.NotifyServerProcessTermination.name());
    }
}
