/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.OperatingSystem;

public class ProcessBuilderFactory {

    /**
     * Return ProcessBuilderWrapper based on OS
     * @param processConfiguration
     * @param operatingSystem
     * @return
     */
    public static final ProcessBuilderWrapper getProcessBuilder(final GameProcessConfiguration processConfiguration,
                                                                final OperatingSystem operatingSystem) {
        switch(operatingSystem) {
            case AMAZON_LINUX_2:
            case AMAZON_LINUX_2023:
                return new LinuxProcessBuilderWrapper(processConfiguration, operatingSystem);
            case WIN_2012:
            case WINDOWS_2016:
            case WINDOWS_2019:
                return new WindowsProcessBuilderWrapper(processConfiguration, operatingSystem);
            case INVALID:
            default:
                throw new IllegalArgumentException("Failed to find underlying process builder for OS "
                        + operatingSystem);
        }
    }
}
