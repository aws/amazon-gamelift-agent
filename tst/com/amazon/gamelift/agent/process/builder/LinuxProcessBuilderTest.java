/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.amazon.gamelift.agent.model.GameProcessConfiguration;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.exception.BadExecutablePathException;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LinuxProcessBuilderTest {

    private static final GameProcessConfiguration PROCESS_CONFIG = GameProcessConfiguration.builder()
            .concurrentExecutions(1)
            .launchPath("testCommand")
            .parameters("--parameter1 --parameter2")
            .build();
    private static final OperatingSystem OPERATING_SYSTEM = OperatingSystem.DEFAULT_OS;

    @Mock private ProcessBuilder mockProcessBuilder;
    @Mock private Process mockProcess;
    @Mock private Map<String, String> mockEnvironmentVariableMap;

    private LinuxProcessBuilderWrapper linuxProcessBuilderWrapper;
    private LinuxProcessBuilderWrapper spyLinuxProcessBuilderWrapper;

    @BeforeEach
    public void setup() {

        linuxProcessBuilderWrapper = new LinuxProcessBuilderWrapper(PROCESS_CONFIG, OPERATING_SYSTEM,
                mockProcessBuilder);
        spyLinuxProcessBuilderWrapper = spy(linuxProcessBuilderWrapper);
    }

    @Test
    public void GIVEN_validProcessConfigurationWithParameters_WHEN_start_THEN_returnsProcessUuid() throws IOException, BadExecutablePathException {
        // GIVEN
        when(spyLinuxProcessBuilderWrapper.verifyLaunchFileExists()).thenReturn(true);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);

        // WHEN
        Process process = spyLinuxProcessBuilderWrapper.buildProcess(mockEnvironmentVariableMap);

        // THEN
        assertEquals(process, mockProcess);
        verify(mockProcessBuilder).environment();
        verify(mockProcessBuilder).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        verify(mockProcessBuilder).redirectError(ProcessBuilder.Redirect.DISCARD);
    }

    @Test
    public void GIVEN_ioExceptionOnStart_WHEN_start_THEN_returnsProcessUuid() throws IOException {
        // GIVEN
        when(spyLinuxProcessBuilderWrapper.verifyLaunchFileExists()).thenReturn(true);
        when(mockProcessBuilder.start()).thenThrow(new IOException("unit-test"));

        // WHEN
        assertThrows(RuntimeException.class, () ->
                spyLinuxProcessBuilderWrapper.buildProcess(mockEnvironmentVariableMap));
    }

    @Test
    public void GIVEN_processConfigurationWithInvalidPath_WHEN_buildProcess_THEN_throwException() {
        // BadExecutablePathException is thrown since "testCommand" file specified for launchPath doesn't exist
        assertThrows(BadExecutablePathException.class, () ->
                linuxProcessBuilderWrapper.buildProcess(mockEnvironmentVariableMap));
    }
}
