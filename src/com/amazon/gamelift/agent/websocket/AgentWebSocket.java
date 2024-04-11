/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import java.net.http.WebSocket;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketRequest;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;

/**
 * The GameLift agent wrapper class for a Java 11 WebSocket instance (and it's associated WebSocket.Listener).
 * This primarily contains logic for sending messages over the connection - see AgentWebSocketListener
 * for how messages received over the connection are handled.
 *
 * See Java Doc for more details on how to use the WebSocket instance:
 * https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/WebSocket.html
 */
@Slf4j
public class AgentWebSocket {

    private final WebSocket websocketSender;
    private final GameLiftAgentWebSocketListener websocketListener;
    private final WebSocketExceptionProvider webSocketExceptionProvider;
    private final ObjectMapper objectMapper;

    // This queue is used to store an ordered list of messages to be sent out over the websocket. Only one message may
    // be outgoing (IE actually sending text out) at once or IllegalStateException is thrown and message fails to send
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/WebSocket.html#sendText(java.lang.CharSequence,boolean
    private final Queue<WebsocketRequest> requestQueue = new LinkedList<>();

    // Boolean flag for whether a message is currently being sent out over the websocket. When this is 'true'
    // AgentWebSocket will enqueue other incoming messages to be sent in order.
    private boolean messageInFlight;

    /**
     * Constructor for GameLiftAgentWebSocket
     * @param websocketSender
     * @param websocketListener
     * @param objectMapper
     */
    @Inject
    public AgentWebSocket(final WebSocket websocketSender,
                          final GameLiftAgentWebSocketListener websocketListener,
                          final WebSocketExceptionProvider webSocketExceptionProvider,
                          final ObjectMapper objectMapper) {
        this.websocketSender = websocketSender;
        this.websocketListener = websocketListener;
        this.webSocketExceptionProvider = webSocketExceptionProvider;
        this.objectMapper = objectMapper;
        this.messageInFlight = false;
    }

    /**
     * Sends a request synchronously over the WebSocket connection.
     *
     * To do this, enqueue the associated request ID to the WebSocket listener. This ID should also be present
     * in the response, so if the response is received in the listener, the provided Future will be completed
     * and this method will return the deserialized response.
     *
     * @param request - The message to send over the connection
     * @param responseClass - The class to deserialize the response into
     * @param timeout - The amount of time the function will wait for a response
     * @throws AgentException
     */
    public <T extends WebsocketResponse> T sendRequest(
            final WebsocketRequest request,
            final Class<T> responseClass,
            final Duration timeout) throws AgentException {
        final String requestId = request.getRequestId();
        final CompletableFuture<String> responseFuture = new CompletableFuture<>();

        websocketListener.addExpectedResponse(requestId, responseFuture);

        try {
            sendRequestAsync(request);

            final String webSocketResponse = responseFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            final AgentException responseException =
                    webSocketExceptionProvider.getExceptionFromWebSocketMessage(webSocketResponse);
            if (responseException != null) {
                throw responseException;
            }

            return objectMapper.readValue(webSocketResponse, responseClass);
        } catch (final CancellationException e) {
            log.warn("Request was cancelled, this indicates the GameLift agent is shutting down", e);
            return null;
        } catch (final ExecutionException | InterruptedException e) {
            log.error("Failed to process the response for request {}", request, e);
            throw new RuntimeException(e);
        } catch (final JsonProcessingException e) {
            log.error("Failed to deserialize the response for request {}", request, e);
            throw new RuntimeException(e);
        } catch (final TimeoutException e) {
            log.error("Failed to receive a response for request {} in {}", request, timeout, e);
            throw new RuntimeException(e);
        } finally {
            // Must be done within a finally to ensure no memory leaks
            websocketListener.removeExpectedResponse(requestId);
        }
    }

    /**
     * Sends a message asynchronously over the Websocket, skipping the message if the connection is closed.
     *
     * NOTE: Currently this doesn't handle splitting over multiple messages as it is not expected to send large
     * requests over the WebSocket. If this changes, this logic must be updated to handle splitting the message.
     * NOTE: The returned CompletableFuture is not used by the caller but facilitates unit testing possible.
     *
     * @param message - Message to send over the WebSocket
     */
    public synchronized CompletableFuture<WebSocket> sendRequestAsync(final WebsocketRequest message) {
        // If a message is currently being sent out over the websocket, or if there is a backlog of pending messages,
        // any send messages will be enqueued. If `messageInFlight` is false, it is safe to send a message currently.
        // Because all methods are synchronized, a normal boolean is thread-safe for this usage.
        // The web socket connection throws an InvalidStateException if another message is sent before the prior
        // message has completed sending.
        if (!messageInFlight) {
            if (!websocketSender.isInputClosed()) {
                messageInFlight = true;
                return sendText(message);
            } else {
                log.warn("Attempting to send message over closed WebSocket connection: {}", message);
                return null;
            }
        } else {
            requestQueue.add(message);
            return null;
        }
    }

    /**
     * Sends a message out over the web socket connection. This method returns a CompletableFuture. When this future
     * completes, `thenRun` will trigger a callback to the `handleSendTextCompletion` method, which is responsible
     * for continuing to send enqueued messages or for releasing the `messageInFlight` boolean if no work remains
     * @param message
     * @return
     */
    private synchronized CompletableFuture<WebSocket> sendText(final WebsocketRequest message) {
        try {
            final CompletableFuture<WebSocket> future
                    = websocketSender.sendText(objectMapper.writeValueAsString(message), true);
            future.thenRun(this::handleSendTextCompletion);
            return future;
        } catch (final JsonProcessingException e) {
            log.error("Failed to serialize websocket message: {}", message, e);
            throw new RuntimeException(e);
        }
    }

    /**
     *  This method is a callback from a completed future. It will check for additional enqueued messages to send while
     *  holding the `messageInFlight` boolean - set to true when a message is first sent - and will release the boolean
     *  once no additional messages are enqueued.
     */
    private synchronized void handleSendTextCompletion() {
        if (!requestQueue.isEmpty()) {
            final WebsocketRequest nextMessage = requestQueue.poll();
            log.debug("Sending queued websocket message: {} - Remaining messages in queue: {}",
                    nextMessage, requestQueue.size());
            sendText(nextMessage);
        } else {
            messageInFlight = false;
        }
    }

    /**
     * Attempts to close the WebSocket connection. Waits a certain amount of time to validate the connection closes,
     * but will swallow exceptions even if the timeout is reached.
     *
     * @param timeout - Amount of time to wait for the connection to close successfully
     */
    public void closeConnection(final Duration timeout) {
        if (websocketSender.isInputClosed()) {
            log.info("Attempted to close a WebSocket connection, but the connection's input was already closed");
            return;
        }
        try {
            websocketSender.sendClose(WebSocket.NORMAL_CLOSURE, "GameLift agent requested Websocket closure")
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            log.warn("Swallowing exception encountered when closing GameLiftAgent Websocket connection", e);
        }
    }

}
