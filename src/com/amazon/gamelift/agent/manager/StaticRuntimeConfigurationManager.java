/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.RuntimeConfiguration;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StaticRuntimeConfigurationManager implements RuntimeConfigurationManager {
    private final RuntimeConfiguration runtimeConfiguration;

    @Override
    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtimeConfiguration;
    }
}
