/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

/**
 * MalformedRequestException
 */
public class MalformedRequestException extends AgentException {

    /**
     * Creates MalformedRequestException with message and throwable
     */
    public MalformedRequestException(String message, Throwable exception) {
        super(message, exception, false);
    }
}
