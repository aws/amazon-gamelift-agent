/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * NotFinishedException
 */
public class NotFinishedException extends AgentException {

    /**
     * Creates NotFinishedException with message and throwable
     */
    public NotFinishedException(final String message, final Throwable exception) {
        super(message, exception, true);
    }

    /**
     * Creates NotFinishedException with message
     */
    public NotFinishedException(final String message) {
        super(message, true);
    }
}
