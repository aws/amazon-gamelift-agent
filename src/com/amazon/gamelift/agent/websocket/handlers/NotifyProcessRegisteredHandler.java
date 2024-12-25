/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.websocket.NotifyProcessRegisteredMessage;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.inject.Inject;

@Slf4j
public class NotifyProcessRegisteredHandler extends MessageHandler<NotifyProcessRegisteredMessage> {

    private final GameProcessManager gameProcessManager;

    /**
     * Constructor for NotifyProcessRegisteredHandler
     * See GameLiftAgentWebSocketListener for message routing by Action
     * @param objectMapper
     */
    @Inject
    public NotifyProcessRegisteredHandler(final ObjectMapper objectMapper,
                                          final GameProcessManager gameProcessManager) {
        super(NotifyProcessRegisteredMessage.class, objectMapper);
        this.gameProcessManager = gameProcessManager;
    }

    @Override
    public void handle(final NotifyProcessRegisteredMessage message) {
        log.info("NotifyProcessRegistered message received: Process {}", message.getProcessId());

        try {
            if (CollectionUtils.isNotEmpty(message.getLogPaths())) {
                log.info("Recording log paths for process {}: {}", message.getProcessId(), message.getLogPaths());
                gameProcessManager.updateProcessOnRegistration(message.getProcessId(), message.getLogPaths());
            }
        } catch (final NotFoundException nfe) {
            log.info("Swallowing NotFoundException when saving log paths: {}", nfe.getMessage());
        }
    }
}
