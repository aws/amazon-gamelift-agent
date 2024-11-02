/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.exception.MalformedRequestException;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Interface class for handlers for messages received over the web socket
 */
@Slf4j
public abstract class MessageHandler<T extends WebsocketMessage> {
    private final ObjectMapper objectMapper;
    private final Class<T> clazz;

    /**
     * Constructor for MessageHandler
     * @param clazz
     * @param objectMapper
     */
    public MessageHandler(final Class<T> clazz, final ObjectMapper objectMapper) {
        this.clazz = clazz;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle a message (as string)
     * @param message
     * @throws MalformedRequestException
     */
    public void handle(final String message) throws MalformedRequestException {
        try {
            handle(objectMapper.readValue(message, clazz));
        } catch (final JsonProcessingException e) {
            log.error("Failed to parse Websocket message: {}", message, e);
            throw new MalformedRequestException(String.format("Could not parse message %s", message), e);
        }
    }

    /**
     * Handle message
     * @param message
     */
    public abstract void handle(T message);
}
