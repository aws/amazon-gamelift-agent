/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.websocket;

import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import com.amazon.gamelift.agent.model.websocket.base.ErrorWebsocketResponse;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WebSocketExceptionProviderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private WebSocketExceptionProvider provider;

    @BeforeEach
    public void setup() {
        provider = new WebSocketExceptionProvider(OBJECT_MAPPER);
    }

    @Test
    public void GIVEN_successResponse_WHEN_getExceptionFromWebSocketMessage_THEN_returnNull()
            throws JsonProcessingException {
        // GIVEN
        WebsocketResponse response = new WebsocketResponse();
        response.setAction(RandomStringUtils.randomAlphanumeric(10));
        response.setRequestId(RandomStringUtils.randomAlphanumeric(10));
        response.setStatusCode(HttpStatus.SC_OK);

        String serializedResponse = OBJECT_MAPPER.writeValueAsString(response);

        // WHEN / THEN
        assertNull(provider.getExceptionFromWebSocketMessage(serializedResponse));
    }

    @Test
    public void GIVEN_invalidRequestError_WHEN_getExceptionFromWebSocketMessage_THEN_returnsCorrectException()
            throws JsonProcessingException {
        testErrorType(HttpStatus.SC_BAD_REQUEST, InvalidRequestException.class);
    }

    @Test
    public void GIVEN_unauthorizedError_WHEN_getExceptionFromWebSocketMessage_THEN_returnsCorrectException()
            throws JsonProcessingException {
        testErrorType(HttpStatus.SC_UNAUTHORIZED, UnauthorizedException.class);
    }

    @Test
    public void GIVEN_notFoundError_WHEN_getExceptionFromWebSocketMessage_THEN_returnsCorrectException()
            throws JsonProcessingException {
        testErrorType(HttpStatus.SC_NOT_FOUND, NotFoundException.class);
    }

    @Test
    public void GIVEN_internalServiceError_WHEN_getExceptionFromWebSocketMessage_THEN_returnsCorrectException()
            throws JsonProcessingException {
        testErrorType(HttpStatus.SC_INTERNAL_SERVER_ERROR, InternalServiceException.class);
    }

    @Test
    public void GIVEN_unknownError_WHEN_getExceptionFromWebSocketMessage_THEN_returnsCorrectException()
            throws JsonProcessingException {
        testErrorType(HttpStatus.SC_GONE, InternalServiceException.class);
    }

    private void testErrorType(final int statusCode, final Class<?> exceptionClass) throws JsonProcessingException {
        // GIVEN
        ErrorWebsocketResponse response = new ErrorWebsocketResponse();
        response.setAction(RandomStringUtils.randomAlphanumeric(10));
        response.setRequestId(RandomStringUtils.randomAlphanumeric(10));
        response.setErrorMessage(RandomStringUtils.randomAlphanumeric(10));
        response.setStatusCode(statusCode);

        String serializedResponse = OBJECT_MAPPER.writeValueAsString(response);

        // WHEN
        Exception exception = provider.getExceptionFromWebSocketMessage(serializedResponse);

        // THEN
        assertTrue(exceptionClass.isInstance(exception),
                String.format("Exception returned was not a %s: %s", exceptionClass.getSimpleName(), exception));
        assertEquals(response.getErrorMessage(), exception.getMessage());
    }
}
