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
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

@Slf4j
public class ForceExitServerProcessHandler extends MessageHandler<ForceExitServerProcessMessage> {

    private final GameProcessManager gameProcessManager;
    private static final ProcessTerminationReason DEFAULT_TERMINATION_REASON =
            ProcessTerminationReason.NORMAL_TERMINATION;

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
        ProcessTerminationReason reason;
        try {
            reason = StringUtils.isBlank(message.getTerminationReason())
                    ? DEFAULT_TERMINATION_REASON
                    : ProcessTerminationReason.valueOf(message.getTerminationReason());
        } catch (final IllegalArgumentException e) {
            log.error(String.format("Received ForceExitServerProcess message with unknown termination reason %s. "
                    + "Using default termination reason %s.",
                    message.getTerminationReason(), DEFAULT_TERMINATION_REASON));
            reason = DEFAULT_TERMINATION_REASON;
        }

        log.info("Force exiting server process with UUID: {}", message.getProcessId());

        gameProcessManager.terminateProcessByUUID(message.getProcessId(),
                reason);
    }
}
