/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.websocket;

import com.amazon.gamelift.agent.module.ProcessModule;
import com.amazon.gamelift.agent.websocket.handlers.RefreshConnectionHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RefreshConnectionMessageTest {

    private static final String REFRESH_ENDPOINT = "www.refresh.com";
    private static final String AUTH_TOKEN = "auth-token-12345";

    // Expiration Timestamp will be sent from GameLift as Epoch Seconds
    private static final Instant EXPIRATION_INSTANT = Instant.ofEpochSecond(1702427749L);

    // Get ObjectMapper from the Dagger component to utilize the correct configurations
    private final ObjectMapper objectMapper = new ProcessModule().provideObjectMapper();

    @Test
    public void GIVEN_validMessageWithEpochMillis_WHEN_objectMapperDeserialize_THEN_deserializesInstant() throws Exception {
        // GIVEN
        final String serializedMessage = "{"
                + "\"Action\": \"" + RefreshConnectionHandler.ACTION + "\","
                + "\"RefreshConnectionEndpoint\": \"" + REFRESH_ENDPOINT + "\","
                + "\"AuthToken\": \"" + AUTH_TOKEN + "\","
                + "\"ExpirationTime\": \"" + EXPIRATION_INSTANT.getEpochSecond() + "\""
                + "}";

        // WHEN
        RefreshConnectionMessage deserializedMessage =
                objectMapper.readValue(serializedMessage, RefreshConnectionMessage.class);

        // THEN
        assertEquals(RefreshConnectionHandler.ACTION, deserializedMessage.getAction());
        assertEquals(REFRESH_ENDPOINT, deserializedMessage.getRefreshConnectionEndpoint());
        assertEquals(AUTH_TOKEN, deserializedMessage.getAuthToken());
        assertEquals(EXPIRATION_INSTANT, deserializedMessage.getExpirationTime());
    }
}
