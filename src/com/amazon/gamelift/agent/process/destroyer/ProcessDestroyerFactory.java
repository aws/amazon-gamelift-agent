/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.destroyer;

import com.amazon.gamelift.agent.model.OperatingSystem;

public class ProcessDestroyerFactory {
    /**
     * Return ProcessDestroyer based on OS
     * @param operatingSystem
     * @return
     */
    public static ProcessDestroyer getProcessDestroyer(final OperatingSystem operatingSystem) {
        return switch (operatingSystem.getOperatingSystemFamily()) {
            case LINUX -> new LinuxProcessDestroyer(operatingSystem);
            case WINDOWS -> new WindowsProcessDestroyer(operatingSystem);
            default -> throw new IllegalArgumentException("Failed to find underlying process destroyer for OS "
                    + operatingSystem);
        };
    }
}
