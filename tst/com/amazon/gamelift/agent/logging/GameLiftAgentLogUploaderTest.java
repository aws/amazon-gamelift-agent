/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.websocket.AgentWebSocket;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazon.gamelift.agent.manager.ExecutorServiceManager;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith({MockitoExtension.class})
public class GameLiftAgentLogUploaderTest {

    private static final String GAMELIFT_AGENT_BUCKET = RandomStringUtils.randomAlphanumeric(8);
    private static final String FLEET_ID = "fleet-" + RandomStringUtils.randomAlphanumeric(8);
    private static final String COMPUTE_NAME = RandomStringUtils.randomAlphanumeric(6);
    private static final String FILE_NAME_1 = "01-01-2023-00-00-00.zip";
    private static final String FILE_NAME_2 = "01-01-2023-01-00-00.zip";
    private static final String FILE_KEY_1 = FLEET_ID + "/" + COMPUTE_NAME + "/" + FILE_NAME_1;
    private static final String FILE_KEY_2 = FLEET_ID + "/" + COMPUTE_NAME + "/" + FILE_NAME_2;
    private static final String FILE_NAME_UNZIPPED = "gameliftagent.log";

    // Instant of epoch millis 1672535400000L is equal to 2023-01-01-01-10 (yyyy-MM-dd-hh-mm) - used for testing the
    // format when uploading the active GameLiftAgent log such as during shut down.
    private static final Instant STATIC_INSTANT = Instant.ofEpochMilli(1672535400000L);
    private static final String FILE_NAME_UNZIPPED_TIMESTAMPED = FILE_NAME_UNZIPPED + ".2023-01-01-01-10";
    private static final String FILE_KEY_UNZIPPED = FLEET_ID + "/" + COMPUTE_NAME + "/" + FILE_NAME_UNZIPPED_TIMESTAMPED;

    @Mock private File mockLogFile1;
    @Mock private File mockLogFile2;
    @Mock private File mockLogFileUnzipped;
    @Mock private Path mockLogPath1;
    @Mock private Path mockLogPath2;
    @Mock private Path mockLogPathUnzipped;
    @Mock private LogFileHelper mockLogFileHelper;
    @Mock private S3FileUploader mockS3FileUploader;
    @Mock private AgentWebSocket mockAgentWebSocket;
    @Mock private ExecutorServiceManager mockExecutorServiceManager;
    @Mock private ScheduledExecutorService mockGameLiftAgentLogUploaderExecutorService;

    @Captor private ArgumentCaptor<Long> longCaptor;

