/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.websocket.ForceExitProcessMessage;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
@Deprecated
public class ForceExitProcessHandler extends MessageHandler<ForceExitProcessMessage> {

    private final GameProcessManager gameProcessManager;

    /**
     * Constructor for ForceExitProcessHandler
     * See GameLiftAgentWebSocketListener for message routing by Action
     * @param objectMapper
     * @param gameProcessManager
     */
    @Inject
    public ForceExitProcessHandler(final ObjectMapper objectMapper,
                                   final GameProcessManager gameProcessManager)  {
        super(ForceExitProcessMessage.class, objectMapper);
        this.gameProcessManager = gameProcessManager;
    }

    @Override
    public void handle(final ForceExitProcessMessage message) {
        log.info("Forcing process to exit message received: {}", message);

        if (!WebSocketActions.ForceExitProcess.name().equals(message.getAction())) {
            log.error("Message is not a ForceExitProcess action. Is {}. Not processing",
                    message.getAction());
            return;
        }

        log.info("Force exiting process with UUID: {}", message.getProcessId());
        gameProcessManager.terminateProcessByUUID(message.getProcessId(),
                ProcessTerminationReason.NORMAL_TERMINATION);
    }
}
