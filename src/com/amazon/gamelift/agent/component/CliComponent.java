/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.component;

import com.amazon.gamelift.agent.cli.AgentCliParser;
import com.amazon.gamelift.agent.module.CliModule;
import dagger.Component;

/**
 * Interface for CLI dagger dependency injection.
 */
@Component(modules = CliModule.class)
public interface CliComponent {
    /**
     * Builds a CLI Parser
     * @return
     */
    AgentCliParser buildCliParser();
}
