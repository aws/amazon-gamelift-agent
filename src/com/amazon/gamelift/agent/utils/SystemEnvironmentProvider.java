package com.amazon.gamelift.agent.utils;

public interface SystemEnvironmentProvider {
    /**
     * Get environment variable
     * @return
     */
    String getenv(String env);
}
