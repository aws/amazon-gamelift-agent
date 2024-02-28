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
    public UnauthorizedException(String message, Throwable exception) {
        super(message, exception, false);
    }

    /**
     * Creates UnauthorizedException with message
     */
    public UnauthorizedException(String message) {
        super(message, false);
    }
}
