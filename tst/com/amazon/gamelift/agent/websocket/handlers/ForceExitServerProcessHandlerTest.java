/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket.handlers;

import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.amazon.gamelift.agent.model.websocket.ForceExitServerProcessMessage;
import com.amazon.gamelift.agent.process.GameProcessManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ForceExitServerProcessHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PROCESS_ID = "ProcessId";

    @Mock
    private GameProcessManager gameProcessManager;

    private ForceExitServerProcessMessage message;
    private String messageAsString;

    private ForceExitServerProcessHandler forceExitServerProcessHandler;

    @BeforeEach
    public void setup() {
        forceExitServerProcessHandler = new ForceExitServerProcessHandler(OBJECT_MAPPER, gameProcessManager);
    }

    @Test
    public void GIVEN_terminateProcessMessage_WHEN_handle_THEN_processTerminated() throws Exception {
        //Givens
        message = new ForceExitServerProcessMessage();
        message.setProcessId(PROCESS_ID);
        message.setAction(WebSocketActions.ForceExitServerProcess.name());
        message.setTerminationReason(ProcessTerminationReason.CUSTOMER_INITIATED.toString());
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        //When
        forceExitServerProcessHandler.handle(messageAsString);

        //Then
        verify(gameProcessManager).terminateProcessByUUID(PROCESS_ID,
                ProcessTerminationReason.CUSTOMER_INITIATED);
    }

    @Test
    public void GIVEN_terminateProcessMessageWithNoTerminationReason_WHEN_handle_THEN_processTerminatedWithDefaultReason() throws Exception {
        //Givens
        message = new ForceExitServerProcessMessage();
        message.setProcessId(PROCESS_ID);
        message.setAction(WebSocketActions.ForceExitServerProcess.name());
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        //When
        forceExitServerProcessHandler.handle(messageAsString);

        //Then
        verify(gameProcessManager).terminateProcessByUUID(PROCESS_ID,
                ProcessTerminationReason.NORMAL_TERMINATION);
    }

    @Test
    public void GIVEN_invalidAction_WHEN_handle_THEN_processTerminated() throws Exception {
        //Givens
        message = new ForceExitServerProcessMessage();
        message.setProcessId(PROCESS_ID);
        message.setAction("");
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        //When
        forceExitServerProcessHandler.handle(messageAsString);

        //Then
        verify(gameProcessManager, never()).terminateProcessByUUID(any(), any());
    }

    @Test
    public void GIVEN_unknownTerminationReason_WHEN_handle_THEN_processTerminatedWithNormalReason() throws Exception {
        message = new ForceExitServerProcessMessage();
        message.setProcessId(PROCESS_ID);
        message.setAction(WebSocketActions.ForceExitServerProcess.name());
        message.setTerminationReason(RandomStringUtils.randomAlphanumeric(7));
        messageAsString = OBJECT_MAPPER.writeValueAsString(message);

        //When
        forceExitServerProcessHandler.handle(messageAsString);

        //Then
        verify(gameProcessManager).terminateProcessByUUID(PROCESS_ID,
                ProcessTerminationReason.NORMAL_TERMINATION);
    }
}

