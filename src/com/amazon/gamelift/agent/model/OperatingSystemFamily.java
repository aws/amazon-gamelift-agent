/**
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import lombok.Getter;

/**
 *  Specifies Amazon GameLift's supported Operating System families. Used to classify the operating system on which
 *  GameLiftAgent is running.
 *
 */
public enum OperatingSystemFamily {
    WINDOWS("Windows"),
    LINUX("Unix"),
    INVALID("Invalid");

    @Getter private final String osFamilyName;

    OperatingSystemFamily(String osFamilyName) {
        this.osFamilyName = osFamilyName;
    }

    /**
     * Convert a CloudFormation parameter name to an OperatingSystemFamily.
     * @param osFamilyName The name to convert
     * @return a {@link OperatingSystemFamily}
     */
    public static OperatingSystemFamily fromOsFamilyName(String osFamilyName) {
        for (OperatingSystemFamily family : values()) {
            if (family.osFamilyName.equals(osFamilyName)) {
                return family;
            }
        }
        throw new IllegalArgumentException(String.format("No family found for the value %s", osFamilyName));
    }
}
