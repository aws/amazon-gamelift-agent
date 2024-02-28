/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazonaws.util.EC2MetadataUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Optional;

@Slf4j
public class TerminationNoticeReader {
    private static final String TERMINATION_NOTICE_PATH = "/latest/meta-data/spot/termination-time";
    private static final String MANUAL_TERMINATION_FILE_LOCATION = SystemUtils.IS_OS_WINDOWS
            ? "C:\\GameLift\\Agent\\termination.txt"
            : "/local/GameLift/Agent/termination.txt";

    /**
     * Constructor for TerminationNoticeReader
     */
    @Inject
    public TerminationNoticeReader() {
    }

    /**
     * This method calls the EC2 metadata service to verify if a spot instance termination notice as been issued.
     * See http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-interruptions.html
     *
     * @return The parsed termination Instant, or an empty optional if the termination notice has not been set
     */
    public Optional<Instant> getTerminationNoticeFromEC2Metadata() {
        return parseInstant(EC2MetadataUtils.getData(TERMINATION_NOTICE_PATH));
    }

    /**
     * This method tries to read a local file. It is intended to be used as a manual trigger of the spot instance
     * termination notice. Expected format of the timestamp is YYYY-MM-DD'T'HH:mm:ss'Z' i.e. 2015-01-05T18:02:00Z
     *
     * @return The parsed termination Instant, or an empty optional if the termination notice has not been set
     */
    public Optional<Instant> getTerminationNoticeFromLocalFile() {
        return parseInstant(getTimestampFromLocalFile());
    }

    private Optional<Instant> parseInstant(final String timestamp) {
        if (StringUtils.isBlank(timestamp)) {
            log.debug("Termination notice timestamp is empty/null. This may be proper behavior (404).");
            return Optional.empty();
        }

        try {
            return Optional.of(Instant.parse(timestamp));
        } catch (final DateTimeException e) {
            log.warn("Could not parse timestamp from {}", timestamp, e);
            return Optional.empty();
        }
    }

    private String getTimestampFromLocalFile() {
        try {
            return Files.readString(Path.of(MANUAL_TERMINATION_FILE_LOCATION), StandardCharsets.UTF_8);
        } catch (final IOException | SecurityException | OutOfMemoryError e) {
            // Use debug logs to avoid polluting logs in production - these exceptions can occur frequently since the
            // file will only exist if the instance is being terminated.
            log.debug("Manual termination notice was not found or could not be read at '{}'",
                      MANUAL_TERMINATION_FILE_LOCATION, e);
            return null;
        }
    }
}
