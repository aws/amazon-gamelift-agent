/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExecutorServiceSafeRunnableTest {

    @Test
    public void GIVEN_runnableThrowsException_WHEN_run_THEN_suppressExceptions() {
        // GIVEN
        final Runnable runnableThatThrows = () -> {
            throw new RuntimeException("This should be caught and suppressed! O_O");
        };

        // WHEN
        new ExecutorServiceSafeRunnable(runnableThatThrows).run();

        // THEN - no exception
    }
}
