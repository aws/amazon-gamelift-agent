/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.component;

import com.amazon.gamelift.agent.Agent;
import com.amazon.gamelift.agent.module.ClientModule;
import com.amazon.gamelift.agent.module.ConfigModule;
import com.amazon.gamelift.agent.module.ProcessModule;
import com.amazon.gamelift.agent.module.ThreadingModule;
import dagger.Component;

import javax.inject.Singleton;

/**
 * Interface for global agent dagger dependency injection. Dagger suggests having a single component
 * used by your service at runtime.
 */
@Singleton
@Component(modules = {ConfigModule.class, ClientModule.class, ProcessModule.class, ThreadingModule.class})
public interface GameLiftAgentComponent {
    /**
     * Builds a GameLiftAgent (singleton instance)
     * @return
     */
    Agent buildGameLiftAgent();
}
