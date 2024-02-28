/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import lombok.Value;

@Value
public class CredentialPair {
    private String username;
    private String password;
}
