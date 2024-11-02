/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import com.amazon.gamelift.agent.model.ConfiguredLogPaths;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GameSessionLogFileHelperTest {
    private final String tooLongPathsLinux = "/local/game/logs/"
            + RandomStringUtils.randomAlphanumeric(GameSessionLogFileHelper.MAXIMUM_UNIX_PATH_LENGTH) + "/";
    private final List<String> validLogsPathsLinux = ImmutableList.of(
            "/local/game/logs/",
            "/local/game/other/"
    );
    private final List<String> invalidLogsPathsLinux = ImmutableList.of(
            "/invalidDir/",
            "/local/game/logs<>/",
            tooLongPathsLinux
    );

    private final String validLogPathOneWindows = "C:\\Game\\logs\\";
    private final String validLogPathTwoWindows = "C:\\Game\\other\\";
    private final List<String> validLogsPathsWindows = ImmutableList.of(
            validLogPathOneWindows,
            validLogPathTwoWindows
    );

    private final String tooLongPathsWindows = "C:\\Game\\logs\\"
            + RandomStringUtils.randomAlphanumeric(GameSessionLogFileHelper.MAXIMUM_WINDOWS_PATH_LENGTH) + "/";
    private final String invalidLogPathOneWindows = "C:\\InvalidDir\\logs\\";
    private final String invalidLogPathTwoWindows = "C:\\Game\\logs<>\\";
    private final List<String> invalidLogsPathsWindows = ImmutableList.of(
            invalidLogPathOneWindows,
            invalidLogPathTwoWindows,
            tooLongPathsWindows
    );

    private final String linuxLaunchPath = "/local/game/server.exe";
    private final String windowsLaunchPath = "C:\\Game\\GameFolder\\server.exe";

    @Mock private Path mockPath;
    @Mock private Path mockPathParent;

    @Test
    public void GIVEN_validWindowsLogPaths_WHEN_configureLogPaths_THEN_pathsConfigured() {
        // GIVEN
        final List<String> logPaths = new ArrayList<>(validLogsPathsWindows);
        final GameSessionLogFileHelper gameSessionLogFileHelper =
                new GameSessionLogFileHelper(OperatingSystem.WINDOWS_2019);
        when(mockPath.getParent()).thenReturn(mockPathParent);
        when(mockPathParent.toString()).thenReturn("C:\\Game\\");
        when(mockPathParent.isAbsolute()).thenReturn(true);

        // WHEN
        try (MockedStatic<Paths> mockPaths = Mockito.mockStatic(Paths.class)) {
            try (MockedStatic<FilenameUtils> mockFileNameUtils = Mockito.mockStatic(FilenameUtils.class)) {
                mockPaths.when(() -> Paths.get(anyString())).thenReturn(mockPathParent);
                mockPaths.when(() -> Paths.get(windowsLaunchPath)).thenReturn(mockPath);

                mockFileNameUtils.when(() -> FilenameUtils.separatorsToSystem(validLogPathOneWindows))
                        .thenReturn(validLogPathOneWindows);
                mockFileNameUtils.when(() -> FilenameUtils.separatorsToSystem(validLogPathTwoWindows))
                        .thenReturn(validLogPathTwoWindows);

                final ConfiguredLogPaths configuredLogPaths =
                        gameSessionLogFileHelper.configureLogPaths(logPaths, windowsLaunchPath);
                final List<String> actualValidLogPaths = configuredLogPaths.getValidLogPaths();

                // THEN
                assertTrue(configuredLogPaths.getInvalidLogPaths().isEmpty());
                assertEquals(actualValidLogPaths.size(), validLogsPathsWindows.size());
                assertTrue(actualValidLogPaths.containsAll(validLogsPathsWindows));
            }
        }
    }

    @Test
    public void GIVEN_validLinuxLogPaths_WHEN_configureLogPaths_THEN_pathsConfigured() {
        // GIVEN
        final List<String> logPaths = new ArrayList<>(validLogsPathsLinux);
        final GameSessionLogFileHelper gameSessionLogFileHelper =
                new GameSessionLogFileHelper(OperatingSystem.AMAZON_LINUX_2);

        // WHEN
        final ConfiguredLogPaths configuredLogPaths =
                gameSessionLogFileHelper.configureLogPaths(logPaths, linuxLaunchPath);
        final List<String> actualValidLogPaths = configuredLogPaths.getValidLogPaths();

        // THEN
        assertEquals(validLogsPathsLinux.size(), actualValidLogPaths.size());
        assertTrue(actualValidLogPaths.containsAll(validLogsPathsLinux));
        assertTrue(configuredLogPaths.getInvalidLogPaths().isEmpty());
    }

    @Test
    public void GIVEN_invalidCharacterWindowsLogPath_WHEN_configureLogPaths_THEN_pathsConfigured() {
        // GIVEN
        final List<String> logPaths = new ArrayList<>(invalidLogsPathsWindows);
        final GameSessionLogFileHelper gameSessionLogFileHelper =
                new GameSessionLogFileHelper(OperatingSystem.WINDOWS_2019);
        when(mockPath.getParent()).thenReturn(mockPathParent);
        when(mockPathParent.toString()).thenReturn("C:\\Game\\");
        when(mockPathParent.isAbsolute()).thenReturn(true);

        // WHEN
        try (MockedStatic<Paths> mockPaths = Mockito.mockStatic(Paths.class)) {
            try (MockedStatic<FilenameUtils> mockFileNameUtils = Mockito.mockStatic(FilenameUtils.class)) {
                mockPaths.when(() -> Paths.get(anyString())).thenReturn(mockPathParent);
                mockPaths.when(() -> Paths.get(windowsLaunchPath)).thenReturn(mockPath);

                mockFileNameUtils.when(() -> FilenameUtils.separatorsToSystem(invalidLogPathOneWindows))
                        .thenReturn(invalidLogPathOneWindows);
                mockFileNameUtils.when(() -> FilenameUtils.separatorsToSystem(invalidLogPathTwoWindows))
                        .thenReturn(invalidLogPathTwoWindows);
                mockFileNameUtils.when(() -> FilenameUtils.separatorsToSystem(tooLongPathsWindows))
                        .thenReturn(tooLongPathsWindows);

                final ConfiguredLogPaths configuredLogPaths =
                        gameSessionLogFileHelper.configureLogPaths(logPaths, windowsLaunchPath);
                final List<String> invalidConfiguredLogPaths = configuredLogPaths.getInvalidLogPaths();

                // THEN
                assertTrue(configuredLogPaths.getValidLogPaths().isEmpty());
                assertEquals(invalidConfiguredLogPaths.size(), invalidLogsPathsWindows.size());
                assertTrue(invalidConfiguredLogPaths.containsAll(invalidLogsPathsWindows));
            }
        }
    }

    @Test
    public void GIVEN_invalidLinuxLogPaths_WHEN_configureLogPaths_THEN_pathsConfigured() {
        // GIVEN
        final List<String> logPaths = new ArrayList<>(invalidLogsPathsLinux);
        final GameSessionLogFileHelper gameSessionLogFileHelper =
                new GameSessionLogFileHelper(OperatingSystem.AMAZON_LINUX_2);

        // WHEN
        final ConfiguredLogPaths configuredLogPaths =
                gameSessionLogFileHelper.configureLogPaths(logPaths, linuxLaunchPath);
        final List<String> invalidConfiguredLogPaths = configuredLogPaths.getInvalidLogPaths();

        // THEN
        assertEquals(invalidConfiguredLogPaths.size(), invalidLogsPathsLinux.size());
        assertTrue(invalidConfiguredLogPaths.containsAll(invalidLogsPathsLinux));
        assertTrue(configuredLogPaths.getValidLogPaths().isEmpty());
    }
}
