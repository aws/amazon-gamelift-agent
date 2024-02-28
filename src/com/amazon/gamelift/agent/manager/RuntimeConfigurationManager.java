/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.RuntimeConfiguration;

public interface RuntimeConfigurationManager {
    /**
     * Get RuntimeConfiguration
     * @return
     */
    RuntimeConfiguration getRuntimeConfiguration();
}
