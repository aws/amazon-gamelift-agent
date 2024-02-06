/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.command;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinuxCommandTransformTest {

    private static final GameProcessConfiguration PROCESS_CONFIG = GameProcessConfiguration.builder()
            .concurrentExecutions(1)
            .launchPath("testCommand")
            .parameters("--parameter1 --parameter2")
            .build();

    private static final String USER = "TEST_USER";

    private CommandTransform commandTransform;

    @BeforeEach
    public void setup() throws IOException {
        commandTransform = new LinuxCommandTransform();
    }

    @Test
    public void GIVEN_validInput_WHEN_getFullCommandFromConfig_THEN_returnLinuxCommand() {
        // Given

        // When
        List<String> result = commandTransform.getFullCommandFromConfig(PROCESS_CONFIG);

        // Then
        assertEquals(List.of("setsid", "testCommand", "--parameter1", "--parameter2"), result);
    }

}