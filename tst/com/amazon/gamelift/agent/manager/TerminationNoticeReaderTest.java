/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazonaws.SdkClientException;
import com.amazonaws.util.EC2MetadataUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

public class TerminationNoticeReaderTest {
    private final TerminationNoticeReader terminationNoticeReader = new TerminationNoticeReader();

    @Test
    public void GIVEN_ec2MetadataNotice_WHEN_getTerminationNoticeFromEC2Metadata_THEN_returnsInstant() {
        try (MockedStatic<EC2MetadataUtils> ec2MetadataUtils = mockStatic(EC2MetadataUtils.class)) {
            final Instant instant = Instant.now();
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(eq("/latest/meta-data/instance-life-cycle"))).thenReturn("spot");
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(eq("/latest/meta-data/spot/termination-time"))).thenReturn(instant.toString());

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromEC2Metadata();
            assertEquals(instant, metadata.get());
        }
    }

    @Test
    public void GIVEN_onDemandInstanceType_WHEN_getTerminationNoticeFromEC2Metadata_THEN_returnsEmpty() {
        try (MockedStatic<EC2MetadataUtils> ec2MetadataUtils = mockStatic(EC2MetadataUtils.class)) {
            final Instant instant = Instant.now();
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(eq("/latest/meta-data/instance-life-cycle"))).thenReturn("on-demand");
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(eq("/latest/meta-data/spot/termination-time"))).thenReturn(instant.toString());

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromEC2Metadata();
            assertTrue(metadata.isEmpty());
        }
    }

    @Test
    public void GIVEN_noMetadataServiceAvailable_WHEN_getTerminationNoticeFromEC2Metadata_THEN_returnsEmpty() {
        try (MockedStatic<EC2MetadataUtils> ec2MetadataUtils = mockStatic(EC2MetadataUtils.class)) {
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(any())).thenThrow(SdkClientException.class);

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromEC2Metadata();
            assertTrue(metadata.isEmpty());
        }
    }

    @Test
    public void GIVEN_emptyMetadataNotice_WHEN_getTerminationNoticeFromEC2Metadata_THEN_returnsEmpty() {
        try (MockedStatic<EC2MetadataUtils> ec2MetadataUtils = mockStatic(EC2MetadataUtils.class)) {
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(eq("/latest/meta-data/instance-life-cycle"))).thenReturn("spot");
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(eq("/latest/meta-data/spot/termination-time"))).thenReturn("");

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromEC2Metadata();
            assertTrue(metadata.isEmpty());
        }
    }

    @Test
    public void GIVEN_badMetadataNotice_WHEN_getTerminationNoticeFromEC2Metadata_THEN_returnsEmpty() {
        try (MockedStatic<EC2MetadataUtils> ec2MetadataUtils = mockStatic(EC2MetadataUtils.class)) {
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(eq("/latest/meta-data/instance-life-cycle"))).thenReturn("spot");
            ec2MetadataUtils.when(() -> EC2MetadataUtils.getData(eq("/latest/meta-data/spot/termination-time"))).thenReturn("(☞ﾟヮﾟ)☞ ┻━┻");

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromEC2Metadata();
            assertTrue(metadata.isEmpty());
        }
    }

    @Test
    public void GIVEN_manualMetadataNotice_WHEN_getTerminationNoticeFromLocalFile_THEN_returnsInstant() {
        try (MockedStatic<Files> ec2MetadataUtils = mockStatic(Files.class)) {
            final Instant instant = Instant.now();
            ec2MetadataUtils.when(() -> Files.readString(any(), any())).thenReturn(instant.toString());

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromLocalFile();
            assertEquals(instant, metadata.get());
        }
    }

    @Test
    public void GIVEN_emptyMetadataNotice_WHEN_getTerminationNoticeFromLocalFile_THEN_returnsEmpty() {
        try (MockedStatic<Files> ec2MetadataUtils = mockStatic(Files.class)) {
            ec2MetadataUtils.when(() -> Files.readString(any(), any())).thenReturn("");

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromLocalFile();
            assertTrue(metadata.isEmpty());
        }
    }

    @Test
    public void GIVEN_badMetadataNotice_WHEN_getTerminationNoticeFromLocalFile_THEN_returnsEmpty() {
        try (MockedStatic<Files> ec2MetadataUtils = mockStatic(Files.class)) {
            ec2MetadataUtils.when(() -> Files.readString(any(), any())).thenReturn("(☞ﾟヮﾟ)☞ ┻━┻");

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromLocalFile();
            assertTrue(metadata.isEmpty());
        }
    }

    @Test
    public void GIVEN_ioException_WHEN_getTerminationNoticeFromLocalFile_THEN_returnsEmpty() {
        try (MockedStatic<Files> ec2MetadataUtils = mockStatic(Files.class)) {
            ec2MetadataUtils.when(() -> Files.readString(any(), any())).thenThrow(IOException.class);

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromLocalFile();
            assertTrue(metadata.isEmpty());
        }
    }

    @Test
    public void GIVEN_securityException_WHEN_getTerminationNoticeFromLocalFile_THEN_returnsEmpty() {
        try (MockedStatic<Files> ec2MetadataUtils = mockStatic(Files.class)) {
            ec2MetadataUtils.when(() -> Files.readString(any(), any())).thenThrow(SecurityException.class);

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromLocalFile();
            assertTrue(metadata.isEmpty());
        }
    }

    @Test
    public void GIVEN_outOfMemoryError_WHEN_getTerminationNoticeFromLocalFile_THEN_returnsEmpty() {
        try (MockedStatic<Files> ec2MetadataUtils = mockStatic(Files.class)) {
            ec2MetadataUtils.when(() -> Files.readString(any(), any())).thenThrow(OutOfMemoryError.class);

            final Optional<Instant> metadata = terminationNoticeReader.getTerminationNoticeFromLocalFile();
            assertTrue(metadata.isEmpty());
        }
    }
}
