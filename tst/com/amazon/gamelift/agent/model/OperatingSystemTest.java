/*
  * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
  */
package com.amazon.gamelift.agent.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OperatingSystemTest {

    @Test
    public void GIVEN_OSStrings_WHEN_fromString_THEN_correctValuesReturned() {
        assertSame(OperatingSystem.WIN_2012, OperatingSystem.fromString("WIN_2012"));
        assertSame(OperatingSystem.WINDOWS_2019, OperatingSystem.fromString("WINDOWS_2019"));
        assertSame(OperatingSystem.AMAZON_LINUX_2, OperatingSystem.fromString("AMAZON_LINUX_2"));
        assertSame(OperatingSystem.AMAZON_LINUX_2023, OperatingSystem.fromString("AMAZON_LINUX_2023"));
        assertSame(OperatingSystem.WIN_2012, OperatingSystem.fromString("win_2012"));
        assertSame(OperatingSystem.INVALID, OperatingSystem.fromString("BURRITO_2022"));
        assertSame(OperatingSystem.INVALID, OperatingSystem.fromString(null));
    }

    @Test
    public void GIVEN_defaultOS_WHEN_getDefaultOS_THEN_correctOSReturned() {
        // GIVEN/WHEN
        OperatingSystem os = OperatingSystem.DEFAULT_OS;

        // THEN
        assertNotNull(os);
        assertEquals(os.name(), OperatingSystem.AMAZON_LINUX_2.name());
    }

    @Test
    public void GIVEN_differentOSTypes_WHEN_getOperatingSystemFamily_THEN_correctOperatingSystemFamilyReturned() {
        assertEquals(OperatingSystem.AMAZON_LINUX_2.getOperatingSystemFamily().getOsFamilyName(), "Unix");
        assertEquals(OperatingSystem.AMAZON_LINUX_2023.getOperatingSystemFamily().getOsFamilyName(), "Unix");
        assertEquals(OperatingSystem.WIN_2012.getOperatingSystemFamily().getOsFamilyName(), "Windows");
        assertEquals(OperatingSystem.WINDOWS_2019.getOperatingSystemFamily().getOsFamilyName(), "Windows");
    }

    @Test
    public void GIVEN_windows_WHEN_isLinux_THEN_returnsFalse() {
        // GIVEN
        OperatingSystem os = OperatingSystem.fromString("WINDOWS_2019");

        // WHEN/THEN
        assertFalse(os.isLinux());
    }

    @Test
    public void GIVEN_amazonLinux2_WHEN_isLinux_THEN_returnsTrue() {
        // GIVEN
        OperatingSystem os = OperatingSystem.fromString("AMAZON_LINUX_2");

        // WHEN/THEN
        assertTrue(os.isLinux());
    }
}