/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.manager.FleetRoleCredentialsConfigurationManager;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class S3FileUploaderTest {

    private static final String TEST_LOG_PATH = "tst/resources/test_log";
    private static final String BUCKET_NAME = RandomStringUtils.randomAlphanumeric(8);
    private static final String FILE_KEY = RandomStringUtils.randomAlphanumeric(12);
    private static final String FILE_NAME = RandomStringUtils.randomAlphanumeric(4);
    private static final AWSCredentialsProvider CREDENTIALS = new AWSStaticCredentialsProvider(
            new BasicSessionCredentials("access", "secret", "token"));

    @Mock private File mockFileToUpload;
    @Mock private AmazonS3 mockAmazonS3;
    @Mock private FleetRoleCredentialsConfigurationManager mockCredManager;

    private S3FileUploader uploader;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        uploader = Mockito.spy(new S3FileUploader(Regions.US_WEST_2.getName(), mockCredManager));
        when(mockCredManager.getFleetRoleCredentials()).thenReturn(CREDENTIALS);
        doReturn(mockAmazonS3).when(uploader).getS3Client(any());
    }

    @Test
    public void GIVEN_noException_WHEN_uploadFile_THEN_doesNotRetry() throws AgentException {
        // WHEN
        uploader.uploadFile(BUCKET_NAME, FILE_KEY, mockFileToUpload, false);

        // THEN
        verify(mockCredManager).getFleetRoleCredentials();
        verify(uploader).attemptPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, mockFileToUpload);
        verify(mockAmazonS3).putObject(any(PutObjectRequest.class));
    }

    @Test
    public void GIVEN_shouldZipFile_WHEN_uploadFile_THEN_zipsBeforeUpload() throws AgentException, IOException {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN - grab a local file to test with
            final File testFile = new File(TEST_LOG_PATH);

            // WHEN
            uploader.uploadFile(BUCKET_NAME, FILE_KEY, testFile, true);

            // THEN
            verify(mockCredManager).getFleetRoleCredentials();
            verify(uploader).attemptZipAndPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, testFile);
            verify(mockAmazonS3).putObject(any(PutObjectRequest.class));
            mockedFiles.verify(() -> Files.deleteIfExists(any()));
        }
    }

    @Test
    public void GIVEN_nonRetryableException_WHEN_uploadFile_THEN_doesNotRetry() throws AgentException {
        // GIVEN
        Mockito.doThrow(new InvalidRequestException("not-retryable")).when(uploader)
                .attemptPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, mockFileToUpload);

        // WHEN
        assertThrows(AgentException.class,
                () -> uploader.uploadFile(BUCKET_NAME, FILE_KEY, mockFileToUpload, false));

        // THEN
        verify(mockCredManager).getFleetRoleCredentials();
        verify(uploader).attemptPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, mockFileToUpload);
    }

    @Test
    public void GIVEN_retryableException_WHEN_uploadFile_THEN_doesRetry() throws AgentException {
        // GIVEN
        Mockito.doThrow(new InternalServiceException()).when(uploader)
                .attemptPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, mockFileToUpload);

        // WHEN
        assertThrows(AgentException.class,
                () -> uploader.uploadFile(BUCKET_NAME, FILE_KEY, mockFileToUpload, false));

        // THEN - Retries 3 times
        verify(mockCredManager).getFleetRoleCredentials();
        verify(uploader, times(3)).attemptPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, mockFileToUpload);
    }

    @Test
    public void GIVEN_retryableExceptionThenSuccess_WHEN_uploadFile_THEN_onlyRetriesOnce() throws AgentException {
        // GIVEN
        doThrow(new InternalServiceException())
                .doNothing().when(uploader)
                .attemptPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, mockFileToUpload);

        // WHEN
        uploader.uploadFile(BUCKET_NAME, FILE_KEY, mockFileToUpload, false);

        // THEN - Retries once, then success on the 2nd run
        verify(mockCredManager).getFleetRoleCredentials();
        verify(uploader, times(2)).attemptPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, mockFileToUpload);
    }

    @Test
    public void GIVEN_failToCreateTempFile_WHEN_uploadFile_THEN_doesRetry() throws IOException, AgentException {
        try (MockedStatic<File> mockedFile = mockStatic(File.class);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN
            when(mockFileToUpload.getName()).thenReturn(FILE_NAME);
            mockedFile.when(() -> File.createTempFile(anyString(), anyString())).thenThrow(IOException.class);

            // WHEN
            assertThrows(RuntimeException.class,
                    () -> uploader.uploadFile(BUCKET_NAME, FILE_KEY, mockFileToUpload, true));

            // THEN
            verify(mockCredManager).getFleetRoleCredentials();
            verify(uploader, times(3)).attemptZipAndPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, mockFileToUpload);
            mockedFile.verify(() -> File.createTempFile(anyString(), anyString()), times(3));
            mockedFiles.verifyNoInteractions();
        }
    }

    @Test
    public void GIVEN_failToCopyToFile_WHEN_uploadFile_THEN_doesRetryAndDeleteTempFiles()
            throws IOException, AgentException {
        try (MockedStatic<IOUtils> mockedIOUtils = mockStatic(IOUtils.class);
                MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // GIVEN - grab a local file to test with
            final File testFile = new File(TEST_LOG_PATH);
            mockedIOUtils.when(() -> IOUtils.copy(any(InputStream.class), any(OutputStream.class)))
                    .thenThrow(IOException.class);
            log.info("##### {}", testFile.getName());

            // WHEN
            assertThrows(RuntimeException.class,
                    () -> uploader.uploadFile(BUCKET_NAME, FILE_KEY, testFile, true));

            // THEN
            verify(mockCredManager).getFleetRoleCredentials();
            verify(uploader, times(3)).attemptZipAndPutObject(mockAmazonS3, BUCKET_NAME, FILE_KEY, testFile);
            mockedIOUtils.verify(() -> IOUtils.copy(any(InputStream.class), any(OutputStream.class)), times(3));
            mockedFiles.verify(() -> Files.deleteIfExists(any()), times(3));
        }
    }
}
