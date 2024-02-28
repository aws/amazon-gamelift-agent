/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class StartComputeTerminationMessage extends WebsocketMessage {
    /**
     * No additional fields are required for this message beyond what is on WebsocketMessage. This is created for
     * clarity of code and to have it in place for when/if more information is added to this message.
     */
}
