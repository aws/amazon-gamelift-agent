/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OperatingSystemFamilyTest {
    @Test
    public void GIVEN_Windows_WHEN_fromOsFamilyName_thenWindowsFamilyReturned() {
        // WHEN
        final OperatingSystemFamily family = OperatingSystemFamily.fromOsFamilyName("Windows");

        // THEN
        assertEquals(family, OperatingSystemFamily.WINDOWS);
    }

    @Test
    public void GIVEN_Unix_WHEN_fromOsFamilyName_thenLinuxFamilyReturned() {
        // WHEN
        final OperatingSystemFamily family = OperatingSystemFamily.fromOsFamilyName("Unix");

        // THEN
        assertEquals(family, OperatingSystemFamily.LINUX);
    }

    @Test
    public void GIVEN_Invalid_WHEN_fromOsFamilyName_thenInvalidFamilyReturned() {
        // WHEN/THEN
        assertThrows(IllegalArgumentException.class, () -> OperatingSystemFamily.fromOsFamilyName("tacoTuesday"));
    }
}