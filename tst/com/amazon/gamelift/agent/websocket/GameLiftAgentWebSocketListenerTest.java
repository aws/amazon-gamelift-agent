/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.amazon.gamelift.agent.model.exception.MalformedRequestException;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.amazon.gamelift.agent.websocket.handlers.MessageHandler;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GameLiftAgentWebSocketListenerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String TEST_REQUEST_ID = "testRequestId";
    private static final String TEST_PROCESS_ID = "testProcessId";

    @Mock private WebSocket mockWebSocket;
    @Mock private MessageHandler<WebsocketResponse> mockHandler;
    @Mock private MessageHandler<WebsocketResponse> mockDefaultHandler;
    @Mock private CompletableFuture<String> mockResponseFuture;

    private GameLiftAgentWebSocketListener testListener;

    @BeforeEach
    public void setup() {
        Map<String, MessageHandler<?>> mockHandlers = ImmutableMap.of(
                WebSocketActions.ForceExitProcess.name(), mockHandler,
                WebSocketActions.Default.name(), mockDefaultHandler);
        testListener = new GameLiftAgentWebSocketListener(mockHandlers, OBJECT_MAPPER);
    }

    @Test
    public void GIVEN_validInput_WHEN_onOpen_THEN_doesNothingAndReturns() {
        // GIVEN

        // WHEN
        testListener.onOpen(mockWebSocket);

        // THEN - does not throw exception
    }

    @Test
    public void GIVEN_validInput_WHEN_onError_THEN_doesNothingAndReturns() {
        // GIVEN

        // WHEN
        testListener.onError(mockWebSocket, new RuntimeException("onError Test"));

        // THEN - does not throw exception
    }

    @Test
    public void GIVEN_openRequests_WHEN_onClose_THEN_cancelsAllFutures() {
        // GIVEN
        testListener.addExpectedResponse(TEST_REQUEST_ID, mockResponseFuture);

        // WHEN
        testListener.onClose(mockWebSocket, 200, "Test onClose");

        // THEN
        verify(mockResponseFuture).cancel(true);
    }

    @Test
    public void GIVEN_validTextAndLastMessage_WHEN_onText_THEN_passesMessageToHandler() throws Exception {
        // GIVEN
        final String message =
                "{\"Action\":\"" + WebSocketActions.ForceExitProcess.name() +
                "\",\"RequestId\":\"" + TEST_REQUEST_ID +
                "\",\"ProcessId\":\"" + TEST_PROCESS_ID + "\"}";

        // WHEN
        testListener.onText(mockWebSocket, message, true);

        // THEN
        verify(mockHandler).handle(message);
        verifyNoInteractions(mockResponseFuture, mockDefaultHandler);
    }

    @Test
    public void GIVEN_validTextOverMultipleMessages_WHEN_onText_THEN_passesCompleteMessageToHandler() throws Exception {
        // GIVEN
        final String firstMessage =
                "{\"Action\":\"" + WebSocketActions.ForceExitProcess.name() + "\",\"RequestId\":\"";
        final String secondMessage =
                TEST_REQUEST_ID + "\",\"ProcessId\":\"" + TEST_PROCESS_ID + "\"}";

        // WHEN
        testListener.onText(mockWebSocket, firstMessage, false);
        testListener.onText(mockWebSocket, secondMessage, true);

        // THEN
        verify(mockHandler).handle(firstMessage + secondMessage);
        verifyNoInteractions(mockResponseFuture, mockDefaultHandler);
    }

    @Test
    public void GIVEN_validTextWithOpenRequest_WHEN_onText_THEN_passesMessageToFuture() {
        // GIVEN
        final String message =
                "{\"Action\":\"" + WebSocketActions.ForceExitProcess.name() +
                "\",\"RequestId\":\"" + TEST_REQUEST_ID +
                "\",\"ProcessId\":\"" + TEST_PROCESS_ID + "\"}";
        testListener.addExpectedResponse(TEST_REQUEST_ID, mockResponseFuture);

        // WHEN
        testListener.onText(mockWebSocket, message, true);

        // THEN
        verify(mockResponseFuture).complete(message);
        verifyNoInteractions(mockDefaultHandler, mockHandler);
    }

    @Test
    public void GIVEN_requestAddedAndRemoved_WHEN_onText_THEN_passesMessageToHandler() throws Exception {
        // GIVEN
        final String message =
                "{\"Action\":\"" + WebSocketActions.ForceExitProcess.name() +
                "\",\"RequestId\":\"" + TEST_REQUEST_ID +
                "\",\"ProcessId\":\"" + TEST_PROCESS_ID + "\"}";
        testListener.addExpectedResponse(TEST_REQUEST_ID, mockResponseFuture);
        testListener.removeExpectedResponse(TEST_REQUEST_ID);

        // WHEN
        testListener.onText(mockWebSocket, message, true);

        // THEN
        verify(mockHandler).handle(message);
        verifyNoInteractions(mockResponseFuture, mockDefaultHandler);
    }

    @Test
    public void GIVEN_actionNotFound_WHEN_onText_THEN_passesToDefaultHandler() throws Exception {
        // GIVEN
        final String message =
                "{\"Action\":\"" + "RANDOM_UNKNOWN_ACTION" +
                "\",\"RequestId\":\"" + TEST_REQUEST_ID +
                "\",\"ProcessId\":\"" + TEST_PROCESS_ID + "\"}";

        // WHEN
        testListener.onText(mockWebSocket, message, true);

        // THEN
        verify(mockDefaultHandler).handle(message);
        verifyNoInteractions(mockResponseFuture, mockHandler);
    }

    @Test
    public void GIVEN_malformedInput_WHEN_onText_THEN_swallowsException() throws Exception {
        // GIVEN
        final String message =
                "{\"Action\":\"" + WebSocketActions.ForceExitProcess.name() +
                "\",\"RequestId\":\"" + TEST_REQUEST_ID +
                "\",\"ProcessId\":\"" + TEST_PROCESS_ID + "\"}";
        Mockito.doThrow(MalformedRequestException.class).when(mockHandler).handle(anyString());

        // WHEN
        testListener.onText(mockWebSocket, message, true);

        // THEN
        verify(mockHandler).handle(message);
        verifyNoInteractions(mockResponseFuture, mockDefaultHandler);
    }

    @Test
    public void GIVEN_unknownExceptionFromFuture_WHEN_onText_THEN_swallowsException() {
        // GIVEN
        final String message =
                "{\"Action\":\"" + WebSocketActions.ForceExitProcess.name() +
                        "\",\"RequestId\":\"" + TEST_REQUEST_ID +
                        "\",\"ProcessId\":\"" + TEST_PROCESS_ID + "\"}";
        testListener.addExpectedResponse(TEST_REQUEST_ID, mockResponseFuture);
        when(mockResponseFuture.complete(any())).thenThrow(RuntimeException.class);

        // WHEN
        testListener.onText(mockWebSocket, message, true);

        // THEN
        verify(mockResponseFuture).complete(message);
        verifyNoInteractions(mockDefaultHandler, mockHandler);
    }
}
