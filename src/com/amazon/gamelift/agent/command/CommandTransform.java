/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.command;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;

import java.util.List;

/**
 * Interface for CommandTransform. Implementations configure process launch commands for various operating systems
 */
public interface CommandTransform {

    /**
     * Resolve the commandLine to execute the delegated command.
     *
     * @param processConfiguration
     */
    List<String> getFullCommandFromConfig(GameProcessConfiguration processConfiguration);

}
