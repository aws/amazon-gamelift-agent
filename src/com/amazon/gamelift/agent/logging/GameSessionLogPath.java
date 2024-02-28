/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import lombok.Getter;

@Getter
public class GameSessionLogPath {
    private String sourcePath;
    private String relativePathInZip;
    private String wildcardToGet;

    /**
     * Constructor for GameSessionLogPath
     * @param sourcePath - Path for the log file to be copied / uploaded
     * @param relativePathInZip - Relative path for the log file within the zip file created for upload
     */
    public GameSessionLogPath(String sourcePath, String relativePathInZip) {
        this(sourcePath, relativePathInZip, null);
    }

    /**
     * Constructor for GameSessionLogPath
     * @param sourcePath - Path for the log file to be copied / uploaded
     * @param relativePathInZip - Relative path for the log file within the zip file created for upload
     * @param wildcardToGet - Wildcard to be expanded into any matching existing log files
     */
    public GameSessionLogPath(String sourcePath, String relativePathInZip, String wildcardToGet) {
        this.sourcePath = sourcePath;
        this.relativePathInZip = relativePathInZip;
        this.wildcardToGet = wildcardToGet == null ? null : wildcardToGet.toLowerCase();
    }
}
