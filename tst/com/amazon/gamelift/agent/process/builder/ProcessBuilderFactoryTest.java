/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.OperatingSystemFamily;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ProcessBuilderFactoryTest {

    @Mock
    private GameProcessConfiguration mockGameProcessConfiguration;

    @Test
    public void GIVEN_os_WHEN_getProcessBuilder_THEN_returnsCorrectProcessBuilder() {
        // GIVEN
        given(mockGameProcessConfiguration.getLaunchPath()).willReturn("./unitTest");
        given(mockGameProcessConfiguration.getParameters()).willReturn(Arrays.asList("unit", "test"));

        for (final OperatingSystem operatingSystem : OperatingSystem.values()) {
            try {
                ProcessBuilderWrapper processBuilderWrapper =
                        ProcessBuilderFactory.getProcessBuilder(mockGameProcessConfiguration, operatingSystem);
                if (OperatingSystemFamily.WINDOWS.equals(operatingSystem.getOperatingSystemFamily())) {
                    // Current implementation of WindowsProcessBuilderWrapper & WindowsProcess requires dlls to exist
                    // on Compute. This condition will currently not be hit.
                    assertInstanceOf(WindowsProcessBuilderWrapper.class, processBuilderWrapper);
                } else if (OperatingSystemFamily.LINUX.equals(operatingSystem.getOperatingSystemFamily())) {
                    assertInstanceOf(LinuxProcessBuilderWrapper.class, processBuilderWrapper);
                } else {
                    throw new RuntimeException(String.format("Found operating system family "
                            + "without ProcessBuilderWrapper configured: %s",
                            operatingSystem.getOperatingSystemFamily()));
                }
            } catch (IllegalArgumentException e) {
                assertEquals(OperatingSystem.INVALID, operatingSystem);
            } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                // Current implementation of windows code requires the dlls to be present for JNA to even
                // create the class. For most dev-cases, developers are running unit tests on linux boxes
                // (including pipeline builds).
                assertEquals(OperatingSystemFamily.WINDOWS, operatingSystem.getOperatingSystemFamily());
            }
        }
    }
}
