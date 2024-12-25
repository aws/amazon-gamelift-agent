/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model.constants;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 *  The values of this enum specify which source to use for credentials when uploading logs.
 *  The selection is provided via CLI input, with a default of fleet-role.
 */
@Getter
@RequiredArgsConstructor
public enum LogCredentials {
    FLEET_ROLE("fleet-role"),
    DEFAULT_PROVIDER_CHAIN("default-provider-chain");

    @NonNull
    private final String value;
}
