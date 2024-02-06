/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.client;

import com.amazon.gamelift.agent.model.gamelift.GetComputeAuthTokenResponse;
import com.amazon.gamelift.agent.model.gamelift.RegisterComputeResponse;
import com.amazon.gamelift.agent.model.exception.ConflictException;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import com.amazonaws.services.gamelift.AmazonGameLift;
import com.amazonaws.services.gamelift.model.Compute;
import com.amazonaws.services.gamelift.model.DeregisterComputeRequest;
import com.amazonaws.services.gamelift.model.DeregisterComputeResult;
import com.amazonaws.services.gamelift.model.GetComputeAuthTokenRequest;
import com.amazonaws.services.gamelift.model.GetComputeAuthTokenResult;
import com.amazonaws.services.gamelift.model.RegisterComputeRequest;
import com.amazonaws.services.gamelift.model.RegisterComputeResult;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AmazonGameLiftClientWrapperTest {

    private static final String COMPUTE_NAME = "compute-name";
    private static final String FLEET_ID = "fleet-id";
    private static final String LOCATION = "us-west-2";
    private static final Instant NOW = Instant.now();
    private static final String AUTH_TOKEN = RandomStringUtils.randomAlphanumeric(12);
    private static final String SDK_ENDPOINT = RandomStringUtils.randomAlphanumeric(12);
    private static final String COMPUTE_STATUS = RandomStringUtils.randomAlphanumeric(12);
    private static final Date COMPUTE_CREATION_TIME = new Date();

    private static final GetComputeAuthTokenRequest AUTH_TOKEN_REQUEST = new GetComputeAuthTokenRequest()
            .withComputeName(COMPUTE_NAME)
            .withFleetId(FLEET_ID);

    private static final RegisterComputeRequest REGISTER_COMPUTE_REQUEST = new RegisterComputeRequest()
            .withComputeName(COMPUTE_NAME)
            .withFleetId(FLEET_ID)
            .withIpAddress("0.0.0.0");

    private static final DeregisterComputeRequest DEREGISTER_COMPUTE_REQUEST = new DeregisterComputeRequest()
            .withComputeName(COMPUTE_NAME)
            .withFleetId(FLEET_ID);

    @Mock private AmazonGameLift mockAmazonGameLift;
    @Mock private GetComputeAuthTokenResult mockAuthResult;
    @Mock private RegisterComputeResult mockRegisterResult;
    @Mock private DeregisterComputeResult mockDeregisterComputeResult;
    @Mock private Compute mockCompute;

    @Captor
    private ArgumentCaptor<GetComputeAuthTokenRequest> authCaptor;
    @Captor
    private ArgumentCaptor<RegisterComputeRequest> registerCaptor;

    private AmazonGameLiftClientWrapper amazonGameLiftClientWrapper;

    @BeforeEach
    public void setup() {
        amazonGameLiftClientWrapper = new AmazonGameLiftClientWrapper(mockAmazonGameLift);
    }

    @Test
    public void GIVEN_request_WHEN_getComputeAuthToken_THEN_authReturned() throws Exception {
        when(mockAmazonGameLift.getComputeAuthToken(any())).thenReturn(mockAuthResult);
        when(mockAuthResult.getComputeName()).thenReturn(COMPUTE_NAME);
        when(mockAuthResult.getFleetId()).thenReturn(FLEET_ID);
        when(mockAuthResult.getAuthToken()).thenReturn(AUTH_TOKEN);
        when(mockAuthResult.getExpirationTimestamp()).thenReturn(Date.from(NOW));

        GetComputeAuthTokenResponse result = amazonGameLiftClientWrapper.getComputeAuthToken(AUTH_TOKEN_REQUEST);
        verify(mockAmazonGameLift).getComputeAuthToken(authCaptor.capture());

        assertEquals(AUTH_TOKEN, result.getAuthToken());
        assertEquals(COMPUTE_NAME, result.getComputeName());
        assertEquals(FLEET_ID, result.getFleetId());

        GetComputeAuthTokenRequest capturedRequest = authCaptor.getValue();
        assertEquals(COMPUTE_NAME, capturedRequest.getComputeName());
        assertEquals(FLEET_ID, capturedRequest.getFleetId());
    }

    @Test
    public void GIVEN_unauthorizedException_WHEN_getComputeAuthToken_THEN_unauthorizedException() {
        when(mockAmazonGameLift.getComputeAuthToken(any()))
                .thenThrow(new com.amazonaws.services.gamelift.model.UnauthorizedException("unit-test"));

        // WHEN
        assertThrows(UnauthorizedException.class, () -> amazonGameLiftClientWrapper.getComputeAuthToken(AUTH_TOKEN_REQUEST));
    }

    @Test
    public void GIVEN_invalidRequestException_WHEN_getComputeAuthToken_THEN_invalidRequestException() {
        when(mockAmazonGameLift.getComputeAuthToken(any()))
                .thenThrow(new com.amazonaws.services.gamelift.model.InvalidRequestException("unit-test"));

        // WHEN
        assertThrows(InvalidRequestException.class, () -> amazonGameLiftClientWrapper.getComputeAuthToken(AUTH_TOKEN_REQUEST));
    }

    @Test
    public void GIVEN_notFoundException_WHEN_getComputeAuthToken_THEN_notFoundException() {
        when(mockAmazonGameLift.getComputeAuthToken(any()))
                .thenThrow(new com.amazonaws.services.gamelift.model.NotFoundException("unit-test"));

        // WHEN
        assertThrows(NotFoundException.class, () -> amazonGameLiftClientWrapper.getComputeAuthToken(AUTH_TOKEN_REQUEST));
    }

    @Test
    public void GIVEN_internalServiceException_WHEN_getComputeAuthToken_THEN_internalServiceException() {
        when(mockAmazonGameLift.getComputeAuthToken(any()))
                .thenThrow(new com.amazonaws.services.gamelift.model.InternalServiceException("unit-test"));

        // WHEN
        assertThrows(InternalServiceException.class, () -> amazonGameLiftClientWrapper.getComputeAuthToken(AUTH_TOKEN_REQUEST));
    }

    @Test
    public void GIVEN_request_WHEN_registerCompute_THEN_registered() throws Exception {
        when(mockAmazonGameLift.registerCompute(any())).thenReturn(mockRegisterResult);
        when(mockRegisterResult.getCompute()).thenReturn(mockCompute);
        when(mockCompute.getFleetId()).thenReturn(FLEET_ID);
        when(mockCompute.getComputeName()).thenReturn(COMPUTE_NAME);
        when(mockCompute.getGameLiftServiceSdkEndpoint()).thenReturn(SDK_ENDPOINT);
        when(mockCompute.getLocation()).thenReturn(LOCATION);
        when(mockCompute.getComputeStatus()).thenReturn(COMPUTE_STATUS);
        when(mockCompute.getCreationTime()).thenReturn(COMPUTE_CREATION_TIME);

        RegisterComputeResponse response = amazonGameLiftClientWrapper.registerCompute(REGISTER_COMPUTE_REQUEST);
        verify(mockAmazonGameLift).registerCompute(registerCaptor.capture());

        RegisterComputeRequest capturedRequest = registerCaptor.getValue();
        assertEquals(COMPUTE_NAME, capturedRequest.getComputeName());
        assertEquals(FLEET_ID, capturedRequest.getFleetId());
        assertEquals(REGISTER_COMPUTE_REQUEST.getIpAddress(), capturedRequest.getIpAddress());

        assertEquals(FLEET_ID, response.getFleetId());
        assertEquals(COMPUTE_NAME, response.getComputeName());
        assertEquals(SDK_ENDPOINT, response.getSdkWebsocketEndpoint());
        assertEquals(LOCATION, response.getLocation());
        assertEquals(COMPUTE_STATUS, response.getStatus());
        assertEquals(COMPUTE_CREATION_TIME, response.getCreationTime());
    }

    @Test
    public void GIVEN_unauthorizedException_WHEN_registerCompute_THEN_unauthorizedException() {
        when(mockAmazonGameLift.registerCompute(any()))
                .thenThrow(new com.amazonaws.services.gamelift.model.UnauthorizedException("unit-test"));

        // WHEN
        assertThrows(UnauthorizedException.class, () -> amazonGameLiftClientWrapper.registerCompute(REGISTER_COMPUTE_REQUEST));
    }

    @Test
    public void GIVEN_invalidRequestException_WHEN_registerCompute_THEN_invalidRequestException() {
        when(mockAmazonGameLift.registerCompute(any()))
                .thenThrow(new com.amazonaws.services.gamelift.model.InvalidRequestException("unit-test"));

        // WHEN
        assertThrows(InvalidRequestException.class, () -> amazonGameLiftClientWrapper.registerCompute(REGISTER_COMPUTE_REQUEST));
    }

    @Test
    public void GIVEN_conflictException_WHEN_registerCompute_THEN_conflictException() {
        when(mockAmazonGameLift.registerCompute(any()))
                .thenThrow(new com.amazonaws.services.gamelift.model.ConflictException("unit-test"));

        // WHEN
        assertThrows(ConflictException.class, () -> amazonGameLiftClientWrapper.registerCompute(REGISTER_COMPUTE_REQUEST));
    }

    @Test
    public void GIVEN_internalServiceException_WHEN_registerCompute_THEN_internalServiceException() {
        when(mockAmazonGameLift.registerCompute(any()))
                .thenThrow(new com.amazonaws.services.gamelift.model.InternalServiceException("unit-test"));

        // WHEN
        assertThrows(InternalServiceException.class, () -> amazonGameLiftClientWrapper.registerCompute(REGISTER_COMPUTE_REQUEST));
    }

    @Test
    public void GIVEN_request_WHEN_deregisterCompute_THEN_deregistered() throws Exception {
        //GIVEN
        when(mockAmazonGameLift.deregisterCompute(DEREGISTER_COMPUTE_REQUEST)).thenReturn(mockDeregisterComputeResult);

        //WHEN
        DeregisterComputeResult result = amazonGameLiftClientWrapper.deregisterCompute(DEREGISTER_COMPUTE_REQUEST);

        //THEN
        verify(mockAmazonGameLift).deregisterCompute(DEREGISTER_COMPUTE_REQUEST);
        assertEquals(result, mockDeregisterComputeResult);
    }

    @Test
    public void GIVEN_UnauthorizedException_WHEN_deregisterCompute_THEN_UnauthorizedException() {
        //GIVEN
        when(mockAmazonGameLift.deregisterCompute(DEREGISTER_COMPUTE_REQUEST))
                .thenThrow(com.amazonaws.services.gamelift.model.UnauthorizedException.class);

        //WHEN
        assertThrows(UnauthorizedException.class, () ->amazonGameLiftClientWrapper.deregisterCompute(DEREGISTER_COMPUTE_REQUEST));

        //THEN
        verify(mockAmazonGameLift).deregisterCompute(DEREGISTER_COMPUTE_REQUEST);
    }

    @Test
    public void GIVEN_InvalidRequestException_WHEN_deregisterCompute_THEN_InvalidRequestException() {
        //GIVEN
        when(mockAmazonGameLift.deregisterCompute(DEREGISTER_COMPUTE_REQUEST))
                .thenThrow(com.amazonaws.services.gamelift.model.InvalidRequestException.class);

        //WHEN
        assertThrows(InvalidRequestException.class, () ->amazonGameLiftClientWrapper.deregisterCompute(DEREGISTER_COMPUTE_REQUEST));

        //THEN
        verify(mockAmazonGameLift).deregisterCompute(DEREGISTER_COMPUTE_REQUEST);
    }

    @Test
    public void GIVEN_NotFoundException_WHEN_deregisterCompute_THEN_NotFoundException() {
        //GIVEN
        when(mockAmazonGameLift.deregisterCompute(DEREGISTER_COMPUTE_REQUEST))
                .thenThrow(com.amazonaws.services.gamelift.model.NotFoundException.class);

        //WHEN
        assertThrows(NotFoundException.class, () ->amazonGameLiftClientWrapper.deregisterCompute(DEREGISTER_COMPUTE_REQUEST));

        //THEN
        verify(mockAmazonGameLift).deregisterCompute(DEREGISTER_COMPUTE_REQUEST);
    }

    @Test
    public void GIVEN_InternalServiceException_WHEN_deregisterCompute_THEN_InternalServiceException() {
        //GIVEN
        when(mockAmazonGameLift.deregisterCompute(DEREGISTER_COMPUTE_REQUEST))
                .thenThrow(com.amazonaws.services.gamelift.model.InternalServiceException.class);

        //WHEN
        assertThrows(InternalServiceException.class, () ->amazonGameLiftClientWrapper.deregisterCompute(DEREGISTER_COMPUTE_REQUEST));

        //THEN
        verify(mockAmazonGameLift).deregisterCompute(DEREGISTER_COMPUTE_REQUEST);
    }
}
