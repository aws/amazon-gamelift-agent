/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.constants;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 *  The values of this enum specify which source to use for credentials when creating an Amazon GameLift client.
 *  The selection is provided via CLI input, with a default of INSTANCE_PROFILE.
 */
@RequiredArgsConstructor
public enum GameLiftCredentials {
    INSTANCE_PROFILE("instance-profile"),
    ENVIRONMENT_VARIABLE("environment-variable"),
    CONTAINER("container");

    @Getter @NonNull
    private final String value;
}
