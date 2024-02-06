/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AgentLogFileFilterTest {

    @Mock private File testFile;

    @InjectMocks private AgentLogFileFilter filter;

    @Test
    public void GIVEN_notAFile_WHEN_accept_THEN_returnsFalse() {
        when(testFile.getName()).thenReturn("gameliftagent.log");
        when(testFile.isFile()).thenReturn(false);
        assertFalse(filter.accept(testFile));
    }

    @Test
    public void GIVEN_badFileName_WHEN_accept_THEN_returnsFalse() {
        when(testFile.isFile()).thenReturn(true);
        when(testFile.isDirectory()).thenReturn(false);
        when(testFile.getName()).thenReturn("somelogfile.log");
        assertFalse(filter.accept(testFile));
    }

    @Test
    public void GIVEN_directory_WHEN_accept_THEN_returnsFalse() {
        when(testFile.isFile()).thenReturn(true);
        when(testFile.isDirectory()).thenReturn(true);
        when(testFile.getName()).thenReturn("gameliftagent.log");
        assertFalse(filter.accept(testFile));
    }
}
