/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import com.amazon.gamelift.agent.utils.ExecutorServiceSafeRunnable;
import com.amazon.gamelift.agent.manager.ExecutorServiceManager;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.module.ConfigModule;
import com.amazon.gamelift.agent.module.ThreadingModule;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.amazon.gamelift.agent.module.ThreadingModule.GAMELIFT_AGENT_LOG_UPLOADER_EXECUTOR;

/**
 * Class that owns handling the scheduled upload of GameLift agent log files to S3
 */
@Slf4j
@Singleton
public class GameLiftAgentLogUploader {

    private static final long UPLOAD_INTERVAL_IN_SECONDS = Duration.ofHours(1).toSeconds();
    private static final long FIFTEEN_MINUTES_IN_SECONDS = Duration.ofMinutes(15).toSeconds();
    private static final int FIVE_MIN_AFTER_HOUR = 65;
    private static final String S3_FILE_KEY_FORMAT = "%s/%s/%s";

    private final LogFileHelper logFileHelper;
    private final S3FileUploader s3FileUploader;
    private final String agentLogBucket;
    private final String fleetId;
    private final String computeName;
    private final ExecutorServiceManager executorServiceManager;
    private final ScheduledExecutorService executorService;

    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final AtomicBoolean skipUploadLogged = new AtomicBoolean(false);

    /**
     * Constructor for GameLiftAgentLogUploader
     */
    @Inject
    public GameLiftAgentLogUploader(
            final LogFileHelper logFileHelper,
            final S3FileUploader s3FileUploader,
            @Named(ConfigModule.GAMELIFT_AGENT_LOG_BUCKET) @Nullable final String agentLogBucket,
            @Named(ConfigModule.FLEET_ID) @Nullable final String fleetId,
            @Named(ConfigModule.COMPUTE_NAME) @Nullable final String computeName,
            @Named(ThreadingModule.EXECUTOR_SERVICE_MANAGER) final ExecutorServiceManager executorServiceManager,
            @Named(GAMELIFT_AGENT_LOG_UPLOADER_EXECUTOR) final ScheduledExecutorService executorService) {
        this.logFileHelper = logFileHelper;
        this.s3FileUploader = s3FileUploader;
        this.agentLogBucket = agentLogBucket;
        this.fleetId = fleetId;
        this.computeName = computeName;
        this.executorServiceManager = executorServiceManager;
        this.executorService = executorService;
    }

