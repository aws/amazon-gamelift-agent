/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.OperatingSystemFamily;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ProcessBuilderFactoryTest {

    @Mock
    private GameProcessConfiguration mockGameProcessConfiguration;

    private static final String USER = "TEST_USER";

    @Test
    public void GIVEN_os_WHEN_getProcessBuilder_THEN_returnsCorrectProcessBuilder() {
        // GIVEN
        given(mockGameProcessConfiguration.getLaunchPath()).willReturn("./unitTest");
        given(mockGameProcessConfiguration.getParameters()).willReturn(Arrays.asList("unit", "test"));

        for (OperatingSystem operatingSystem : OperatingSystem.values()) {
            try {
                ProcessBuilderWrapper processBuilderWrapper =
                        ProcessBuilderFactory.getProcessBuilder(mockGameProcessConfiguration, operatingSystem);
                if (OperatingSystemFamily.WINDOWS.equals(operatingSystem.getOperatingSystemFamily())) {
                    // Current implementation of WindowsProcessBuilderWrapper & WindowsProcess requires dlls to exist
                    // on Compute. This condition will currently not be hit.
                    assertTrue(processBuilderWrapper instanceof WindowsProcessBuilderWrapper);
                } else if (OperatingSystemFamily.LINUX.equals(operatingSystem.getOperatingSystemFamily())) {
                    assertTrue(processBuilderWrapper instanceof LinuxProcessBuilderWrapper);
                } else {
                    throw new RuntimeException(String.format("Found operating system family "
                            + "without ProcessBuilderWrapper configured: %s",
                            operatingSystem.getOperatingSystemFamily()));
                }
            } catch (IllegalArgumentException e) {
                assertEquals(OperatingSystem.INVALID, operatingSystem);
            } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                // Current implementation of windows code requires the dlls to be present for JNA to even
                // create the class. For most dev-cases, we're running unit tests on linux boxes
                // (including pipeline builds).
                assertEquals(OperatingSystemFamily.WINDOWS, operatingSystem.getOperatingSystemFamily());
            }
        }
    }
}
