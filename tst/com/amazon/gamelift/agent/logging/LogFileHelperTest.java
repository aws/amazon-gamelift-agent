/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogFileHelperTest {

    private final String ACTIVE_LOG_PATH = "/test/file/path/gameliftagent.log";
    private final String ZIPPED_LOG_PATH = "/test/file/path/oldlog1.log.zip";
    private final String GZIPPED_LOG_PATH = "/test/file/path/oldlog1.log.gz";
    private final File[] FILES_IN_GAMELIFT_AGENT_LOG_DIRECTORY = new File[] {
        new File(ACTIVE_LOG_PATH),
        new File(ZIPPED_LOG_PATH),
        new File(GZIPPED_LOG_PATH)
    };

    @Mock private File mockGameLiftAgentLogDirectory;
    @Mock private AgentLogFileFilter mockFileFilter;

    @InjectMocks private LogFileHelper logFileHelper;

    @Test
    public void GIVEN_directoryContainingZipAndNonZipFiles_WHEN_getArchivedGameLiftAgentLogs_THEN_onlyReturnsZipFiles() {
        when(mockGameLiftAgentLogDirectory.listFiles(mockFileFilter)).thenReturn(FILES_IN_GAMELIFT_AGENT_LOG_DIRECTORY);

        List<File> returnedFiles = logFileHelper.getArchivedGameLiftAgentLogs();

        assertEquals(2, returnedFiles.size());
        String filePath1 = returnedFiles.get(0).getAbsolutePath();
        String filePath2 = returnedFiles.get(1).getAbsolutePath();
        assertEquals(Set.of(ZIPPED_LOG_PATH, GZIPPED_LOG_PATH), Set.of(filePath1, filePath2));
    }

    @Test
    public void GIVEN_directoryContainingZipAndNonZipFiles_WHEN_getActiveGameLiftAgentLog_THEN_onlyReturnsZipFiles() {
        when(mockGameLiftAgentLogDirectory.listFiles(mockFileFilter)).thenReturn(FILES_IN_GAMELIFT_AGENT_LOG_DIRECTORY);

        Optional<File> returnedFile = logFileHelper.getActiveGameLiftAgentLog();

        assertTrue(returnedFile.isPresent());
        assertEquals(ACTIVE_LOG_PATH, returnedFile.get().getAbsolutePath());
    }

    @Test
    public void GIVEN_archivedLogFile_WHEN_isArchivedFile_THEN_returnsTrue() {
        assertTrue(logFileHelper.isArchivedFile(new File(ZIPPED_LOG_PATH)));
        assertTrue(logFileHelper.isArchivedFile(new File(GZIPPED_LOG_PATH)));
    }

    @Test
    public void GIVEN_nonArchivedLogFile_WHEN_isArchivedFile_THEN_returnsFalse() {
        assertFalse(logFileHelper.isArchivedFile(new File(ACTIVE_LOG_PATH)));
    }
}
