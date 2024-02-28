/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

/**
 * Enumeration for Compute status.
 *
 * In GameLiftAgent, this enumeration is used to express the state a Compute is in as follows:
 *  From State                         Transition                                      To State
 * (GameLiftAgent Starts)          --[GameLiftAgent starts]--&gt;                    (Initializing)
 * (Initializing)                   --[GameLiftAgent starting server process]--&gt;   (Activating)
 * (Activating)                     --[At least one GameProcess ready]--&gt;           (Active)
 * (Active)                         --[StartComputeTermination message received]--&gt; (Terminating)
 */

public enum ComputeStatus {
    Initializing,                   // Initial state on GameLiftAgent launch.
    Activating,                     // GameProcess launch has initiated; waiting on first process to start.
    Active,                         // At least one GameProcess is ready.
    Terminating,                    // GameLiftAgent received StartComputeTermination message.
    Terminated,                     // GameLiftAgent exiting after StartComputeTermination message.
    Interrupted                     // GameLiftAgent received spot termination notice and will attempt to terminate.
}
