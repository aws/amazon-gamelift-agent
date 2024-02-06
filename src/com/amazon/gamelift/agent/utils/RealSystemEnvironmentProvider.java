package com.amazon.gamelift.agent.utils;

public class RealSystemEnvironmentProvider implements SystemEnvironmentProvider {

    /**
     * Get environment variable
     * @return
     */
    @Override
    public String getenv(String env) {
        return System.getenv(env);
    }
}
