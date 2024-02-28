/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.websocket.RefreshConnectionMessage;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Lazy;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;

@Slf4j
public class RefreshConnectionHandler extends MessageHandler<RefreshConnectionMessage> {

    // Why Lazy? WebSocketConnectionManager depends on Map<String, MessageHandler<?>>, and
    // Map<String, MessageHandler<?>> depends on this class. To get around this, Dagger allows
    // Lazy initialization of a field, so it works, as long as it is not accessed in the constructor.
    private final Lazy<WebSocketConnectionManager> connectionManager;

    /**
     * Constructor for RefreshConnectionHandler
     * See GameLiftAgentWebSocketListener for message routing by Action
     * @param connectionManager
     * @param objectMapper
     */
    @Inject
    public RefreshConnectionHandler(final Lazy<WebSocketConnectionManager> connectionManager,
                                    final ObjectMapper objectMapper) {
        super(RefreshConnectionMessage.class, objectMapper);
        this.connectionManager = connectionManager;
    }

    @Override
    public void handle(RefreshConnectionMessage message) {
        try {
            connectionManager.get().reconnect(message);
        } catch (final Exception e) {
            log.error("Failed to process refresh connection message. Will retry on the next heartbeat", e);
        }
    }
}
