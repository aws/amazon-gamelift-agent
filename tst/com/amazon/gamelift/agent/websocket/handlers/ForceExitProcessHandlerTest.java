/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.websocket.ForceExitProcessMessage;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Deprecated
@ExtendWith(MockitoExtension.class)
public class ForceExitProcessHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PROCESS_ID = "ProcessId";

    @Mock
    private GameProcessManager gameProcessManager;

    private ForceExitProcessMessage message;
    private String messageAsString;

    private ForceExitProcessHandler forceExitProcessHandler;

    @BeforeEach
    public void setup() {
        forceExitProcessHandler = new ForceExitProcessHandler(OBJECT_MAPPER, gameProcessManager);
    }

    @Test
    public void GIVEN_terminateProcessMessage_WHEN_handle_THEN_processTerminated() throws Exception {
        //Givens
        message = new ForceExitProcessMessage();
        message.setProcessId(PROCESS_ID);
        message.setAction(WebSocketActions.ForceExitProcess.name());
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        //When
        forceExitProcessHandler.handle(messageAsString);

        //Then
        verify(gameProcessManager).terminateProcessByUUID(PROCESS_ID,
                ProcessTerminationReason.NORMAL_TERMINATION);
    }

    @Test
    public void GIVEN_invalidAction_WHEN_handle_THEN_processTerminated() throws Exception {
        //Givens
        message = new ForceExitProcessMessage();
        message.setProcessId(PROCESS_ID);
        message.setAction("");
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        //When
        forceExitProcessHandler.handle(messageAsString);

        //Then
        verify(gameProcessManager, never()).terminateProcessByUUID(any(), any());
    }

}
