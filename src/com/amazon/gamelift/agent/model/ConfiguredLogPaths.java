/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import com.amazon.gamelift.agent.logging.GameSessionLogPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
@RequiredArgsConstructor
public class ConfiguredLogPaths {
    private final Set<String> validLogPaths = new HashSet<>();
    private final Set<String> invalidLogPaths = new HashSet<>();

    /**
     * Add a valid log path to the collection
     * @param logPath
     */
    public void addValidLogPath(final String logPath) {
        validLogPaths.add(logPath);
    }

    /**
     * Add an invalid log path to the collection
     * @param logPath
     */
    public void addInvalidLogPath(final String logPath) {
        invalidLogPaths.add(logPath);
    }

    /**
     * Returns the valid log paths as a list (instead of the set used in this class for duplicate reduction)
     * @return List<String>
     */
    public List<String> getValidLogPaths() {
        return validLogPaths.stream().collect(Collectors.toList());
    }

    /**
     * Returns the invalid log paths as a list (instead of the set used in this class for duplicate reduction)
     * @return List<String>
     */
    public List<String> getInvalidLogPaths() {
        return invalidLogPaths.stream().collect(Collectors.toList());
    }

    /**
     * Converts a list of Sting log paths into GameSessionLogPath objects
     * This object contains the full path for a log path, a relative path to use in the zip folder a filename if and
     * only if a wildcard is used (this will later be expanded into individual files for matching files)
     * @param logPaths
     * @return
     */
    public static List<GameSessionLogPath> convertToGameSessionLogPaths(final List<String> logPaths) {
        final List<GameSessionLogPath> gameSessionLogPaths = new ArrayList<>();
        for (String path: logPaths) {
            try {
                /*
                 * /Removes C:\ from the log path but maintains all the parent folders for destination path So
                 * C:\app\logs\sample.txt would be copied to $zip\logs\sample.txt
                 */
                final Path drivePath = Paths.get(FilenameUtils.getPrefix(path));

                String fileName = FilenameUtils.getName(path);
                if (fileName.indexOf("*") >= 0 || fileName.indexOf("?") >= 0) {
                    String fullPath = FilenameUtils.getFullPath(path);
                    Path relativeLogPath = Paths.get(fullPath);
                    relativeLogPath = drivePath.relativize(relativeLogPath);
                    gameSessionLogPaths.add(new GameSessionLogPath(fullPath, relativeLogPath.toString(), fileName));
                } else {
                    Path relativeLogPath = Paths.get(path);
                    relativeLogPath = drivePath.relativize(relativeLogPath);
                    gameSessionLogPaths.add(new GameSessionLogPath(path, relativeLogPath.toString()));
                }
            } catch (InvalidPathException ipe) {
                log.error("Error getting path for " + path, ipe);
            }
        }
        return gameSessionLogPaths;
    }
}
