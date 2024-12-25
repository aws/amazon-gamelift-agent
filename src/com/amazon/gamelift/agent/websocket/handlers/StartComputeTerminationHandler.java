/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.websocket.StartComputeTerminationMessage;
import com.amazon.gamelift.agent.manager.ShutdownOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Instant;

@Slf4j
public class StartComputeTerminationHandler extends MessageHandler<StartComputeTerminationMessage> {

    private final ShutdownOrchestrator shutdownOrchestrator;

    /**
     * Constructor for StartComputeTerminationHandler
     * See GameLiftAgentWebSocketListener for message routing by Action
     * @param objectMapper
     * @param shutdownOrchestrator
     */
    @Inject
    public StartComputeTerminationHandler(final ObjectMapper objectMapper, final ShutdownOrchestrator shutdownOrchestrator)  {
        super(StartComputeTerminationMessage.class, objectMapper);
        this.shutdownOrchestrator = shutdownOrchestrator;
    }

    @Override
    public void handle(final StartComputeTerminationMessage message) {
        log.info("StartComputeTermination message received: {}", message);
        shutdownOrchestrator.startTermination(Instant.now().plus(ShutdownOrchestrator.DEFAULT_TERMINATION_DEADLINE), false);
    }
}
