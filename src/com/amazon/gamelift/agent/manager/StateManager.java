/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import com.amazon.gamelift.agent.model.ComputeStatus;
import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *  This class should be either injected as a singleton or plumbed through calls as necessary from the injected
 *  singleton object. The class is responsible for maintaining state data for the GameLiftAgent.
 */
@Singleton
public class StateManager {
    /**
     * ComputeStatus should always progress from Initializing to Terminated. Updates will be visible across threads.
     */
    @Getter(AccessLevel.NONE)
    private final Object statusLock = new Object();

    @GuardedBy("statusLock")
    @VisibleForTesting
    @Setter(value = AccessLevel.PACKAGE, onMethod = @__(@Synchronized("statusLock")))
    private ComputeStatus computeStatus = ComputeStatus.Initializing;

    /**
     * Constructor for StateManager
     */
    @Inject
    public StateManager() {

    }

    /**
     * Get Compute status (locks on statusLock)
     * @return
     */
    @Synchronized("statusLock")
    public ComputeStatus getComputeStatus() {
        return computeStatus;
    }

    /**
     * Switch status to Activating (locks on status lock)
     */
    @Synchronized("statusLock")
    public void reportComputeActivating() {
        if (computeStatus == ComputeStatus.Initializing) {
            computeStatus = ComputeStatus.Activating;
        }
    }

    /**
     * Switch status to Active (locks on status lock)
     */
    @Synchronized("statusLock")
    public void reportComputeActive() {
        if (computeStatus == ComputeStatus.Activating || computeStatus == ComputeStatus.Initializing) {
            computeStatus = ComputeStatus.Active;
        }
    }

    /**
     * Start terminating the Compute if not already being terminated
     */
    @Synchronized("statusLock")
    public void reportComputeTerminating() {
        if (!isComputeTerminatingOrTerminated()) {
            computeStatus = ComputeStatus.Terminating;
        }
    }

    /**
     * The Compute has received a Spot interruption notice
     */
    @Synchronized("statusLock")
    public void reportComputeInterrupted() {
        if (computeStatus != ComputeStatus.Terminated && computeStatus != ComputeStatus.Interrupted) {
            computeStatus = ComputeStatus.Interrupted;
        }
    }

    /**
     * Switch status to Terminated (locks on statusLock)
     */
    @Synchronized("statusLock")
    public void reportComputeTerminated() {
        computeStatus = ComputeStatus.Terminated;
    }

    /**
     * Returns true if status is terminating/terminated/interrupted
     * @return
     */
    @Synchronized("statusLock")
    public boolean isComputeTerminatingOrTerminated() {
        return computeStatus == ComputeStatus.Terminating
                || computeStatus == ComputeStatus.Terminated
                || computeStatus == ComputeStatus.Interrupted;
    }

    /**
     * Returns true if status is initializing
     * @return
     */
    @Synchronized("statusLock")
    public boolean isComputeInitializing() {
        return computeStatus == ComputeStatus.Initializing;
    }
}
