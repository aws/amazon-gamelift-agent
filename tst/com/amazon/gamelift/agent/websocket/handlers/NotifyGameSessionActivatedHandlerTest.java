/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.websocket.NotifyGameSessionActivatedMessage;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NotifyGameSessionActivatedHandlerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TEST_PROCESS_ID = "TEST_PROCESS_ID";
    private static final String TEST_GAME_SESSION_ID = "TEST_GAME_SESSION_ID";

    private NotifyGameSessionActivatedMessage message;
    private String messageAsString;

    @Mock private GameProcessManager mockGameProcessManager;

    private NotifyGameSessionActivatedHandler notifyGameSessionActivatedHandler;

    @BeforeEach
    public void setup() {
        notifyGameSessionActivatedHandler = new NotifyGameSessionActivatedHandler(OBJECT_MAPPER, mockGameProcessManager);
    }

    @Test
    public void GIVEN_processRegisteredMessage_WHEN_handle_THEN_logPathsSaved() throws Exception {
        // GIVEN
        message = new NotifyGameSessionActivatedMessage();
        message.setProcessId(TEST_PROCESS_ID);
        message.setGameSessionId(TEST_GAME_SESSION_ID);
        message.setAction(WebSocketActions.NotifyGameSessionActivated.name());
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        // WHEN
        notifyGameSessionActivatedHandler.handle(messageAsString);

        // THEN
        verify(mockGameProcessManager)
                .updateProcessOnGameSessionActivation(eq(TEST_PROCESS_ID), eq(TEST_GAME_SESSION_ID));
    }

    @Test
    public void GIVEN_notFoundException_WHEN_handle_THEN_exceptionSwallowed() throws Exception {
        // GIVEN
        message = new NotifyGameSessionActivatedMessage();
        message.setProcessId(TEST_PROCESS_ID);
        message.setGameSessionId(TEST_GAME_SESSION_ID);
        message.setAction(WebSocketActions.NotifyProcessRegistered.name());
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        Mockito.doThrow(NotFoundException.class)
                .when(mockGameProcessManager).updateProcessOnGameSessionActivation(any(), any());

        // WHEN
        notifyGameSessionActivatedHandler.handle(messageAsString);

        // THEN
        verify(mockGameProcessManager)
                .updateProcessOnGameSessionActivation(eq(TEST_PROCESS_ID), eq(TEST_GAME_SESSION_ID));
    }
}
