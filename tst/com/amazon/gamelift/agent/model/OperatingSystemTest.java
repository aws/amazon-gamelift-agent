/*
  * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
  */
package com.amazon.gamelift.agent.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        assertSame(OperatingSystem.WINDOWS_2022, OperatingSystem.fromString("WINDOWS_2022"));
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
        assertEquals(OperatingSystem.WINDOWS_2022.getOperatingSystemFamily().getOsFamilyName(), "Windows");
    }

    @ParameterizedTest
    @ValueSource(strings = {"WIN_2012", "WINDOWS_2016", "WINDOWS_2019", "WINDOWS_2022"})
    public void GIVEN_windows_WHEN_isLinux_THEN_returnsFalse(String desiredOS) {
        // GIVEN
        OperatingSystem os = OperatingSystem.fromString(desiredOS);

        // WHEN/THEN
        assertFalse(os.isLinux());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AMAZON_LINUX_2", "AMAZON_LINUX_2023"})
    public void GIVEN_amazonLinux2_WHEN_isLinux_THEN_returnsTrue(String desiredOS) {
        // GIVEN
        OperatingSystem os = OperatingSystem.fromString(desiredOS);

        // WHEN/THEN
        assertTrue(os.isLinux());
    }

    @Test
    public void GIVEN_windows2022_WHEN_fromOperatingSystem_THEN_correctOSReturned() {
        final String osName = "os.name";
        String previousOsName = System.getProperty(osName);
        System.setProperty(osName, OperatingSystem.WINDOWS_2022.getDisplayName());
        assertEquals(OperatingSystem.fromSystemOperatingSystem(), OperatingSystem.WINDOWS_2022);

        System.setProperty(osName, previousOsName);
    }
}