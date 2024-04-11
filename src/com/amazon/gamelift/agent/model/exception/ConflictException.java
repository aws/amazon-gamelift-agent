/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * ConflictException
 */
public class ConflictException extends AgentException {

    /**
     * Creates ConflictException with message and throwable
     */
    public ConflictException(final String message, final Throwable exception) {
        super(message, exception, false);
    }
}
