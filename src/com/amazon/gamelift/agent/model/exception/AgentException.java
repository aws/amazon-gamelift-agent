/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.exception;

import lombok.Getter;

/**
 * Base exception type for all exceptions in the GameLiftAgent, which allows us to identify
 * which exceptions are retryable & which aren't
 */
@Getter
public class AgentException extends Exception {

    private final boolean isRetryable;

    protected AgentException(final String message, final Throwable exception, final boolean isRetryable) {
        super(message, exception);
        this.isRetryable = isRetryable;
    }

    protected AgentException(final String message, final boolean isRetryable) {
        super(message);
        this.isRetryable = isRetryable;
    }

    protected AgentException(final boolean isRetryable) {
        super();
        this.isRetryable = isRetryable;
    }
}
