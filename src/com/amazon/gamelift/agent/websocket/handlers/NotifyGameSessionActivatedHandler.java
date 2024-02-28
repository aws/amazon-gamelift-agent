/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.websocket.NotifyGameSessionActivatedMessage;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class NotifyGameSessionActivatedHandler extends MessageHandler<NotifyGameSessionActivatedMessage> {

    private final GameProcessManager gameProcessManager;

    /**
     * Constructor for NotifyGameSessionActivatedHandler
     * See GameLiftAgentWebSocketListener for message routing by Action
     * @param objectMapper
     */
    @Inject
    public NotifyGameSessionActivatedHandler(final ObjectMapper objectMapper,
                                             final GameProcessManager gameProcessManager) {
        super(NotifyGameSessionActivatedMessage.class, objectMapper);
        this.gameProcessManager = gameProcessManager;
    }

    @Override
    public void handle(final NotifyGameSessionActivatedMessage message) {
        log.info("NotifyGameSessionActivatedMessage message received: Process {}", message.getProcessId());

        try {
            gameProcessManager.updateProcessOnGameSessionActivation(message.getProcessId(), message.getGameSessionId());
        } catch (NotFoundException nfe) {
            log.info("Swallowing NotFoundException when saving log paths: {}", nfe.getMessage());
        }
    }
}
