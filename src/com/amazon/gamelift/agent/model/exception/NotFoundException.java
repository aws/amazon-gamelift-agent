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
    public NotFoundException(String message, Throwable exception) {
        super(message, exception, false);
    }

    /**
     * Throws NotFoundException with message
     */
    public NotFoundException(String message) {
        super(message, false);
    }
}
