/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.gamelift.agent.model;

/**
 * An internal representation of the status of game processes
 */
public enum ProcessStatus {
    Initializing,       // The Process has been started, but has not yet connected using the GameLift SDK
    Active              // The GameLiftAgent has been notified that the Process has successfully connected with the SDK
}
