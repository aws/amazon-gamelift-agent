/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.destroyer;

public interface ProcessDestroyer {
    /**
     * Terminates an underlying process and all its child processes.
     * @param internalProcess
     * @return
     */
    void destroyProcess(Process internalProcess);
}
