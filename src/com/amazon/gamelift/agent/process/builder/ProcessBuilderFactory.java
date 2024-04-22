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
    public static ProcessBuilderWrapper getProcessBuilder(final GameProcessConfiguration processConfiguration,
                                                          final OperatingSystem operatingSystem) {
        return switch (operatingSystem) {
            case AMAZON_LINUX_2, AMAZON_LINUX_2023 ->
                    new LinuxProcessBuilderWrapper(processConfiguration, operatingSystem);
            case WIN_2012, WINDOWS_2016, WINDOWS_2019, WINDOWS_2022 ->
                    new WindowsProcessBuilderWrapper(processConfiguration, operatingSystem);
            default -> throw new IllegalArgumentException("Failed to find underlying process builder for OS "
                    + operatingSystem);
        };
    }
}
