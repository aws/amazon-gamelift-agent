/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.websocket.NotifyProcessRegisteredMessage;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class NotifyProcessRegisteredHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TEST_PROCESS_ID = "TEST_PROCESS_ID";
    private static final List<String> TEST_LOG_PATHS = ImmutableList.of("/game/logs/", "/game/logs/1234");

    private NotifyProcessRegisteredMessage message;
    private String messageAsString;

    @Mock
    private GameProcessManager mockGameProcessManager;

    @Captor
    private ArgumentCaptor<List<String>> logPathsArgumentCaptor;

    private NotifyProcessRegisteredHandler processRegisteredHandler;

    @BeforeEach
    public void setup() {
        processRegisteredHandler = new NotifyProcessRegisteredHandler(OBJECT_MAPPER, mockGameProcessManager);
    }

    @Test
    public void GIVEN_processRegisteredMessage_WHEN_handle_THEN_logPathsSaved() throws Exception {
        // GIVEN
        message = new NotifyProcessRegisteredMessage();
        message.setProcessId(TEST_PROCESS_ID);
        message.setLogPaths(TEST_LOG_PATHS);
        message.setAction(WebSocketActions.NotifyProcessRegistered.name());
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        // WHEN
        processRegisteredHandler.handle(messageAsString);

        // THEN
        verify(mockGameProcessManager).updateProcessOnRegistration(eq(TEST_PROCESS_ID), logPathsArgumentCaptor.capture());
        final List<String> capturedLogPaths = logPathsArgumentCaptor.getValue();
        assertEquals(TEST_LOG_PATHS, capturedLogPaths);
    }

    @Test
    public void GIVEN_notFoundException_WHEN_handle_THEN_exceptionSwallowed() throws Exception {
        // GIVEN
        message = new NotifyProcessRegisteredMessage();
        message.setProcessId(TEST_PROCESS_ID);
        message.setLogPaths(TEST_LOG_PATHS);
        message.setAction(WebSocketActions.NotifyProcessRegistered.name());
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);
        Mockito.doThrow(NotFoundException.class).when(mockGameProcessManager).updateProcessOnRegistration(any(), any());

        // WHEN
        processRegisteredHandler.handle(messageAsString);

        // THEN
        verify(mockGameProcessManager).updateProcessOnRegistration(eq(TEST_PROCESS_ID), logPathsArgumentCaptor.capture());
        final List<String> capturedLogPaths = logPathsArgumentCaptor.getValue();
        assertEquals(TEST_LOG_PATHS, capturedLogPaths);
    }

    @Test
    public void GIVEN_processRegisteredMessageWithoutLogPaths_WHEN_handle_THEN_saveLogPathsNotInvoked() throws Exception {
        // GIVEN
        message = new NotifyProcessRegisteredMessage();
        message.setProcessId(TEST_PROCESS_ID);
        message.setLogPaths(null);
        message.setAction(WebSocketActions.NotifyProcessRegistered.name());
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        // WHEN
        processRegisteredHandler.handle(messageAsString);

        // THEN
        verifyNoInteractions(mockGameProcessManager);
    }
}
