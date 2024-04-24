/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.amazon.gamelift.agent.utils.RetryHelper;
import com.amazon.gamelift.agent.manager.FleetRoleCredentialsConfigurationManager;
import com.amazon.gamelift.agent.model.exception.InternalServiceException;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.module.ConfigModule;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * Helper class for handling file uploads to S3 through PutObject requests.
 */
@Slf4j
@Singleton
public class S3FileUploader {

    private static final String GZ_SUFFIX = ".gz";

    private final String region;
    private final FleetRoleCredentialsConfigurationManager fleetRoleCredentialsManager;

    /**
     * Constructor for S3FileUploader
     */
    @Inject
    public S3FileUploader(@Named(ConfigModule.REGION) final String region,
                          final FleetRoleCredentialsConfigurationManager fleetRoleCredentialsManager) {
        this.region = region;
        this.fleetRoleCredentialsManager = fleetRoleCredentialsManager;
    }

    /**
     * Build an S3 client based on input credentials and attempt to upload the File with RetryHelper.
     */
    public void uploadFile(final String bucketName,
                           final String fileKey,
                           final File logFile,
                           final boolean shouldZipFile) throws AgentException {
        log.info("Preparing to upload file {} to bucket {} under key {}", logFile.getName(), bucketName, fileKey);
        final AWSCredentialsProvider fleetRoleCredentials = fleetRoleCredentialsManager.getFleetRoleCredentials();
        final AmazonS3 amazonS3 = getS3Client(fleetRoleCredentials);
        if (shouldZipFile) {
            RetryHelper.runRetryable(() -> attemptZipAndPutObject(amazonS3, bucketName, fileKey, logFile));
        } else {
            RetryHelper.runRetryable(() -> attemptPutObject(amazonS3, bucketName, fileKey, logFile));
        }
    }

    /**
     * Upload with PutObject directly from the File.
     */
    @VisibleForTesting
    Void attemptPutObject(final AmazonS3 amazonS3,
                          final String bucketName,
                          final String fileKey,
                          final File logFile) throws AgentException {
        try {
            final PutObjectRequest request = new PutObjectRequest(bucketName, fileKey, logFile);
            final PutObjectResult result = amazonS3.putObject(request);
            log.info("File uploaded successfully: {}", result);
        } catch (final AmazonS3Exception e) {
            throw new InternalServiceException(e.getMessage());
        }
        // Retryable will retry unless something is returned
        return null;
    }

    /**
     * Zip the file using a GZIPInputStream then attempt the upload.
     *
     * NOTE: When using an InputStream with PutObject, the content-length is required, which involves reading the File
     * into memory.  In order to avoid OOM issues with larger files, a temp file is used and deleted.
     */
    @VisibleForTesting
    Void attemptZipAndPutObject(final AmazonS3 amazonS3,
                                final String bucketName,
                                final String fileKey,
                                final File logFile) throws IOException, AgentException {
        final File tempZipFile = File.createTempFile(logFile.getName(), GZ_SUFFIX);
        try (InputStream inputStream = new FileInputStream(logFile);
             GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(tempZipFile))) {
            // Copy the FileInputStream to the GZIP/FileOutputStream
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
            // Perform the S3 PutObject operation, append zip suffix to fileKey
            attemptPutObject(amazonS3, bucketName, fileKey + GZ_SUFFIX, tempZipFile);
        } catch (final IOException e) {
            log.error("Failed to zip file: {}", logFile.getName());
            throw e;
        } finally {
            Files.deleteIfExists(tempZipFile.toPath());
        }
        // Retryable will retry unless something is returned
        return null;
    }

    @VisibleForTesting
    AmazonS3 getS3Client(final AWSCredentialsProvider awsCredentialsProvider) {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentialsProvider)
                .withRegion(region)
                .build();
    }
}
