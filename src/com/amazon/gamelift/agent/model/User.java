/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import lombok.Getter;

@Getter
public enum User {
    // TODO:: GLIFT-20129 - Remove this class entirely once Windows launch commands do not rely on it
    UserServer("gl-user-server");

    private final String userName;

    User(final String userName) {
        this.userName = userName;
    }
}
