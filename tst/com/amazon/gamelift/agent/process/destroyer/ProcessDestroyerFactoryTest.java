/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.destroyer;

import com.amazon.gamelift.agent.model.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ProcessDestroyerFactoryTest {
    @ParameterizedTest
    @ValueSource(strings = {"WIN_2012", "WINDOWS_2016", "WINDOWS_2019", "WINDOWS_2022"})
    public void GIVEN_windowsOs_WHEN_getProcessDestroyer_THEN_returnsWindowsProcessDestroyer(String os) {
        // GIVEN
        OperatingSystem operatingSystem = OperatingSystem.fromString(os);
        // WHEN
        ProcessDestroyer processDestroyer = ProcessDestroyerFactory.getProcessDestroyer(operatingSystem);
        // THEN
        assertTrue(processDestroyer instanceof WindowsProcessDestroyer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AMAZON_LINUX_2", "AMAZON_LINUX_2023"})
    public void GIVEN_linuxOs_WHEN_getProcessDestroyer_THEN_returnsLinuxProcessDestroyer(String os) {
        // GIVEN
        OperatingSystem operatingSystem = OperatingSystem.AMAZON_LINUX_2023;
        // WHEN
        ProcessDestroyer processDestroyer = ProcessDestroyerFactory.getProcessDestroyer(operatingSystem);
        // THEN
        assertTrue(processDestroyer instanceof LinuxProcessDestroyer);
    }

    @Test
    public void GIVEN_unsupportedOs_WHEN_getProcessDestroyer_THEN_throwsException() {
        // GIVEN
        OperatingSystem operatingSystem = OperatingSystem.INVALID;
        // WHEN
        assertThrows(IllegalArgumentException.class, () -> ProcessDestroyerFactory.getProcessDestroyer(operatingSystem));
        // THEN - Exception
    }
}
