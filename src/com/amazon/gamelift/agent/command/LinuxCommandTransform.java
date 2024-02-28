/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.command;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * LinuxCommandTransform is used to generate a process launch command for Linux operating system.
 */
public class LinuxCommandTransform implements CommandTransform {

    /**
     * {@inheritDoc}
     *
     * Setsid to force the new  process into its own session ID (SID) and process group ID (PGID).
     * This allows us to kill process trees more efficiently by using the PGID.
     */
    @Override
    public List<String> getFullCommandFromConfig(final GameProcessConfiguration processConfiguration) {
        final List<String> commandComponentList = new ArrayList<>();

        commandComponentList.add("setsid");

        commandComponentList.add(processConfiguration.getLaunchPath());
        commandComponentList.addAll(processConfiguration.getParameters());

        return commandComponentList;
    }
}
