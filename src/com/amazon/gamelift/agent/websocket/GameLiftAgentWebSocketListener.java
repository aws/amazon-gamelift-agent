/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.amazon.gamelift.agent.model.exception.MalformedRequestException;
import com.amazon.gamelift.agent.websocket.handlers.MessageHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The GameLift agent implementation of the Java 11 WebSocket.Listener.
 * This handles the logic for parsing messages over the WebSocket connection and determining how to process them.
 * It is associated with a WebSocket instance, but operates independently of it.
 *
 * See Java docs for more details:
 * https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/WebSocket.Listener.html
 */
@Slf4j
@AllArgsConstructor
public class GameLiftAgentWebSocketListener implements WebSocket.Listener {

    // openRequests is a map used specifically for processing messages in a request/response manner.
    // Requests, and responses to those requests, will have an associated ID which is used to map responses to the
    // request. When a response is found when processing messages, the associated future in this map is completed.
    private final Map<String, CompletableFuture<String>> openRequests = new HashMap<>();

    private final StringBuilder messageBuffer = new StringBuilder();
    private final Map<String, MessageHandler<?>> messageHandlers;
    private final ObjectMapper objectMapper;

    /**
     * Simple implementation of onOpen that logs the connection opening.
     * The super call is needed for incrementing an internal counter for the Java 11 Websocket implementation.
     *
     * @param webSocket - the associated websocket connection that was opened
     */
    @Override
    public void onOpen(final WebSocket webSocket) {
        log.info("GameLift agent WebSocket connection opened");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    /**
     * Simple implementation of onError that logs when errors are received on the connection.
     *
     * @param webSocket - the associated websocket connection that encountered the error
     * @param error - the exception that was encountered
     */
    @Override
    public void onError(final WebSocket webSocket, final Throwable error) {
        log.error("GameLift agent error received from Websocket connection", error);
    }

    /**
     * Simple implementation of onClose which logs and cancels/clears out any pending request futures
     *
     * @param webSocket - the associated websocket connection that encountered the error
     * @param statusCode - status code for the connection closure
     * @param reason - reason provided for the connection closure
     */
    @Override
    public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
        log.info("GameLift agent WebSocket connection closed: code={}, reason={}", statusCode, reason);
        synchronized (openRequests) {
            for (final CompletableFuture<String> openRequestFuture : openRequests.values()) {
                openRequestFuture.cancel(true);
            }
            openRequests.clear();
        }
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    /**
     * The primary way which GameLift agent receives messages over the WebSocket connection.
     *
     * Messages can be split over multiple onText calls, but will be received in-order, which is why messageBuffer is
     * used to assemble the message until the final part is received (indicated by the 'last' flag)
     *
     * Once the completed message is received, this method will process the message in one of two ways:
     *  - The request ID in the message is found within the openRequests map, in which case the message will be
     *    given to the associated Future in that map and then completed (for request/reply message processing)
     *  - The request ID is not found in openRequests, in which case the message is passed it to the associated
     *    MessageHandler to process the required logic for the message (for asynchronous message processing)
     *
     * @param webSocket - The Websocket instance over which the message was received
     * @param data - The textual message received on the Websocket. Can be partial data if last = false
     * @param last - Flag to indicate if this is the last part of the message. If false, there will be subsequent
     *               calls to onText which will contain the next parts of the message
     */
    @Override
    public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence data, final boolean last) {
        messageBuffer.append(data);

        if (last) {
            final String completedMessage = messageBuffer.toString();
            messageBuffer.setLength(0); // Reset the buffer for following messages

            try {
                final WebsocketResponse response = objectMapper.readValue(completedMessage, WebsocketResponse.class);
                final String requestId = response.getRequestId();
                final String action = response.getAction();
                boolean synchronousRequestProcessed = false;

                synchronized (openRequests) {
                    // requestId will be null if a message is received instead of a response
                    if (openRequests.containsKey(requestId)) {
                        openRequests.remove(requestId).complete(completedMessage);
                        synchronousRequestProcessed = true;
                    }
                }

                // If the function returns the message via the openRequests queue, then that means it was a response that
                // was being processed synchronously, and the function doesn't need to invoke an async handler
                if (!synchronousRequestProcessed) {
                    final MessageHandler<?> handler =
                            messageHandlers.getOrDefault(action, messageHandlers.get(WebSocketActions.Default.name()));
                    handler.handle(completedMessage);
                }
            } catch (final JsonProcessingException | MalformedRequestException e) {
                log.error("Failed to deserialize message {} into a response", completedMessage, e);
            } catch (final Exception e) {
                // Swallow and log any unknown exceptions that occur. Throwing an exception from the onText() handler
                // will cause the WebSocket connection to be closed and the onError() handler to be triggered
                log.error("Unexpected error occurred when processing WebSocket message: {}", completedMessage, e);
            }
        }

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    /**
     * Adds an expected request ID to our openRequests queue. This is used in {@link AgentWebSocket} to
     * add requests which responses are expected for when communicating in a synchronous request/response method.
     *
     * If the given requestId is found when processing WebSocket messages (in onText), then that message will be
     * considered a response to the given request, and that response message will be provided to the associated Future,
     * which will then be completed.
     *
     * NOTE: Make sure any requests enqueued are safely dequeued via removeExpectedResponse.
     *       Failure to do so will result in memory leaks.
     *
     * @param requestId - the ID of the request which the function is expecting a response for
     * @param responseFuture - the Future which will be completed once the response is received over the Websocket
     */
    void addExpectedResponse(final String requestId, final CompletableFuture<String> responseFuture) {
        synchronized (openRequests) {
            openRequests.put(requestId, responseFuture);
        }
    }

    /**
     * Dequeues the request ID from the openRequests queue. Should be utilized in a safe manner (i.e. finally blocks)
     * to ensure there's no memory leaks caused by not removing entries from the request queue.
     *
     * @param requestId - the ID of the request that the function is removing from the openRequests queue.
     */
    void removeExpectedResponse(final String requestId) {
        synchronized (openRequests) {
            openRequests.remove(requestId);
        }
    }
}
