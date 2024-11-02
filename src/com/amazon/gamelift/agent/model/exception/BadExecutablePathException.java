/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * BadExecutablePathException
 */
public class BadExecutablePathException extends AgentException {
    /**
     * Creates BadExecutablePathException with message and exception
     */
    public BadExecutablePathException(final String message, final Throwable exception) {
        super(message, exception, false);
    }

    /**
     * Creates BadExecutablePathException with message
     */
    public BadExecutablePathException(final String message) {
        super(message, false);
    }
}
