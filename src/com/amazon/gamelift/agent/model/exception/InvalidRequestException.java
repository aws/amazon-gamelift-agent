/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * InvalidRequestException
 */
public class InvalidRequestException extends AgentException {

    /**
     * Creates InvalidRequestException with message and throwable
     */
    public InvalidRequestException(String message, Throwable exception) {
        super(message, exception, false);
    }

    /**
     * Creates InvalidRequestException with message
     */
    public InvalidRequestException(String message) {
        super(message, false);
    }
}
