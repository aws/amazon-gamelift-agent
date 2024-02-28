/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import com.amazon.gamelift.agent.model.exception.AgentException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class UploadGameSessionLogsCallable implements Callable<Void> {

    private static final String S3_FILE_KEY_FORMAT = "%s/%s/%s.zip";
    private static final AtomicBoolean SKIP_UPLOAD_LOGGED = new AtomicBoolean(false);

    private final String gameSessionLogBucket;
    @NonNull private final String fleetId;
    @NonNull private final String computeName;
    @NonNull private final String processUUID;
    @NonNull private final List<String> logPaths;
    @Nullable private final String gameSessionId;
    private final S3FileUploader s3FileUploader;
    private final GameSessionLogsCollector gameSessionLogsCollector;

    /**
     * If a gameSessionLogBucket is not provided, the callable will output a log and do nothing.
     *
     * If a gameSessionLogBucket is provided, the GameLiftAgent will:
     *   1. Collect game session logs and zip them into 1 File.
     *   2. Upload the zip to the S3 bucket provided.
     *   3. Delete the zip File that was created.
     *   4. Delete the original game session logs so disk space doesn't continue filling up.
     * If any of the steps fail, an exception is thrown and following steps are not executed.
     */
    @Override
    public Void call() throws Exception {
        try {
            if (StringUtils.isNotBlank(gameSessionLogBucket)) {
                return performUpload();
            } else if (SKIP_UPLOAD_LOGGED.compareAndSet(false, true)) {
                log.warn("GameSessionLogBucket not specified, skipping GameSession log uploads.");
            }
        } catch (Exception e) {
            // Don't re-throw exceptions to avoid potentially impacted scheduled ExecutorServices using this callable
            log.error("Suppressing unexpected exception encountered when uploading GameSession logs", e);
        }
        return null;
    }

    private Void performUpload() throws Exception {
        // If process is associated with a game session ID the game session ID will be used as an identifier on the
        // uploaded log file. Otherwise, the process ID will be used instead.
        final String logUploadId = StringUtils.isBlank(gameSessionId) ? processUUID : gameSessionId;

        // Collect GameSession logs into a single zipped File
        log.info("Collecting logs for processUUID {}, GameSession {}", processUUID, gameSessionId);
        final File logFile = gameSessionLogsCollector.collectGameSessionLogs(logPaths, logUploadId);

        // Upload log files to S3
        uploadLogFile(logUploadId, logFile);

        return null;
    }

    private void uploadLogFile(final String logUploadId, final File logFile) throws AgentException {
        try {
            // Upload the log to S3 key FleetId/ComputeName/<game-session-id>.zip (when game session ID present) or
            // FleetId/ComputeName/<process-id>.zip (when game session ID is not present)
            final String logFileKey = String.format(S3_FILE_KEY_FORMAT, fleetId, computeName, logUploadId);
            s3FileUploader.uploadFile(gameSessionLogBucket, logFileKey, logFile, false);
        } catch (Exception e) {
            log.error("Unable to upload GameSession log file at path: {}", logFile.getAbsolutePath(), e);
            throw e;
        }
    }

    private void deleteLogFile(final File logFile) throws IOException {
        try {
            log.info("Deleting GameSession log zip file after successful S3 upload: {}", logFile.getAbsolutePath());
            Files.deleteIfExists(logFile.toPath());
        }  catch (IOException e) {
            log.error("Unable to delete GameSession log zip file: {}", logFile.getAbsolutePath(), e);
            throw e;
        }
    }
}
