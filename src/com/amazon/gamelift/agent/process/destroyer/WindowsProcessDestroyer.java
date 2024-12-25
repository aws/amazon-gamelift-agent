/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.destroyer;

import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.OperatingSystemFamily;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class WindowsProcessDestroyer implements ProcessDestroyer {
    private final OperatingSystem operatingSystem;

    /**
     * Constructor for WindowsProcessDestroyer
     * @param operatingSystem
     */
    @Inject
    public WindowsProcessDestroyer(final OperatingSystem operatingSystem) {
        if (!OperatingSystemFamily.WINDOWS.equals(operatingSystem.getOperatingSystemFamily())) {
            //Creation validation. This class should only be used for Windows-based OS
            throw new IllegalArgumentException("Attempted to create Windows process for non Windows-based OS. Found "
                    + operatingSystem);
        }
        this.operatingSystem = operatingSystem;
    }

    @Override
    public void destroyProcess(final Process internalProcess) {
        try {
            // Destroy all sub-processes first, but be sure to use .descendents() not .children() because .descendants()
            // includes all processes created by the root or by children of the root.
            // children() only includes direct children of the root process.
            // This should be done before destroying the root process to avoid race conditions with the OS.
            internalProcess.descendants().forEach(processHandle -> {
                log.info("Destroying sub-process with PID {}", processHandle.pid());
                processHandle.destroyForcibly();
            });

            // Destroy the root process. This should be the "powershell" process on Windows.
            log.info("Destroying parent process with PID {}", internalProcess.pid());
            internalProcess.destroyForcibly();
        } catch (final Exception e) {
            log.error("Failure terminating process: {}", internalProcess.pid());
        }
    }
}
