/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.amazon.gamelift.agent.model.exception.BadExecutablePathException;

import java.util.Map;

public interface ProcessBuilderWrapper {

    /**
     * Creates an underlying process with the environment variables set. Returns a java.lang Process.
     * @param environmentVariables
     * @return
     */
    Process buildProcess(Map<String, String> environmentVariables) throws BadExecutablePathException;
}
