/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import lombok.Getter;

@Getter
public class GameSessionLogPath {
    private final String sourcePath;
    private final String relativePathInZip;
    private final String wildcardToGet;

    /**
     * Constructor for GameSessionLogPath
     * @param sourcePath - Path for the log file to be copied / uploaded
     * @param relativePathInZip - Relative path for the log file within the zip file created for upload
     */
    public GameSessionLogPath(final String sourcePath, final String relativePathInZip) {
        this(sourcePath, relativePathInZip, null);
    }

    /**
     * Constructor for GameSessionLogPath
     * @param sourcePath - Path for the log file to be copied / uploaded
     * @param relativePathInZip - Relative path for the log file within the zip file created for upload
     * @param wildcardToGet - Wildcard to be expanded into any matching existing log files
     */
    public GameSessionLogPath(final String sourcePath, final String relativePathInZip, final String wildcardToGet) {
        this.sourcePath = sourcePath;
        this.relativePathInZip = relativePathInZip;
        this.wildcardToGet = wildcardToGet == null ? null : wildcardToGet.toLowerCase();
    }
}
