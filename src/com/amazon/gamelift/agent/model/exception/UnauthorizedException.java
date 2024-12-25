/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * UnauthorizedException
 */
public class UnauthorizedException extends AgentException {

    /**
     * Creates UnauthorizedException with message and throwable
     */
    public UnauthorizedException(final String message, final Throwable exception) {
        super(message, exception, false);
    }

    /**
     * Creates UnauthorizedException with message
     */
    public UnauthorizedException(final String message) {
        super(message, false);
    }
}
