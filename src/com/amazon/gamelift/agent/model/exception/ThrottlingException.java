/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * ThrottlingException
 */
public class ThrottlingException extends AgentException {

    /**
     * Creates ThrottlingException with message and throwable
     */
    public ThrottlingException(final String message, final Throwable exception) {
        super(message, exception, true);
    }

    /**
     * Creates ThrottlingException with message
     */
    public ThrottlingException(final String message) {
        super(message, true);
    }

    /**
     * Creates ThrottlingException
     */
    public ThrottlingException() {
        super(true);
    }
}
