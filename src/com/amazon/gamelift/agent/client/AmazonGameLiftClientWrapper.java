/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.client;

import com.amazon.gamelift.agent.model.exception.ConflictException;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.NotFoundException;
import com.amazon.gamelift.agent.model.exception.UnauthorizedException;
import com.amazon.gamelift.agent.model.gamelift.GetComputeAuthTokenResponse;
import com.amazon.gamelift.agent.model.gamelift.RegisterComputeResponse;
import com.amazonaws.services.gamelift.AmazonGameLift;
import com.amazonaws.services.gamelift.model.Compute;
import com.amazonaws.services.gamelift.model.DeregisterComputeRequest;
import com.amazonaws.services.gamelift.model.DeregisterComputeResult;
import com.amazonaws.services.gamelift.model.GetComputeAuthTokenRequest;
import com.amazonaws.services.gamelift.model.GetComputeAuthTokenResult;
import com.amazonaws.services.gamelift.model.RegisterComputeRequest;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Instant;

@Slf4j
public class AmazonGameLiftClientWrapper {
    private final AmazonGameLift amazonGameLift;

    /**
     * Constructor for AmazonGameLiftClientWrapper
     * @param amazonGameLift
     */
    @Inject
    public AmazonGameLiftClientWrapper(final AmazonGameLift amazonGameLift) {
        this.amazonGameLift = amazonGameLift;
    }

    /**
     * Calls Amazon GameLift to get a ComputeAuthToken
     * @param request
     * @return
     * @throws UnauthorizedException
     * @throws InvalidRequestException
     * @throws NotFoundException
     * @throws InternalServiceException
     */
    public GetComputeAuthTokenResponse getComputeAuthToken(final GetComputeAuthTokenRequest request)
            throws UnauthorizedException, InvalidRequestException, NotFoundException, InternalServiceException {
        try {
            final GetComputeAuthTokenResult result = amazonGameLift.getComputeAuthToken(request);

            return GetComputeAuthTokenResponse.builder()
                    .computeName(result.getComputeName())
                    .fleetId(result.getFleetId())
                    .authToken(result.getAuthToken())
                    .expirationTimeEpochMillis(Instant.ofEpochMilli(result.getExpirationTimestamp().getTime()))
                    .build();
        } catch (final com.amazonaws.services.gamelift.model.UnauthorizedException e) {
            throw new UnauthorizedException("Failed to authorize request", e);
        } catch (final com.amazonaws.services.gamelift.model.InvalidRequestException e) {
            throw new InvalidRequestException("Invalid request", e);
        } catch (final com.amazonaws.services.gamelift.model.NotFoundException e) {
            throw new NotFoundException("Resource not found", e);
        } catch (final com.amazonaws.services.gamelift.model.InternalServiceException e) {
            throw new InternalServiceException("Internal Server Issue", e);
        }
    }

    /**
     * Calls Amazon GameLift to register compute
     * @param request
     * @return
     * @throws UnauthorizedException
     * @throws InvalidRequestException
     * @throws ConflictException
     * @throws InternalServiceException
     */
    public RegisterComputeResponse registerCompute(final RegisterComputeRequest request) throws UnauthorizedException,
            InvalidRequestException, ConflictException, InternalServiceException {
        log.info("RegisterCompute request received. Processing...");
        try {
            final Compute computeResult = amazonGameLift.registerCompute(request).getCompute();

            return RegisterComputeResponse.builder()
                    .fleetId(computeResult.getFleetId())
                    .computeName(computeResult.getComputeName())
                    .sdkWebsocketEndpoint(computeResult.getGameLiftServiceSdkEndpoint())
                    .status(computeResult.getComputeStatus())
                    .location(computeResult.getLocation())
                    .creationTime(computeResult.getCreationTime())
                    .build();
        } catch (final com.amazonaws.services.gamelift.model.UnauthorizedException e) {
            throw new UnauthorizedException("Failed to authorize request", e);
        } catch (final com.amazonaws.services.gamelift.model.InvalidRequestException e) {
            throw new InvalidRequestException("Invalid request", e);
        } catch (final com.amazonaws.services.gamelift.model.ConflictException e) {
            throw new ConflictException("Resource already exists", e);
        } catch (final com.amazonaws.services.gamelift.model.InternalServiceException e) {
            throw new InternalServiceException("Internal Server Issue", e);
        }
    }

    /**
     * Calls Amazon GameLift to register compute
     * @param deregisterComputeRequest
     * @return
     * @throws UnauthorizedException
     * @throws InvalidRequestException
     * @throws NotFoundException
     * @throws InternalServiceException
     */
    public DeregisterComputeResult deregisterCompute(final DeregisterComputeRequest deregisterComputeRequest)
            throws UnauthorizedException, InvalidRequestException, NotFoundException, InternalServiceException {
        log.info("DeregisterCompute request received. Processing...");
        try {
            return amazonGameLift.deregisterCompute(deregisterComputeRequest);
        } catch (final com.amazonaws.services.gamelift.model.UnauthorizedException e) {
            throw new UnauthorizedException("Failed to authorized request", e);
        } catch (final com.amazonaws.services.gamelift.model.InvalidRequestException e) {
            throw new InvalidRequestException("Invalid request", e);
        } catch (final com.amazonaws.services.gamelift.model.NotFoundException e) {
            throw new NotFoundException("Resource not found", e);
        } catch (final com.amazonaws.services.gamelift.model.InternalServiceException e) {
            throw new InternalServiceException("Internal Server Issue", e);
        }
    }
}
