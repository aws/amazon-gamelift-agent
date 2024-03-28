/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.destroyer;

import com.amazon.gamelift.agent.command.CommandTransform;
import com.amazon.gamelift.agent.command.LinuxCommandTransform;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.OperatingSystemFamily;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Slf4j
@Singleton
public class LinuxProcessDestroyer implements ProcessDestroyer {
    private final CommandTransform commandTransform;
    private final OperatingSystem operatingSystem;
    private final ProcessBuilder processBuilder;

    /**
     * Constructor for LinuxProcessDestroyer
     * @param operatingSystem
     */
    @Inject
    public LinuxProcessDestroyer(final OperatingSystem operatingSystem) {
        if (!OperatingSystemFamily.LINUX.equals(operatingSystem.getOperatingSystemFamily())) {
            //Creation validation. This class should only be used for Linux-based OS
            throw new IllegalArgumentException("Attempted to create Linux process for non Linux-based OS. Found "
                    + operatingSystem);
        }
        this.operatingSystem = operatingSystem;
        this.commandTransform = new LinuxCommandTransform();
        this.processBuilder = new ProcessBuilder();
    }

    /**
     * Test constructor for LinuxProcessDestroyer
     * @param operatingSystem
     */
    @VisibleForTesting
    public LinuxProcessDestroyer(final OperatingSystem operatingSystem,
                                 final ProcessBuilder processBuilder) {
        if (!OperatingSystemFamily.LINUX.equals(operatingSystem.getOperatingSystemFamily())) {
            //Creation validation. This class should only be used for Linux-based OS
            throw new IllegalArgumentException("Attempted to create Linux process for non Linux-based OS. Found "
                    + operatingSystem);
        }
        this.operatingSystem = operatingSystem;
        this.commandTransform = new LinuxCommandTransform();
        this.processBuilder = processBuilder;
    }

    @Override
    public void destroyProcess(final Process internalProcess) {
        final String processGroupId = Long.toString(internalProcess.pid());
        // Use the kill command to forcibly terminate processes with the given Process Group ID (PGID).
        // "-9" indicates to forcibly terminate the process.
        // "-$PGID" indicates to destroy the entire PGID
        // Example command: setsid kill -9 -11111
        processBuilder.command(
                ImmutableList.of("setsid", "kill", "-9", String.format("-%s", processGroupId)));
        try {
            processBuilder.start().waitFor();
        } catch (IOException | InterruptedException e) {
            String errorMessage =
                    String.format("Failed to destroy process with PGID %s", processGroupId);
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
