/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent;

import java.io.IOException;
import java.util.Properties;

import com.amazon.gamelift.agent.cli.HelpRequestedException;
import com.amazon.gamelift.agent.component.DaggerCliComponent;
import com.amazon.gamelift.agent.component.DaggerGameLiftAgentComponent;
import com.amazon.gamelift.agent.model.AgentArgs;
import com.amazon.gamelift.agent.component.CliComponent;
import com.amazon.gamelift.agent.manager.LogConfigurationManager;
import com.amazon.gamelift.agent.module.ConfigModule;

/**
 * Application launcher for managing client processes.
 */
public final class Application {

    static final String JAVA_PROP_TTL_KEY = "sun.net.inetaddr.ttl";
    static final String JAVA_PROP_TTL_VALUE = "60";
    static final String JAVA_PROP_NEG_TTL_KEY = "sun.net.inetaddr.negative.ttl";
    static final String JAVA_PROP_NEG_TTL_VALUE = "1";
    static final String JAVA_PROP_NETWORK_ADDR_CACHE_TTL_KEY = "networkaddress.cache.ttl";
    static final String JAVA_PROP_NETWORK_ADDR_CACHE_TTL_VALUE = "60";
    static final String JAVA_PROP_PREFER_IPV4_STACK_KEY = "java.net.preferIPv4Stack";
    static final String JAVA_PROP_PREFER_IPV4_STACK_VALUE = "true";
    static final String JAVA_PROP_FORKJOINPOOL_PARALLELISM_KEY = "java.util.concurrent.ForkJoinPool.common.parallelism";
    static final String JAVA_PROP_FORKJOINPOOL_PARALLELISM_VALUE = "55";
    static final String JAVA_PROP_SECURITY_EGD_KEY = "java.security.egd";
    static final String JAVA_PROP_SECURITY_EGD_VALUE =  "file:/dev/./random";

    private Application() { }

    /**
     * Executable jar entry point responsible for setting up the GameLiftAgent.
     *
     * @param args CLI args
     * @throws IOException
     */
    public static void main(final String[] args) throws Exception {
        setUpJavaSystemProperties();

        final CliComponent cliComponent = DaggerCliComponent.create();

        try {
            final AgentArgs parsedArgs = cliComponent.buildCliParser().parse(args);
            LogConfigurationManager.configureLogging(parsedArgs.getAgentLogPath());

            final Agent agent = DaggerGameLiftAgentComponent.builder()
                    .configModule(new ConfigModule(parsedArgs))
                    .build()
                    .buildGameLiftAgent();

            // This should not be the normal method of termination - normal termination should be triggered by a
            // Websocket message or Spot Interruption signal which calls the ShutdownOrchestrator class. But in the
            // event that there's some sort of unexpected thread interruption, attempt to perform a clean shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> agent.shutdown()));

            // Start the GameLiftAgent and wait for the main thread to terminate
            agent.start();
            Thread.currentThread().join();
        } catch (final HelpRequestedException e) {
            // Exit gracefully if help requested
            System.exit(0);
        }
    }

    private static void setUpJavaSystemProperties() {
        Properties systemProperties = System.getProperties();
        systemProperties.put(JAVA_PROP_TTL_KEY, JAVA_PROP_TTL_VALUE);
        systemProperties.put(JAVA_PROP_NEG_TTL_KEY, JAVA_PROP_NEG_TTL_VALUE);
        systemProperties.put(JAVA_PROP_NETWORK_ADDR_CACHE_TTL_KEY, JAVA_PROP_NETWORK_ADDR_CACHE_TTL_VALUE);
        systemProperties.put(JAVA_PROP_PREFER_IPV4_STACK_KEY, JAVA_PROP_PREFER_IPV4_STACK_VALUE);
        systemProperties.put(JAVA_PROP_FORKJOINPOOL_PARALLELISM_KEY, JAVA_PROP_FORKJOINPOOL_PARALLELISM_VALUE);
        systemProperties.put(JAVA_PROP_SECURITY_EGD_KEY, JAVA_PROP_SECURITY_EGD_VALUE);
        System.setProperties(systemProperties);
    }
}
