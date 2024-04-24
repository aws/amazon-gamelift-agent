/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

public class RealSystemEnvironmentProvider implements SystemEnvironmentProvider {

    /**
     * Get environment variable
     * @return
     */
    @Override
    public String getenv(final String env) {
        return System.getenv(env);
    }
}
