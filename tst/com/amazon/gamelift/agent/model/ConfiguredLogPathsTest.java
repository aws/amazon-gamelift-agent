/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import com.amazon.gamelift.agent.logging.GameSessionLogPath;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class ConfiguredLogPathsTest {

    @Test
    public void GIVEN_logPaths_WHEN_getValidInvalidLogPaths_THEN_listReturned() {
        // GIVEN
        final ConfiguredLogPaths configuredLogPaths = new ConfiguredLogPaths();
        final String goodLogPath = "/logs/goodLogPath.txt";
        final String badLogPath = "/bad/badLogPath.txt";
        configuredLogPaths.addValidLogPath(goodLogPath);
        configuredLogPaths.addInvalidLogPath(badLogPath);

        final List<String> expectedValidLogPaths = ImmutableList.of(goodLogPath);
        final List<String> expectedInvalidLogPaths = ImmutableList.of(badLogPath);

        // WHEN
        final List<String> actualValidLogPaths = configuredLogPaths.getValidLogPaths();
        final List<String> actualInvalidLogPaths = configuredLogPaths.getInvalidLogPaths();

        // THEN
        assertEquals(expectedValidLogPaths.size(), actualValidLogPaths.size());
        assertTrue(expectedValidLogPaths.containsAll(actualValidLogPaths));

        assertEquals(expectedInvalidLogPaths.size(), actualInvalidLogPaths.size());
        assertTrue(expectedInvalidLogPaths.containsAll(actualInvalidLogPaths));
    }

    @Test
    public void GIVEN_logPaths_WHEN_convertToGameSessionLogPaths_THEN_convertedAsExpected() {
        // GIVEN
        final ConfiguredLogPaths configuredLogPaths = new ConfiguredLogPaths();
        final String goodLogPath = "/logs/goodLogPath.txt";
        final String relativeGoodLogPath = "logs/goodLogPath.txt";

        final String wildcardLogPath = "/logs/moreLogs/*.txt";
        final String sourceWildcardLogPath = "/logs/moreLogs/";
        final String relativeWildcardLogPath = "logs/moreLogs/";
        final String wildcardFilename = ".txt";

        configuredLogPaths.addValidLogPath(goodLogPath);
        configuredLogPaths.addValidLogPath(wildcardLogPath);

        final List<GameSessionLogPath> expectedGameSessionLogPaths = new ArrayList<>();
        expectedGameSessionLogPaths.add(new GameSessionLogPath(goodLogPath, relativeGoodLogPath, null));
        expectedGameSessionLogPaths.add(new GameSessionLogPath(sourceWildcardLogPath,
                relativeWildcardLogPath, wildcardFilename));

        // WHEN
        final List<GameSessionLogPath> actualGameSessionLogPaths =
                ConfiguredLogPaths.convertToGameSessionLogPaths(configuredLogPaths.getValidLogPaths());

        // THEN
        assertEquals(expectedGameSessionLogPaths.size(), actualGameSessionLogPaths.size(),
                String.format("Actual game session log path size %s differed from expected %s",
                        actualGameSessionLogPaths.size(), expectedGameSessionLogPaths.size()));

        int validatedActualLogPaths = 0;
        for (GameSessionLogPath expectedLogPath : expectedGameSessionLogPaths) {
            for (GameSessionLogPath actualLogPath : actualGameSessionLogPaths) {
                if (expectedLogPath.getSourcePath() == actualLogPath.getSourcePath()
                        && expectedLogPath.getRelativePathInZip() == actualLogPath.getRelativePathInZip()
                        && expectedLogPath.getWildcardToGet() == actualLogPath.getWildcardToGet()) {
                    validatedActualLogPaths++;
                }
            }
        }
        assertEquals(expectedGameSessionLogPaths.size(), actualGameSessionLogPaths.size(),
                String.format("Count of validated actual GameSessionLogPath %s differed from expected %s",
                        validatedActualLogPaths, expectedGameSessionLogPaths.size()));
    }

}
