/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import com.amazon.gamelift.agent.module.ConfigModule;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named
public class UploadGameSessionLogsCallableFactory {

    private final String gameSessionLogBucket;
    private final String fleetId;
    private final String computeName;
    private final S3FileUploader s3FileUploader;
    private final GameSessionLogFileHelper gameSessionLogFileHelper;

    /**
     * Constructor for UploadGameSessionLogsCallableFactory
     * @param fleetId
     * @param computeName
     */
    @Inject
    public UploadGameSessionLogsCallableFactory(@Named(ConfigModule.GAME_SESSION_LOG_BUCKET) final String gsLogBucket,
                                                @Named(ConfigModule.FLEET_ID) final String fleetId,
                                                @Named(ConfigModule.COMPUTE_NAME) final String computeName,
                                                final S3FileUploader s3FileUploader,
                                                final GameSessionLogFileHelper gameSessionLogFileHelper) {
        this.gameSessionLogBucket = gsLogBucket;
        this.fleetId = fleetId;
        this.computeName = computeName;
        this.s3FileUploader = s3FileUploader;
        this.gameSessionLogFileHelper = gameSessionLogFileHelper;
    }

    /**
     * Creates and returns a UploadGameSessionLogsCallable
     * @param processUUID
     * @param launchPath
     * @return
     */
    public UploadGameSessionLogsCallable newUploadGameSessionLogsCallable(final String processUUID,
                                                                          final String launchPath,
                                                                          final List<String> logPaths,
                                                                          final String gameSessionId) {
        // Collect the game session logs
        final GameSessionLogsCollector gameSessionLogsCollector =
                new GameSessionLogsCollector(fleetId, computeName, processUUID, launchPath, gameSessionLogFileHelper);
        // Create a Callable to attempt uploading GameSession logs to S3
        return new UploadGameSessionLogsCallable(gameSessionLogBucket, fleetId, computeName, processUUID, logPaths,
                gameSessionId, s3FileUploader, gameSessionLogsCollector);
    }
}