    /**
     * If the log uploader has not been started, this method will schedule the log uploader to run at a schedule
     * which will upload the log at some time from the 5th-20th minute in the hour. This is done to distribute
     * S3 traffic, and also to make sure Log4j has a chance to rotate the active log file.
     */
    public void start() {
        if (isStarted.compareAndSet(false, true)) {
            // Calculate initial delay of log uploading task. This task should run somewhere in between 5 - 20 min
            // after the hour every hour, with jitter to randomly distribute traffic
            final long currentMinute = LocalTime.now().getMinute();
            final long minuteToFiveAfterHour = currentMinute < 5 ? 5 - currentMinute : FIVE_MIN_AFTER_HOUR - currentMinute;
            final long jitterSeconds = ThreadLocalRandom.current().nextLong(0, FIFTEEN_MINUTES_IN_SECONDS);
            final long initialDelaySeconds = Duration.ofMinutes(minuteToFiveAfterHour).toSeconds() + jitterSeconds;

            final LocalTime firstLogUploadTime = LocalTime.now().plusSeconds(initialDelaySeconds);
            log.info("Scheduling GameLiftAgent log upload with initialDelay of {} seconds to run at: {}.",
                    initialDelaySeconds, firstLogUploadTime);
            executorService.scheduleWithFixedDelay(new ExecutorServiceSafeRunnable(this::runScheduledLogUpload),
                    initialDelaySeconds, UPLOAD_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
        } else {
            log.warn("Attempted to start GameLiftAgent log upload daemon, but it has already started; ignoring request.");
        }
    }

    /**
     * Terminates the scheduled uploader thread and uploads all GameLiftAgent log files.
     * This is intended to only be executed when the host is shutting down.
     */
    public void shutdownAndUploadLogs() {
        if (isStarted.compareAndSet(true, false)) {
            log.info("Stopping scheduled GameLiftAgent log upload manager and uploading all logs on host.");
            executorServiceManager.shutdownScheduledThreadPoolExecutorServiceByName(this.getClass().getSimpleName());
            for (final File logFile : logFileHelper.getArchivedGameLiftAgentLogs()) {
                try {
                    uploadLogFile(logFile);
                } catch (final Exception e) {
                    log.error("Unable to upload log file at path {}; swallowing exception to unblock other log uploads",
                            logFile.getAbsolutePath(), e);
                }
            }

            // Flush all log buffers right before uploading to S3, normally this is done with the default log4j
            // shutdown hook, but that executes in parallel and will start to cut off logs from our shutdown hook.
            LogFileHelper.flushLogBuffers();

            final Optional<File> activeLogFile = logFileHelper.getActiveGameLiftAgentLog();
            if (activeLogFile.isPresent()) {
                try {
                    uploadLogFile(activeLogFile.get());
                } catch (final Exception e) {
                    log.error("Unable to upload active GameLiftAgent log file at path {}; swallowing exception",
                            activeLogFile.get().getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * If a GameLiftAgentLogBucket is not provided, this will not upload the logs to S3, but still deletes
     * GameLiftAgent logs to avoid filling up the disk.
     * If a GameLiftAgentLogBucket is provided, this will upload to S3 before deleting the GameLiftAgent log.
     */
    @VisibleForTesting void runScheduledLogUpload() {
        log.info("Starting scheduled hourly GameLiftAgent log upload");
        int failedUploadCount = 0;
        int failedDeletionCount = 0;
        for (final File logFile : logFileHelper.getArchivedGameLiftAgentLogs()) {
            try {
                uploadLogFile(logFile);
            } catch (final Exception e) {
                failedUploadCount++;
                log.error("Unable to upload log file at path {}; swallowing exception and "
                        + "retrying on the next scheduled upload", logFile.getAbsolutePath(), e);
                continue;
            }
            try {
                deleteLogFile(logFile);
            } catch (final Exception e) {
                failedDeletionCount++;
                log.error("Unable to delete GameLiftAgent log file: " + logFile.getAbsolutePath(), e);
            }
        }
        log.info("Completed scheduled hourly log upload. {} failed upload(s) and {} failed log file deletions.",
                failedUploadCount, failedDeletionCount);
    }

    private void uploadLogFile(final File logFile) throws AgentException {
        if (StringUtils.isNotBlank(agentLogBucket)) {
            // In order to save on storage costs, determine if the file is zipped or needs to be zipped
            final boolean shouldZipLogFile = !logFileHelper.isArchivedFile(logFile);
            // Upload the log to S3 key FleetId/ComputeName/<file-name>
            final String fileKey;
            if (shouldZipLogFile) {
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
                final String logFileKeySuffix = formatter.format(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
                final String timestampedLogFileName = logFile.getName() + "." + logFileKeySuffix;
                fileKey = String.format(S3_FILE_KEY_FORMAT, fleetId, computeName, timestampedLogFileName);
            } else {
                fileKey = String.format(S3_FILE_KEY_FORMAT, fleetId, computeName, logFile.getName());
            }

            s3FileUploader.uploadFile(agentLogBucket, fileKey, logFile, shouldZipLogFile);
        } else if (skipUploadLogged.compareAndSet(false, true)) {
            log.warn("GameLiftAgentLogBucket not specified, skipping GameLiftAgent log uploads.");
        }
    }

    private void deleteLogFile(final File logFile) throws IOException {
        log.info("Deleting GameLiftAgent log file: {}", logFile);
        Files.deleteIfExists(logFile.toPath());
    }
}
