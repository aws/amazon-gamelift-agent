/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UploadGameSessionLogsCallableTest {

    @Mock private S3FileUploader mockS3FileUploader;
    @Mock private GameSessionLogsCollector mockGameSessionLogsCollector;
    @Mock private File mockFile;
    @Mock private Path mockPath;

    private static final String GS_LOG_BUCKET = "game-session-log-bucket";
    private static final String FLEET_ID = "fleet-123abc";
    private static final String COMPUTE_NAME = "compute-name";
    private static final String PROCESS_ID = RandomStringUtils.randomAlphanumeric(10);
    private static final String GAME_SESSION_ID = "TEST_GAME_SESSION_ID";
    private static final List<String> LOG_PATHS = ImmutableList.of("/local/game/appLog", "/local/game/otherLog");
    private static final String S3_KEY = FLEET_ID + "/" + COMPUTE_NAME + "/" + GAME_SESSION_ID + ".zip";

    private UploadGameSessionLogsCallable callable;

    @BeforeEach
    public void setup() throws IOException, InternalServiceException, InterruptedException, InvalidRequestException {
        callable = new UploadGameSessionLogsCallable(GS_LOG_BUCKET, FLEET_ID, COMPUTE_NAME, PROCESS_ID, LOG_PATHS,
                GAME_SESSION_ID, mockS3FileUploader, mockGameSessionLogsCollector);
    }

    @Test
    public void GIVEN_noGameSessionLogBucket_WHEN_call_THEN_skipUpload() throws Exception {
        callable = new UploadGameSessionLogsCallable(null, FLEET_ID, COMPUTE_NAME, PROCESS_ID,
                LOG_PATHS, GAME_SESSION_ID, mockS3FileUploader, mockGameSessionLogsCollector);
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // WHEN
            callable.call();

            // THEN
            verifyNoInteractions(mockGameSessionLogsCollector);
            verifyNoInteractions(mockS3FileUploader);
            mockedFiles.verifyNoInteractions();
        }
    }

    @Test
    public void GIVEN_exceptionThrownCollectingLogs_WHEN_call_THEN_doNotThrow() throws Exception {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN
            when(mockGameSessionLogsCollector.collectGameSessionLogs(LOG_PATHS, GAME_SESSION_ID))
                    .thenThrow(new RuntimeException("bad-log-collection"));

            // WHEN
            callable.call();

            // THEN
            verify(mockGameSessionLogsCollector).collectGameSessionLogs(LOG_PATHS, GAME_SESSION_ID);
            verifyNoInteractions(mockS3FileUploader);
            mockedFiles.verifyNoInteractions();
            verifyNoMoreInteractions(mockGameSessionLogsCollector);
        }
    }

    @Test
    public void GIVEN_exceptionThrownUploadingFile_WHEN_call_THEN_doNotThrow() throws Exception {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN
            when(mockGameSessionLogsCollector.collectGameSessionLogs(LOG_PATHS, GAME_SESSION_ID)).thenReturn(mockFile);
            doThrow(new RuntimeException("failed-to-upload")).when(mockS3FileUploader)
                    .uploadFile(GS_LOG_BUCKET, S3_KEY, mockFile, false);

            // WHEN
            callable.call();

            // THEN
            verify(mockGameSessionLogsCollector).collectGameSessionLogs(LOG_PATHS, GAME_SESSION_ID);
            verify(mockS3FileUploader).uploadFile(GS_LOG_BUCKET, S3_KEY, mockFile, false);
            mockedFiles.verifyNoInteractions();
            verifyNoMoreInteractions(mockGameSessionLogsCollector);
        }
    }

    @Test
    public void GIVEN_nullGameSession_WHEN_call_THEN_processIdUsedForLogs() throws Exception {
        final UploadGameSessionLogsCallable nullGameSessionCallable =
                new UploadGameSessionLogsCallable(GS_LOG_BUCKET, FLEET_ID, COMPUTE_NAME, PROCESS_ID, LOG_PATHS,
                        null, mockS3FileUploader, mockGameSessionLogsCollector);
        // When GameSession is null then we expect the process ID to be used instead
        final String s3Key = FLEET_ID + "/" + COMPUTE_NAME + "/" + PROCESS_ID + ".zip";

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN
            when(mockGameSessionLogsCollector.collectGameSessionLogs(LOG_PATHS, PROCESS_ID)).thenReturn(mockFile);

            // WHEN
            nullGameSessionCallable.call();

            // THEN
            verify(mockGameSessionLogsCollector).collectGameSessionLogs(LOG_PATHS, PROCESS_ID);
            verify(mockS3FileUploader).uploadFile(GS_LOG_BUCKET, s3Key, mockFile, false);
        }
    }
}
