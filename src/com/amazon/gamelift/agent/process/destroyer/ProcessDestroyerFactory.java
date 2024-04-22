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
        return switch (operatingSystem) {
            case AMAZON_LINUX_2, AMAZON_LINUX_2023 -> new LinuxProcessDestroyer(operatingSystem);
            case WIN_2012, WINDOWS_2016, WINDOWS_2019, WINDOWS_2022 -> new WindowsProcessDestroyer(operatingSystem);
            default -> throw new IllegalArgumentException("Failed to find underlying process destroyer for OS "
                    + operatingSystem);
        };
    }
}
