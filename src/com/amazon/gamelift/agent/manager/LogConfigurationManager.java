/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.OperatingSystem;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Static helper class for configuring Log4j programmatically, intended to be called at the start of the PM.
 * A programmatic Java configuration is used in favor of a standard XML configuration because it allows us to
 * use the log paths defined in our other Java configurations, and gives us more control over
 * disabling logging for tests
 */
public class LogConfigurationManager {

    public static final String APPLICATION_LOG_FILE_BASENAME = "gameliftagent.log";
    private static final String APP_NAME = "GameLiftAgent";
    private static final String LOG_PATTERN_LAYOUT = "%d{dd MMM yyyy HH:mm:ss,SSS} %highlight{[%p]} (%t) %c: %m%n";
    private static final String APPLICATION_LOG_FILE_PATTERN_FORMAT =
            APPLICATION_LOG_FILE_BASENAME + ".%d{yyyy-MM-dd-HH}.gz";
    private static final String DISABLE_LOGGING_SYSTEM_PROPERTY_NAME = "gamelift.agent.disableFileLogging";

    // Integer ranging from 1-9 to determine how compressed the logs should be.
    // 1 indicates the fastest compression time performance, whereas 9 indicates the best file size compression
    private static final String COMPRESSION_LEVEL = "9";

    // Determines the amount of data the log appender will store in memory before flushing to the disk.
    // RollingRandomAccessFileAppender's default value is 256 * 1024 bytes. Using a larger value here will result in
    // better performance, but using a smaller value makes logs easier to view when debugging.
    private static final int APPENDER_BUFFER_SIZE = 16 * 1024; // 16KB

    /**
     * Configures logging setup
     * @throws IOException
     */
    public static void configureLogging(final String cliGameLiftAgentLogPath) throws IOException {
        // Disable shutdown hook which automatically shuts down logging, this cuts off some logs
        // made in own shutdown hooks if left enabled
        System.setProperty(DefaultShutdownCallbackRegistry.SHUTDOWN_HOOK_ENABLED, "false");

        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        // Log4j will automatically use a default configuration if log events are made before this method is called.
        // Remove these appenders so that they can be replaced with the ones initialized here.
        for (final String appenderName : config.getAppenders().keySet()) {
            config.getRootLogger().removeAppender(appenderName);
        }

        final Appender consoleAppender = createConsoleAppender(config);
        config.addAppender(consoleAppender);
        config.getRootLogger().addAppender(consoleAppender, null, null);

        final boolean disableLogging = Boolean.parseBoolean(System.getProperty(DISABLE_LOGGING_SYSTEM_PROPERTY_NAME));
        if (!disableLogging) {
            final Appender applicationLogAppender = createApplicationLogFileAppender(config, cliGameLiftAgentLogPath);
            config.addAppender(applicationLogAppender);
            config.getRootLogger().addAppender(applicationLogAppender, null, null);
        }

        configureLoggingLevels(config);
        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(Level.INFO);
        ctx.updateLoggers(config);
    }

    private static Appender createConsoleAppender(final Configuration config) {
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(LOG_PATTERN_LAYOUT)
                .withConfiguration(config)
                .build();
        final Appender consoleAppender = ConsoleAppender.createDefaultAppenderForLayout(layout);
        consoleAppender.start();
        return consoleAppender;
    }

    private static Appender createApplicationLogFileAppender(final Configuration config,
                                                             final String cliGameLiftAgentLogPath) throws IOException {
        final String agentLogPath;
        if (!StringUtils.isBlank(cliGameLiftAgentLogPath)) {
            agentLogPath = cliGameLiftAgentLogPath;
        } else {
            agentLogPath = OperatingSystem.fromSystemOperatingSystem().getAgentLogsFolder();
        }
        final Path activeAgentLogFilePath = Paths.get(
                agentLogPath,
                APPLICATION_LOG_FILE_BASENAME);
        final Path rotatedAgentLogFilePath = Paths.get(
                agentLogPath,
                APPLICATION_LOG_FILE_PATTERN_FORMAT);
        Files.createDirectories(activeAgentLogFilePath.getParent());

        final PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(LOG_PATTERN_LAYOUT)
                .withConfiguration(config)
                .build();

        final TriggeringPolicy triggeringPolicy = TimeBasedTriggeringPolicy.newBuilder()
                .withInterval(1)     // Time unit is based on the smallest unit used in the file pattern
                .withModulate(true)  // Force the time to be rounded to occur on a boundary aligned with the increment.
                .build();

        final RolloverStrategy rolloverStrategy = DefaultRolloverStrategy.newBuilder()
                .withCompressionLevelStr(COMPRESSION_LEVEL)
                .withConfig(config)
                .build();

        final RollingRandomAccessFileAppender applicationLogAppender = RollingRandomAccessFileAppender.newBuilder()
                .withFileName(activeAgentLogFilePath.toString())
                .withFilePattern(rotatedAgentLogFilePath.toString())
                .withAppend(true)                    // Always append to existing log files
                .withAdvertise(false)                // Do not advertise log4j config
                .setBufferSize(APPENDER_BUFFER_SIZE) // Set custom buffer size. This appender type always uses buffered IO
                .withPolicy(triggeringPolicy)
                .withStrategy(rolloverStrategy)
                .setName(APP_NAME)
                .setImmediateFlush(false)            // Don't immediately flush the buffer on write (degrades performance)
                .setLayout(layout)
                .setIgnoreExceptions(true)           // Ignore exceptions encountered when appending logs (errors will still get logged)
                .setConfiguration(config)
                .build();

        applicationLogAppender.start();
        return applicationLogAppender;
    }

    private static void configureLoggingLevels(final Configuration config) {
        // Remove Apache HTTP wire, header, and client wire logs in production to help prevent AWS request replay
        config.getLoggerConfig("org.apache.http.wire").setLevel(Level.INFO);
        config.getLoggerConfig("org.apache.http.headers").setLevel(Level.INFO);
        config.getLoggerConfig("httpclient.wire").setLevel(Level.INFO);

        // Remove raw AWS headers
        config.getLoggerConfig("com.amazonaws.auth.AWS4Signer").setLevel(Level.INFO);
        config.getLoggerConfig("com.amazonaws.request").setLevel(Level.INFO);

        // Remove EC2Metadata non-ERROR logging
        config.getLoggerConfig("com.amazonaws.util.EC2MetadataUtils").setLevel(Level.ERROR);
    }
}
