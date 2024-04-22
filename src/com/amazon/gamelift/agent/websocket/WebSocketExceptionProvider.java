/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import com.amazon.gamelift.agent.model.websocket.base.ErrorWebsocketResponse;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;

@Singleton
@RequiredArgsConstructor
public class WebSocketExceptionProvider {

    private final ObjectMapper objectMapper;

    /**
     * Attempts to deserialize a message from the WebSocket as an error response.
     * If the message has an error status code, it'll attempt to translate to a Java exception type.
     * Otherwise, it'll return null
     */
    public AgentException getExceptionFromWebSocketMessage(final String webSocketMessage)
            throws JsonProcessingException {
        final ErrorWebsocketResponse testErrorResponse =
                objectMapper.readValue(webSocketMessage, ErrorWebsocketResponse.class);

        return switch (testErrorResponse.getStatusCode()) {
            case HttpStatus.SC_OK -> null; // If message is not an error response, return null
            case HttpStatus.SC_BAD_REQUEST -> new InvalidRequestException(testErrorResponse.getErrorMessage());
            case HttpStatus.SC_UNAUTHORIZED -> new UnauthorizedException(testErrorResponse.getErrorMessage());
            case HttpStatus.SC_NOT_FOUND -> new NotFoundException(testErrorResponse.getErrorMessage());
            default -> new InternalServiceException(testErrorResponse.getErrorMessage());
        };
    }
}
