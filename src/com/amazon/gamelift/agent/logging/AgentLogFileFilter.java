/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import javax.inject.Inject;

import com.amazon.gamelift.agent.manager.LogConfigurationManager;

/**
 * A basic log file filter that helps the GameLift agent filter out undesirable files in the agent Log directory,
 * so that only actual log files get uploaded
 */
public class AgentLogFileFilter implements FileFilter {

    /**
     * Empty constructor for AgentLogFileFilter
     */
    @Inject
    public AgentLogFileFilter() { }

    @Override
    public boolean accept(final File file) {
        final boolean baseNameMatchesExpected =
                file.getName().startsWith(LogConfigurationManager.APPLICATION_LOG_FILE_BASENAME);

        return (file.isFile()
                && !file.isDirectory()
                && baseNameMatchesExpected
                && !Files.isSymbolicLink(file.toPath()));
    }
}
