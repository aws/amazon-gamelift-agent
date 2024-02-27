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
    public static final ProcessDestroyer getProcessDestroyer(final OperatingSystem operatingSystem) {
        switch(operatingSystem) {
            case AMAZON_LINUX_2:
            case AMAZON_LINUX_2023:
                return new LinuxProcessDestroyer(operatingSystem);
            case WIN_2012:
            case WINDOWS_2016:
            case WINDOWS_2019:
            case WINDOWS_2022:
                return new WindowsProcessDestroyer(operatingSystem);
            case INVALID:
            default:
                throw new IllegalArgumentException("Failed to find underlying process destroyer for OS "
                        + operatingSystem);
        }
    }
}
