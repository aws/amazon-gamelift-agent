/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket.base;

import org.apache.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class WebsocketResponse extends WebsocketMessage {
    @JsonProperty(value = "RequestId")
    private String requestId;

    @JsonProperty(value = "StatusCode")
    private Integer statusCode;

    @JsonIgnore
    public boolean isErrorResponse() {
        return (statusCode != null && statusCode != HttpStatus.SC_OK);
    }
}
