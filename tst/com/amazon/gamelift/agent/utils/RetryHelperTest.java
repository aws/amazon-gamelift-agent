/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

import java.util.concurrent.Callable;

import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.gamelift.agent.model.exception.InternalServiceException;

@ExtendWith(MockitoExtension.class)
public class RetryHelperTest {
    @Mock private Callable<String> failsOnceFunc;

    @BeforeEach
    public void setup() {
        RetryHelper.disableBackoff();
    }

    @Test
    public void GIVEN_oneException_WHEN_runRetryable_THEN_retriesAndSucceeds() throws Exception {
        // GIVEN
        final int numRetries = 1;
        final String successString = "SUCCESS";
        when(failsOnceFunc.call()).thenThrow(new RuntimeException()).thenReturn(successString);

        // WHEN
        String result = RetryHelper.runRetryable(numRetries, false, failsOnceFunc);

        // THEN
        verify(failsOnceFunc, times(numRetries + 1)).call();
        assertEquals(result, successString);
    }

    @Test
    public void GIVEN_multipleException_WHEN_runRetryable_THEN_throws() throws Exception {
        // GIVEN
        final int numRetries = 5;
        when(failsOnceFunc.call()).thenThrow(new RuntimeException());

        // WHEN/Then
        assertThrows(RuntimeException.class, () -> RetryHelper.runRetryable(numRetries, false, failsOnceFunc));
        verify(failsOnceFunc, times(numRetries + 1)).call();
    }

    @Test
    public void GIVEN_invalidRequestException_WHEN_runRetryable_THEN_throws() throws Exception {
        // GIVEN
        when(failsOnceFunc.call()).thenThrow(new InvalidRequestException("test!"));

        // WHEN
        assertThrows(InvalidRequestException.class, () -> RetryHelper.runRetryable(failsOnceFunc));

        // THEN
        verify(failsOnceFunc, times(1)).call();
    }

    @Test
    public void GIVEN_internalServiceException_WHEN_runRetryable_THEN_retriesAndThrows() throws Exception {
        // GIVEN
        when(failsOnceFunc.call()).thenThrow(new InternalServiceException("test!"));

        // WHEN
        assertThrows(InternalServiceException.class, () -> RetryHelper.runRetryable(failsOnceFunc));

        // THEN
        verify(failsOnceFunc, times(3)).call();
    }

}