    private GameLiftAgentLogUploader logUploader;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        logUploader = new GameLiftAgentLogUploader(mockLogFileHelper, mockS3FileUploader,
                GAMELIFT_AGENT_BUCKET, FLEET_ID, COMPUTE_NAME, mockExecutorServiceManager, mockGameLiftAgentLogUploaderExecutorService);
    }

    @Test
    public void GIVEN_firstTimeCalling_WHEN_start_THEN_schedulesTaskWithValidTime() {
        // WHEN
        logUploader.start();

        // THEN
        verify(mockGameLiftAgentLogUploaderExecutorService).scheduleWithFixedDelay(
                any(Runnable.class), longCaptor.capture(), eq(3600L), eq(TimeUnit.SECONDS));
        LocalTime capturedInitialDelayTime = LocalTime.now().plusSeconds(longCaptor.getValue());
        assertTrue(capturedInitialDelayTime.getMinute() >= 5, "Expected initial executor delay to start after the 5th minute of the hour");
        assertTrue(capturedInitialDelayTime.getMinute() <= 20, "Expected initial executor delay to start before the 20th minute of the hour");
    }

    @Test
    public void GIVEN_callStartTwice_WHEN_start_THEN_onlySchedulesOnce() {
        // WHEN
        logUploader.start();
        logUploader.start();

        // THEN - Executor service is only scheduled once
        verify(mockGameLiftAgentLogUploaderExecutorService).scheduleWithFixedDelay(
                any(Runnable.class), anyLong(), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void GIVEN_noErrorsWhenUploading_WHEN_runScheduledLogUpload_THEN_uploadAndDeleteLogs()
            throws AgentException {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN
            setupFileMocks();
            when(mockLogFileHelper.isArchivedFile(any())).thenReturn(true);

            // WHEN
            logUploader.runScheduledLogUpload();

            // THEN
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_1, mockLogFile1, false);
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_2, mockLogFile2, false);
            mockedFiles.verify(() -> Files.deleteIfExists(mockLogPath1));
            mockedFiles.verify(() -> Files.deleteIfExists(mockLogPath2));
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void GIVEN_errorWhenUploading_WHEN_runScheduledLogUpload_THEN_swallowsExceptionAndContinues()
            throws AgentException {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN
            setupFileMocks();
            when(mockLogFileHelper.isArchivedFile(any())).thenReturn(true);
            Mockito.doThrow(InternalServiceException.class).when(mockS3FileUploader)
                    .uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_1, mockLogFile1, false);

            // WHEN
            logUploader.runScheduledLogUpload();

            // THEN
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_1, mockLogFile1, false);
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_2, mockLogFile2, false);
            mockedFiles.verify(() -> Files.deleteIfExists(mockLogPath2));
            mockedFiles.verifyNoMoreInteractions();
        }
    }

    @Test
    public void GIVEN_errorWhenDeleting_WHEN_runScheduledLogUpload_THEN_swallowsExceptionAndContinues()
            throws AgentException {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN
            setupFileMocks();
            when(mockLogFileHelper.isArchivedFile(any())).thenReturn(true);
            mockedFiles.when(() -> Files.deleteIfExists(mockLogPath1)).thenThrow(IOException.class);

            // WHEN
            logUploader.runScheduledLogUpload();

            // THEN
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_1, mockLogFile1, false);
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_2, mockLogFile2, false);
            mockedFiles.verify(() -> Files.deleteIfExists(mockLogPath1));
            mockedFiles.verify(() -> Files.deleteIfExists(mockLogPath2));
        }
    }

    @Test
    public void GIVEN_noErrorsWhenUploading_WHEN_shutdownAndUploadLogs_THEN_uploadsLogsAndStopsThread()
            throws AgentException {
        // GIVEN
        try (MockedStatic<LocalDateTime> mockedDate = mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            // GIVEN
            LocalDateTime mockDate = LocalDateTime.ofInstant(STATIC_INSTANT, ZoneOffset.UTC);
            mockedDate.when(() -> LocalDateTime.ofInstant(any(), any())).thenReturn(mockDate);

            when(mockLogFileHelper.getArchivedGameLiftAgentLogs()).thenReturn(ImmutableList.of(mockLogFile1));
            when(mockLogFileHelper.getActiveGameLiftAgentLog()).thenReturn(Optional.of(mockLogFileUnzipped));

            when(mockLogFile1.getName()).thenReturn(FILE_NAME_1);
            when(mockLogFileHelper.isArchivedFile(eq(mockLogFile1))).thenReturn(true);

            when(mockLogFileUnzipped.getName()).thenReturn(FILE_NAME_UNZIPPED);
            when(mockLogFileHelper.isArchivedFile(eq(mockLogFileUnzipped))).thenReturn(false);

            // WHEN
            logUploader.start();
            logUploader.shutdownAndUploadLogs();

            // THEN
            verify(mockExecutorServiceManager).shutdownScheduledThreadPoolExecutorServiceByName(GameLiftAgentLogUploader.class.getSimpleName());
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_1, mockLogFile1, false);
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_UNZIPPED, mockLogFileUnzipped, true);
        }
    }

    @Test
    public void GIVEN_errorsWhenUploading_WHEN_shutdownAndUploadLogs_THEN_swallowsExceptionsAndContinues()
            throws AgentException {
        try (MockedStatic<LocalDateTime> mockedDate = mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            // GIVEN
            LocalDateTime mockDate = LocalDateTime.ofInstant(STATIC_INSTANT, ZoneOffset.UTC);
            mockedDate.when(() -> LocalDateTime.ofInstant(any(), any())).thenReturn(mockDate);

            when(mockLogFileHelper.getArchivedGameLiftAgentLogs()).thenReturn(ImmutableList.of(mockLogFile1));
            when(mockLogFileHelper.getActiveGameLiftAgentLog()).thenReturn(Optional.of(mockLogFileUnzipped));

            when(mockLogFile1.getName()).thenReturn(FILE_NAME_1);
            when(mockLogFile1.toPath()).thenReturn(mockLogPath1);
            when(mockLogFileHelper.isArchivedFile(eq(mockLogFile1))).thenReturn(true);

            when(mockLogFileUnzipped.getName()).thenReturn(FILE_NAME_UNZIPPED);
            when(mockLogFileUnzipped.toPath()).thenReturn(mockLogPathUnzipped);
            when(mockLogFileHelper.isArchivedFile(eq(mockLogFileUnzipped))).thenReturn(false);

            doThrow(InternalServiceException.class).when(mockS3FileUploader)
                    .uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_1, mockLogFile1, true);

            // WHEN
            logUploader.start();
            logUploader.shutdownAndUploadLogs();

            // THEN
            verify(mockExecutorServiceManager).shutdownScheduledThreadPoolExecutorServiceByName(GameLiftAgentLogUploader.class.getSimpleName());
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_1, mockLogFile1, false);
            verify(mockS3FileUploader).uploadFile(GAMELIFT_AGENT_BUCKET, FILE_KEY_UNZIPPED, mockLogFileUnzipped, true);
        }
    }

    @Test
    public void GIVEN_uploaderNotStarted_WHEN_shutdownAndUploadLogs_THEN_doesNothing() {
        // WHEN
        logUploader.shutdownAndUploadLogs();

        // THEN
        verifyNoInteractions(mockExecutorServiceManager, mockAgentWebSocket, mockS3FileUploader);
    }

    private void setupFileMocks() {
        when(mockLogFileHelper.getArchivedGameLiftAgentLogs())
                .thenReturn(ImmutableList.of(mockLogFile1, mockLogFile2));
        when(mockLogFile1.getName()).thenReturn(FILE_NAME_1);
        when(mockLogFile2.getName()).thenReturn(FILE_NAME_2);
        when(mockLogFile1.toPath()).thenReturn(mockLogPath1);
        when(mockLogFile2.toPath()).thenReturn(mockLogPath2);
    }
}
