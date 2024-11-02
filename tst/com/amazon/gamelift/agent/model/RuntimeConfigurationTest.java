/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class RuntimeConfigurationTest {
    @Test
    public void GIVEN_allValues_WHEN_builder_THEN_success() {
        // GIVEN
        final Integer gameSessionActivationTimeout = 100;
        final Integer maxConcurrentSessions = 10;
        final GameProcessConfiguration process = GameProcessConfiguration.builder().concurrentExecutions(1).launchPath("test").build();
        final GameProcessConfiguration process2 = GameProcessConfiguration.builder().
                concurrentExecutions(2).
                launchPath("test2").
                parameters("test")
                .build();
        // WHEN
        final RuntimeConfiguration config = RuntimeConfiguration.builder()
                .gameSessionActivationTimeoutSeconds(gameSessionActivationTimeout)
                .maxConcurrentGameSessionActivations(maxConcurrentSessions)
                .serverProcesses(Arrays.asList(process, process2))
                .build();
        // THEN
        assertEquals(gameSessionActivationTimeout, config.getGameSessionActivationTimeoutSeconds());
        assertEquals(maxConcurrentSessions, config.getMaxConcurrentGameSessionActivations());
        assertEquals(2, config.getServerProcesses().size());
        assertEquals(process, config.getServerProcesses().get(0));
        assertEquals(process2, config.getServerProcesses().get(1));
    }

    @Test
    public void GIVEN_onlyRequiredFields_WHEN_builder_THEN_success() {
        // GIVEN
        final GameProcessConfiguration process = GameProcessConfiguration.builder().concurrentExecutions(1).launchPath("test").build();
        // WHEN
        final RuntimeConfiguration config = RuntimeConfiguration.builder().serverProcesses(Collections.singletonList(process)).build();
        // THEN
        assertEquals(process, config.getServerProcesses().get(0));
    }

    @Test
    public void GIVEN_missingServerProcess_WHHEN_builder_THEN_failure() {
        // GIVEN
        // WHEN
        try {
            RuntimeConfiguration.builder().build();
            fail("RuntimeConfiguration must have non null server processes.");
        } catch (final NullPointerException e) {
            // THEN
        }
    }

    @Test
    public void GIVEN_noConcurrentExecutions_WHEN_builder_THEN_throws() {
        // THEN
          assertThrows(NullPointerException.class, ()-> GameProcessConfiguration.builder().launchPath("test").build());
    }
}
