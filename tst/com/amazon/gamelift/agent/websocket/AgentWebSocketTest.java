/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketRequest;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.primitives.Ints;

import lombok.NonNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
public class AgentWebSocketTest {

    private static final String TEST_REQUEST_ID = "testRequestId";
    private static final String TEST_SERIALIZED_RESPONSE = "{\"RequestId\":\"" + TEST_REQUEST_ID + "\"}";
    private static final String TEST_SERIALIZED_REQUEST = "testSerializedRequest";

    @Mock private WebSocket mockWebSocketSender;
    @Mock private GameLiftAgentWebSocketListener mockWebSocketListener;
    @Mock private WebSocketExceptionProvider mockWebSocketExceptionProvider;
    @Mock private CompletableFuture<WebSocket> mockFuture;
    @Spy private final ObjectMapper objectMapperSpy = new ObjectMapper();

    private AgentWebSocket webSocketClient;
    private final Random random = new Random();

    @BeforeEach
    public void setup() {
        webSocketClient = new AgentWebSocket(mockWebSocketSender, mockWebSocketListener,
                mockWebSocketExceptionProvider, objectMapperSpy);
    }

    @Test
    public void GIVEN_successfulResponse_WHEN_sendRequest_WHEN_returnsDeserializedResponse() throws Exception {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);
        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                invocation.getArgument(1, CompletableFuture.class).complete(TEST_SERIALIZED_RESPONSE);
                return TEST_SERIALIZED_RESPONSE;
            }
        }).when(mockWebSocketListener).addExpectedResponse(eq(TEST_REQUEST_ID), any());
        when(objectMapperSpy.writeValueAsString(testRequest)).thenReturn(TEST_SERIALIZED_REQUEST);
        when(mockWebSocketSender.sendText(any(), eq(true))).thenReturn(mockFuture);

        // WHEN
        WebsocketResponse response =
                webSocketClient.sendRequest(testRequest, WebsocketResponse.class, Duration.ofSeconds(60));

        // THEN
        assertEquals(TEST_REQUEST_ID, response.getRequestId());
        verify(mockWebSocketSender).sendText(TEST_SERIALIZED_REQUEST, true);
        verify(mockWebSocketListener).removeExpectedResponse(TEST_REQUEST_ID);
    }

    @Test
    public void GIVEN_errorResponse_WHEN_sendRequest_WHEN_returnsDeserializedException() throws Exception {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);
        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                invocation.getArgument(1, CompletableFuture.class).complete(TEST_SERIALIZED_RESPONSE);
                return TEST_SERIALIZED_RESPONSE;
            }
        }).when(mockWebSocketListener).addExpectedResponse(eq(TEST_REQUEST_ID), any());
        when(mockWebSocketExceptionProvider.getExceptionFromWebSocketMessage(TEST_SERIALIZED_RESPONSE))
                .thenReturn(new InternalServiceException());
        when(objectMapperSpy.writeValueAsString(testRequest)).thenReturn(TEST_SERIALIZED_REQUEST);
        when(mockWebSocketSender.sendText(any(), eq(true))).thenReturn(mockFuture);

        // WHEN
        assertThrows(InternalServiceException.class, () ->
                webSocketClient.sendRequest(testRequest, WebsocketResponse.class, Duration.ofSeconds(60)));

        // THEN
        verify(mockWebSocketSender).sendText(TEST_SERIALIZED_REQUEST, true);
        verify(mockWebSocketListener).removeExpectedResponse(TEST_REQUEST_ID);
    }

    @Test
    public void GIVEN_futureGetsException_WHEN_sendRequest_WHEN_throwsException() {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                invocation.getArgument(1, CompletableFuture.class).completeExceptionally(new InternalServiceException());
                return TEST_SERIALIZED_RESPONSE;
            }
        }).when(mockWebSocketListener).addExpectedResponse(eq(TEST_REQUEST_ID), any());
        when(mockWebSocketSender.sendText(any(), eq(true))).thenReturn(mockFuture);

        // WHEN
        final RuntimeException e = assertThrows(RuntimeException.class, () ->
                webSocketClient.sendRequest(testRequest, WebsocketResponse.class, Duration.ofSeconds(60)));

        // THEN
        assertInstanceOf(ExecutionException.class, e.getCause(), String.format("Expected exception cause to be ExecutionException, "
                + "but actually was: %s", e.getCause()));
        verify(mockWebSocketListener).removeExpectedResponse(TEST_REQUEST_ID);
    }

    @Test
    public void GIVEN_deserializationFails_WHEN_sendRequest_WHEN_throwsException() throws Exception {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                invocation.getArgument(1, CompletableFuture.class).complete(TEST_SERIALIZED_RESPONSE);
                return TEST_SERIALIZED_RESPONSE;
            }
        }).when(mockWebSocketListener).addExpectedResponse(eq(TEST_REQUEST_ID), any());
        when(mockWebSocketExceptionProvider.getExceptionFromWebSocketMessage(TEST_SERIALIZED_RESPONSE)).thenReturn(null);
        when(objectMapperSpy.writeValueAsString(any())).thenReturn(TEST_SERIALIZED_REQUEST);
        when(objectMapperSpy.readValue(TEST_SERIALIZED_RESPONSE, WebsocketResponse.class))
                .thenThrow(JsonProcessingException.class);
        when(mockWebSocketSender.sendText(any(), eq(true))).thenReturn(mockFuture);

        // WHEN
        final RuntimeException e = assertThrows(RuntimeException.class, () ->
                webSocketClient.sendRequest(testRequest, WebsocketResponse.class, Duration.ofSeconds(60)));

        // THEN
        assertInstanceOf(JsonProcessingException.class, e.getCause(), String.format("Expected exception cause to be JsonProcessingException, "
                + "but actually was: %s", e.getCause()));
        verify(mockWebSocketListener).removeExpectedResponse(TEST_REQUEST_ID);
    }

    @Test
    public void GIVEN_requestCancelled_WHEN_sendRequest_WHEN_returnsNull() throws AgentException {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);
        when(mockWebSocketSender.sendText(any(), eq(true))).thenReturn(mockFuture);

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                invocation.getArgument(1, CompletableFuture.class).cancel(true);
                return TEST_SERIALIZED_RESPONSE;
            }
        }).when(mockWebSocketListener).addExpectedResponse(eq(TEST_REQUEST_ID), any());

        // WHEN
        final WebsocketResponse response = webSocketClient.sendRequest(testRequest, WebsocketResponse.class, Duration.ofSeconds(60));

        // THEN
        assertNull(response);
        verify(mockWebSocketListener).removeExpectedResponse(TEST_REQUEST_ID);
    }

    @Test
    public void GIVEN_requestTimesOut_WHEN_sendRequest_WHEN_throwsException() {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);
        when(mockWebSocketSender.sendText(any(), eq(true))).thenReturn(mockFuture);

        // WHEN
        final RuntimeException e = assertThrows(RuntimeException.class, () ->
                webSocketClient.sendRequest(testRequest, WebsocketResponse.class, Duration.ofMillis(1)));

        // THEN
        assertInstanceOf(TimeoutException.class, e.getCause());
        verify(mockWebSocketListener).removeExpectedResponse(TEST_REQUEST_ID);
    }

    @Test
    public void GIVEN_noExceptions_WHEN_sendRequestAsync_THEN_sendsOverWebsocket() throws Exception {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);
        when(mockWebSocketSender.sendText(any(), eq(true))).thenReturn(mockFuture);

        when(objectMapperSpy.writeValueAsString(testRequest)).thenReturn(TEST_SERIALIZED_REQUEST);

        // WHEN
        webSocketClient.sendRequestAsync(testRequest);

        // THEN
        verify(mockWebSocketSender).sendText(TEST_SERIALIZED_REQUEST, true);
    }

    @Test
    public void GIVEN_lotsOfMessages_WHEN_sendRequestAsync_THEN_allFuturesComplete() throws Exception {
        // GIVEN
        final int messageCount = 1000;
        final CountDownLatch doneSignal = new CountDownLatch(messageCount);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        // This DelayQueue is used to mock time passing until CompletableFutures complete - see more detail below
        BlockingQueue<DelayFuture> dq = new DelayQueue();
        when(objectMapperSpy.writeValueAsString(any())).thenReturn(TEST_SERIALIZED_REQUEST);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                // This delay configures when the CompletableFuture is available for polling from the DelayQueue.
                // Mocking an amount of time passing before a future completes in this manner.
                // Polling a value from the DelayQueue completes the future.
                final long delay = random.nextInt(6);
                final CompletableFuture<WebSocket> future = new CompletableFuture<>();
                dq.add(new DelayFuture(future, delay));
                return future;
            }
        }).when(mockWebSocketSender).sendText(any(), eq(true));

        // The DelayQueue is full of futures from sendText calls, waiting for completion.
        // Polling the queue fetches them as their delays become available to complete the future.
        final AtomicInteger counter = new AtomicInteger(0);
        executorService.execute(() -> {
            final Instant timeout = Instant.now().plus(Duration.ofSeconds(15));
            do {
                DelayFuture future = null;
                try {
                    future = dq.poll(250, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
                if (future != null) {
                    future.future.complete(mockWebSocketSender);
                    doneSignal.countDown();
                    counter.getAndIncrement();
                }
            } while (Instant.now().isBefore(timeout) && counter.intValue() < messageCount);
        });

        // WHEN
        for (int i = 0; i < messageCount; i++) {
            WebsocketRequest testRequest = new WebsocketRequest();
            final String requestId = TEST_REQUEST_ID + "-" + RandomStringUtils.randomAlphanumeric(5);
            testRequest.setRequestId(requestId);
            webSocketClient.sendRequestAsync(testRequest);
            // Slightly spreading the sending of messages to emulate realistic traffic
            try {
                final long delay = random.nextInt(3);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // Do Nothing
            }
        }

        // Wait for the CountDownLatch to confirm mocked completion of messageCount futures, decremented when the
        // DelayQueue is polled and sendText futures are "completed." This triggers the callback to either send the
        // next queued message or release the messageInFlight boolean if no messages are pending.
        try {
            doneSignal.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Do Nothing
        }

        /**
         *  To verify:
         *  1. `sendText` is called 1000 times - Indicates all messages eventually sent (many should've been queued)
         *  2. Futures placed in the DelayQueue are completed 1000 times - Indicates all `sendText` calls completed
         *  3. The DelayQueue is empty
         */
        // THEN
        verify(mockWebSocketSender, times(messageCount)).sendText(any(), eq(true));
        assertEquals(messageCount, counter.get());
        assertTrue(dq.isEmpty());
    }

    @Test
    public void GIVEN_inputClosed_WHEN_sendRequestAsync_THEN_doesNotSendMessage() {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);
        when(mockWebSocketSender.isInputClosed()).thenReturn(true);

        // WHEN
        webSocketClient.sendRequestAsync(testRequest);

        // THEN
        verifyNoMoreInteractions(mockWebSocketSender);
    }

    @Test
    public void GIVEN_malformedInput_WHEN_sendRequestAsync_THEN_throwsRuntimeException() throws Exception {
        // GIVEN
        WebsocketRequest testRequest = new WebsocketRequest();
        testRequest.setRequestId(TEST_REQUEST_ID);
        doThrow(JsonProcessingException.class).when(objectMapperSpy).writeValueAsString(testRequest);

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> webSocketClient.sendRequestAsync(testRequest));
    }

    @Test
    public void GIVEN_connectionOpen_WHEN_closeConnection_THEN_closesConnection() {
        // GIVEN
        when(mockWebSocketSender.isInputClosed()).thenReturn(false);
        when(mockWebSocketSender.sendClose(anyInt(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        // WHEN
        webSocketClient.closeConnection(Duration.ofSeconds(1));

        // THEN
        verify(mockWebSocketSender).sendClose(eq(WebSocket.NORMAL_CLOSURE), anyString());
    }

    @Test
    public void GIVEN_connectionClosed_WHEN_closeConnection_THEN_doesNothing() {
        // GIVEN
        when(mockWebSocketSender.isInputClosed()).thenReturn(true);

        // WHEN
        webSocketClient.closeConnection(Duration.ofSeconds(1));

        // THEN
        verifyNoMoreInteractions(mockWebSocketSender);
    }

    @Test
    public void GIVEN_connectionOpen_WHEN_closeConnection_THEN_swallowsException() {
        // GIVEN
        when(mockWebSocketSender.isInputClosed()).thenReturn(false);
        when(mockWebSocketSender.sendClose(anyInt(), anyString())).thenThrow(RuntimeException.class);

        // WHEN
        webSocketClient.closeConnection(Duration.ofSeconds(1));

        // THEN - swallows exception;
    }

    public static class DelayFuture implements Delayed {
        private final CompletableFuture<WebSocket> future;
        private final long startTime;

        public DelayFuture(CompletableFuture<WebSocket> future, long delayInMilliseconds) {
            this.future = future;
            this.startTime = System.currentTimeMillis() + delayInMilliseconds;
        }

        @Override
        public long getDelay(@NonNull TimeUnit unit) {
            long diff = startTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NonNull Delayed o) {
            return Ints.saturatedCast(
                    this.startTime - ((DelayFuture) o).startTime);
        }
    }
}
