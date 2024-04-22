/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.amazon.gamelift.agent.model.constants.WebSocketActions;
import com.amazon.gamelift.agent.model.exception.MalformedRequestException;
import com.amazon.gamelift.agent.websocket.handlers.MessageHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
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
class GameLiftAgentWebSocketListener implements WebSocket.Listener {

    // A status code used when the onError() handler needs to invoke the onClose() handler
    private static final int ON_ERROR_CLOSE_STATUS_CODE = -1;

    // openRequests is a map used specifically for processing messages in a request/response manner.
    // Requests, and responses to those requests, will have an associated ID which is used to map responses to the
    // request. When a response is found when processing messages, the associated future in this map is completed.
    private final Map<String, CompletableFuture<String>> openRequests = new HashMap<>();

    // An internal unique ID assigned to our WebSocket instances to differentiate them when multiple connections
    // are open simultaneously. This is generated here since the WebSocket.Listener instance is generated before the
    // WebSocket instance.
    @Getter private final String webSocketIdentifier = UUID.randomUUID().toString();

    // The WebSocketConnectionManager should be the only place where this class is initialized. Pass the instance of
    // the manager to this class so that it can call back for handling WebSocket reconnects
    private final WebSocketConnectionManager webSocketConnectionManager;

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
        log.info("GameLift Agent WebSocket connection opened: webSocketId={}", webSocketIdentifier);
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
        String errorMessage = String.format(
                "GameLiftAgent encountered an error from WebSocket connection: webSocketId=%s, message=%s",
                webSocketIdentifier, error.getMessage());
        log.error(errorMessage, error);

        // Defer to the onClose() handler so that the disconnect-handling logic remains the same across these methods.
        onClose(webSocket, ON_ERROR_CLOSE_STATUS_CODE, errorMessage);
    }

    /**
     * Simple implementation of onClose which logs and cancels/clears out any pending request futures,
     * and then calls into the connection manager to process the disconnection. If this is an unexpected WebSocket
     * disconnect, the connection manager may perform a WebSocket reconnection.
     *
     * @param webSocket - the associated websocket connection that encountered the error
     * @param statusCode - status code for the connection closure
     * @param reason - reason provided for the connection closure
     */
    @Override
    public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
        try {
            log.info("GameLiftAgent WebSocket connection closed: webSocketId={}, code={}, reason={}",
                    webSocketIdentifier, statusCode, reason);
            // Notify the connection manager that the associated WebSocket connection has closed. If the disconnected
            // connection was the currently used WebSocket connection, this will perform a WebSocket reconnect
            webSocketConnectionManager.handleWebSocketDisconnect(webSocketIdentifier);
        } catch (Exception e) {
            log.error("Unexpected exception occurred when handling WebSocket onClose event", e);
        } finally {
            // After processing the disconnect message, cancel all pending requests
            synchronized (openRequests) {
                for (final CompletableFuture<String> openRequestFuture : openRequests.values()) {
                    openRequestFuture.cancel(true);
                }
                openRequests.clear();
            }
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
                    // requestId will be null if this was a message sent from the server, rather than a response from
                    // a previously sent message
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
