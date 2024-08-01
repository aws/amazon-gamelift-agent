/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * NotReadyException
 */
public class NotReadyException extends AgentException {

    /**
     * Creates NotReadyException with message and throwable
     */
    public NotReadyException(final String message, final Throwable exception) {
        super(message, exception, true);
    }

    /**
     * Creates NotReadyException with message
     */
    public NotReadyException(final String message) {
        super(message, true);
    }

    /**
     * Creates NotReadyException
     */
    public NotReadyException() {
        super(true);
    }
}
