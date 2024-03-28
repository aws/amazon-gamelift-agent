/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.amazon.gamelift.agent.model.websocket.ForceExitServerProcessMessage;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class ForceExitServerProcessHandler extends MessageHandler<ForceExitServerProcessMessage> {

    private final GameProcessManager gameProcessManager;

    /**
     * Constructor for ForceExitServerProcessHandler
     * See GameLiftAgentWebSocketListener for message routing by Action
     * @param objectMapper
     * @param gameProcessManager
     */
    @Inject
    public ForceExitServerProcessHandler(final ObjectMapper objectMapper,
                                         final GameProcessManager gameProcessManager)  {
        super(ForceExitServerProcessMessage.class, objectMapper);
        this.gameProcessManager = gameProcessManager;
    }

    @Override
    public void handle(final ForceExitServerProcessMessage message) {
        log.info("Forcing server process to exit message received: {}", message);

        if (!WebSocketActions.ForceExitServerProcess.name().equals(message.getAction())) {
            log.error("Message is not a ForceExitServerProcess action. Is {}. Not processing",
                    message.getAction());
            return;
        }

        log.info("Force exiting process with UUID: {}", message.getProcessId());
        gameProcessManager.terminateProcessByUUID(message.getProcessId(),
                ProcessTerminationReason.NORMAL_TERMINATION);
    }
}
