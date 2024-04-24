/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * InternalServiceException
 */
public class InternalServiceException extends AgentException {

    /**
     * Creates InternalServiceException with message and throwable
     */
    public InternalServiceException(final String message, final Throwable exception) {
        super(message, exception, true);
    }

    /**
     * Creates InternalServiceException with message
     */
    public InternalServiceException(final String message) {
        super(message, true);
    }

    /**
     * Creates InternalServiceException
     */
    public InternalServiceException() {
        super(true);
    }
}
