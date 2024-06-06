/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.gamelift.model.ConflictException;
import com.amazonaws.services.gamelift.model.InternalServiceException;
import com.amazonaws.services.gamelift.model.NotFoundException;
import com.amazonaws.services.gamelift.model.NotReadyException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AmazonGameLiftRetryCondition extends PredefinedRetryPolicies.SDKDefaultRetryCondition {

    /**
     * Override shouldRetry method to add GameLift exceptions.
     */
    @Override
    public boolean shouldRetry(final AmazonWebServiceRequest originalRequest,
                               final AmazonClientException exception,
                               final int retries) {
        // Parent class handles IOException, 5XXs, and ClockSkew issues.
        // Second method checks for retryable GameLift exceptions.
        return super.shouldRetry(originalRequest, exception, retries) || isRetryableGameLiftException(exception);
    }

    /**
     * Determine whether to retry based on GameLift exceptions.
     */
    private boolean isRetryableGameLiftException(final AmazonClientException exception) {
        if (exception instanceof ConflictException) {
            // Potential conflict could succeed on retries, otherwise will exhaust retries and throw ConflictException
            return true;
        } else if (exception instanceof InternalServiceException) {
            // Unknown service failure, always retry
            return true;
        } else if (exception instanceof NotFoundException) {
            // Resource could be eventually consistent, otherwise will exhaust retries and throw NotFoundException
            return true;
        } else if (exception instanceof NotReadyException) {
            // Resource is not ready yet, always retry
            return true;
        }
        return false;
    }
}
