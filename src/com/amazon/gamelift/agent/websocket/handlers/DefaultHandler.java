/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class DefaultHandler extends MessageHandler<WebsocketResponse> {

    /**
     * Constructor for DefaultHandler
     * See GameLiftAgentWebSocketListener for message routing by Action
     * @param objectMapper
     */
    @Inject
    public DefaultHandler(final ObjectMapper objectMapper) {
        super(WebsocketResponse.class, objectMapper);
    }

    @Override
    public void handle(final WebsocketResponse message) {
        log.error("Action {} not supported", message.getAction());
    }
}
