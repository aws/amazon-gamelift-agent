/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import com.amazon.gamelift.agent.manager.LogConfigurationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;

import static com.amazon.gamelift.agent.module.ConfigModule.GAMELIFT_AGENT_LOGS_DIRECTORY;

/**
 * Helper class for methods related to locating/handling log files on the Compute
 */
public class LogFileHelper {

    private static final String ZIP_EXTENSION = ".zip";
    private static final String GZIP_EXTENSION = ".gz";

    private final File gameLiftAgentLogDirectory;
    private final FileFilter gameLiftAgentLogFileFilter;

    /**
     * Constructor for LogFileHelper
     * @param gameLiftAgentLogDirectory
     * @param agentLogFileFilter
     */
    @Inject
    public LogFileHelper(@Named(GAMELIFT_AGENT_LOGS_DIRECTORY) final File gameLiftAgentLogDirectory,
                         final AgentLogFileFilter agentLogFileFilter) {
        this.gameLiftAgentLogDirectory = gameLiftAgentLogDirectory;
        this.gameLiftAgentLogFileFilter = agentLogFileFilter;
    }

    /**
     * Returns list of GameLiftAgent logs (archived)
     * @return
     */
    public List<File> getArchivedGameLiftAgentLogs() {
        return Arrays.stream(Objects.requireNonNull(gameLiftAgentLogDirectory.listFiles(gameLiftAgentLogFileFilter)))
                .filter(this::isArchivedFile)
                .collect(Collectors.toList());
    }

    /**
     * Gets the active GameLiftAgent log.
     * The active log file's name should match the configured base log name. This function assumes there will only
     * ever be one active log file in this directory.
     * @return an optional of the active log file
     */
    public Optional<File> getActiveGameLiftAgentLog() {
        return Arrays.stream(Objects.requireNonNull(gameLiftAgentLogDirectory.listFiles(gameLiftAgentLogFileFilter)))
                .filter(file -> LogConfigurationManager.APPLICATION_LOG_FILE_BASENAME.equals(file.getName()))
                .findFirst();
    }

    /**
     * Returns true if GameLiftAgent log file is archived
     * @param fileToVerify
     * @return
     */
    public boolean isArchivedFile(final File fileToVerify) {
        return fileToVerify.getName().endsWith(ZIP_EXTENSION) || fileToVerify.getName().endsWith(GZIP_EXTENSION);
    }

    /**
     * Will push all logs currently in memory buffers out to their respective files
     */
    public static void flushLogBuffers() {
        final LoggerContext logCtx = ((LoggerContext) LogManager.getContext());
        for (final Logger logger : logCtx.getLoggers()) {
            for (final Appender appender : logger.getAppenders().values()) {
                if (appender instanceof AbstractOutputStreamAppender) {
                    ((AbstractOutputStreamAppender) appender).getManager().flush();
                }
            }
        }
    }
}
