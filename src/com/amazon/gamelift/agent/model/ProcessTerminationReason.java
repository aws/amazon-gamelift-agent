/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import com.amazonaws.services.gamelift.model.EventCode;
import lombok.RequiredArgsConstructor;

/**
 * Representation of the reason why a Game Process gets terminated.
 * Each reason may have an associate EventCode from the GameLift SDK model, which will be reported when sending
 * a notification about the process termination in order to create an associated Fleet Event.
 */
@RequiredArgsConstructor
public enum ProcessTerminationReason {
    SERVER_PROCESS_CRASHED(EventCode.SERVER_PROCESS_CRASHED),
    SERVER_PROCESS_FORCE_TERMINATED(EventCode.SERVER_PROCESS_FORCE_TERMINATED),
    SERVER_PROCESS_INVALID_PATH(EventCode.SERVER_PROCESS_INVALID_PATH),
    SERVER_PROCESS_PROCESS_EXIT_TIMEOUT(EventCode.SERVER_PROCESS_PROCESS_EXIT_TIMEOUT),
    SERVER_PROCESS_PROCESS_READY_TIMEOUT(EventCode.SERVER_PROCESS_PROCESS_READY_TIMEOUT),
    SERVER_PROCESS_SDK_INITIALIZATION_TIMEOUT(EventCode.SERVER_PROCESS_SDK_INITIALIZATION_TIMEOUT),
    SERVER_PROCESS_TERMINATED_UNHEALTHY(EventCode.SERVER_PROCESS_TERMINATED_UNHEALTHY),
    COMPUTE_SHUTTING_DOWN(null),
    CUSTOMER_INITIATED(null),
    NORMAL_TERMINATION(null);

    private final EventCode eventCode;

    /**
     * Retrieves a stringified version of the associated GameLift event code to send for the termination,
     * or returns null if there's no associated event that needs to be reported for the termination reason.
     *
     * @return the associated event code string, or null if no event code is specified
     */
    public String getEventCode() {
        return eventCode == null ? null : eventCode.toString();
    }
}
