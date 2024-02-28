/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

import lombok.Getter;

/**
 * Base exception type for all exceptions in the GameLiftAgent, which allows us to identify
 * which exceptions are retryable & which aren't
 */
public class AgentException extends Exception {

    @Getter
    private boolean isRetryable = true;

    protected AgentException(String message, Throwable exception, boolean isRetryable) {
        super(message, exception);
        this.isRetryable = isRetryable;
    }

    protected AgentException(String message, boolean isRetryable) {
        super(message);
        this.isRetryable = isRetryable;
    }

    protected AgentException(boolean isRetryable) {
        super();
        this.isRetryable = isRetryable;
    }
}
