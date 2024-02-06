/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.gamelift.agent.utils;

public interface SystemEnvironmentProvider {
    /**
     * Get environment variable
     * @return
     */
    String getenv(String env);
}
