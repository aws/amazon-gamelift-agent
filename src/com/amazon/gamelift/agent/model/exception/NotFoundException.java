/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * NotFoundException
 */
public class NotFoundException extends AgentException {

    /**
     * Throws NotFoundException with message and throwable
     */
    public NotFoundException(final String message, final Throwable exception) {
        super(message, exception, true);
    }

    /**
     * Throws NotFoundException with message
     */
    public NotFoundException(final String message) {
        super(message, true);
    }
}
