/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.cli;

/**
 * Exception to indicate that the user has requested the help cli option and no other args were evaluated.
 */
public class HelpRequestedException extends RuntimeException {
    /**
     * Constructor for HelpRequestedException
     * @param message
     */
    public HelpRequestedException(String message) {
        super(message);
    }
}
