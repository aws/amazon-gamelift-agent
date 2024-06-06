/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.gamelift.model.ConflictException;
import com.amazonaws.services.gamelift.model.InternalServiceException;
import com.amazonaws.services.gamelift.model.InvalidRequestException;
import com.amazonaws.services.gamelift.model.NotFoundException;
import com.amazonaws.services.gamelift.model.NotReadyException;
import com.amazonaws.services.gamelift.model.UnauthorizedException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmazonGameLiftRetryConditionTest {

    final AmazonGameLiftRetryCondition retryCondition = new AmazonGameLiftRetryCondition();

    @ParameterizedTest
    @MethodSource("exceptionUseCases")
    public void GIVEN_exception_WHEN_shouldRetry_THEN_expect(final AmazonClientException exception,
                                                             final boolean shouldRetry) {
        final boolean output = retryCondition.shouldRetry(null, exception, 0);
        assertEquals(shouldRetry, output);
    }

    public static Object[][] exceptionUseCases() {
        final List<Object[]> testCases = new ArrayList<>();
        // Expected retries
        testCases.add(new Object[] {new ConflictException("ConflictException"), true});
        testCases.add(new Object[] {new InternalServiceException("InternalServiceException"), true});
        testCases.add(new Object[] {new NotFoundException("NotFoundException"), true});
        testCases.add(new Object[] {new NotReadyException("NotReadyException"), true});
        AmazonServiceException throttlingException = new AmazonServiceException("ThrottlingException");
        throttlingException.setStatusCode(HttpStatus.SC_TOO_MANY_REQUESTS);
        testCases.add(new Object[] {throttlingException, true});
        AmazonServiceException badGatewayException = new AmazonServiceException("BadGatewayException");
        badGatewayException.setStatusCode(HttpStatus.SC_BAD_GATEWAY);
        testCases.add(new Object[] {badGatewayException, true});
        // Do not retry
        testCases.add(new Object[] {new InvalidRequestException("InvalidRequestException"), false});
        testCases.add(new Object[] {new UnauthorizedException("UnauthorizedException"), false});
        testCases.add(new Object[] {new AmazonClientException("GenericException"), false});
        testCases.add(new Object[] {new AmazonServiceException("GenericException"), false});
        return testCases.toArray(new Object[testCases.size()][]);
    }
}
