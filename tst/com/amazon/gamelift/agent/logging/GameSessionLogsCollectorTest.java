/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import com.amazon.gamelift.agent.model.ConfiguredLogPaths;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class GameSessionLogsCollectorTest {
    private static final String FLEET_ID = "fleet-123abc";
    private static final String COMPUTE_NAME = "computeName";
    private static final String PROCESS_UUID = "process-"+ RandomStringUtils.randomAlphanumeric(10);
    private static final String LAUNCH_PATH = "/local/game/TestApplicationServer";
    private static final String GAME_SESSION_ID = "gameSessionId";

    private GameSessionLogsCollector gameSessionLogsCollector;

    private static final String ROOT_PATH = "tests/";
    private static final String ROOT_PATH_DOWNLOADED = "tests/downloadedZip/";
    private static final String WHITEWATER_BOOTSTRAP_PATH = "tests/WhitewaterBootstrap/";

    private ArrayList<String> logPaths = new ArrayList<>();
    private static final List<String> invalidLogPaths = ImmutableList.of("/private/stuff.txt", "/privater/stuff.txt");

    private static final String normalLogPath = ROOT_PATH + "logs/logs.txt";
    private static final String missingLogPath = ROOT_PATH + "logs/missingFile.txt";
    private static final String emptyFolderPath = ROOT_PATH + "logs/emptyFolder/";
    private static final String nonEmptyFolderPath = ROOT_PATH + "logs/nonEmptyFolder/";
    private static final String missingFolderPath = ROOT_PATH + "logs/missingFolder";
    private static final String nestedLogPath = ROOT_PATH + "logs/logsFolder/logs/nestedLogs.txt";
    private static final String wrongPermissionPath = ROOT_PATH + "logs/logsFolder/logs/noReadPermission.txt";
    private static final String parentPath = ROOT_PATH + "logs/logsFolder/logs/";
    private static final String dmpPath = WHITEWATER_BOOTSTRAP_PATH + "launch.dmp";
    private static final String dmpPathWildcard = WHITEWATER_BOOTSTRAP_PATH + "*.dmp";
    private static final String extraLargeLogPath = ROOT_PATH + "logs/extraLargeLogs.txt";
    private static final String largeLogPath1 = ROOT_PATH + "logs/largeLogs1.txt";
    private static final String largeLogPath2 = ROOT_PATH + "logs/largeLogs2.txt";
    private static final String logReadMePath = "ReadMe.txt";

    private static final List<String> folderLogPaths = List.of(emptyFolderPath, nonEmptyFolderPath, missingFolderPath, parentPath);

    @Mock private GameSessionLogFileHelper mockGameSessionLogFileHelper;
    @Mock private ConfiguredLogPaths mockConfiguredLogPaths;

    private static final int MEGABYTE = 1024 * 1024;

    @BeforeEach
    public void setUp() throws IOException{
        cleanUp();

        logPaths = new ArrayList<>();
        logPaths.add(normalLogPath);
        logPaths.add(missingLogPath);
        logPaths.add(emptyFolderPath);
        logPaths.add(nonEmptyFolderPath);
        logPaths.add(missingFolderPath);
        logPaths.add(nestedLogPath);
        logPaths.add(wrongPermissionPath);
        logPaths.add(parentPath);
        logPaths.add(dmpPathWildcard);

        createLogFiles();

        gameSessionLogsCollector = new GameSessionLogsCollector(FLEET_ID, COMPUTE_NAME, PROCESS_UUID,
                LAUNCH_PATH, mockGameSessionLogFileHelper);
    }

    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(new File(ROOT_PATH).getCanonicalFile());
    }

    @Test
    public void GIVEN_validInput_WHEN_collectGameSessionLogs_THEN_zipFileCreatedAsExpected() throws Exception {
        // GIVEN
        when(mockGameSessionLogFileHelper.configureLogPaths(any(), any())).thenReturn(mockConfiguredLogPaths);
        when(mockConfiguredLogPaths.getValidLogPaths()).thenReturn(logPaths);
        when(mockConfiguredLogPaths.getInvalidLogPaths()).thenReturn(new ArrayList<>());

        // WHEN
        final File zipFile = gameSessionLogsCollector.collectGameSessionLogs(logPaths, GAME_SESSION_ID);

        // THEN
        assertNotNull(zipFile);
        verifyZipFileContents(zipFile, false, false);

        cleanUp();
    }

    @Test
    public void GIVEN_invalidLogPaths_WHEN_collectAndUpload_THEN_writeInvalidPathsToReadMe() throws Exception {
        // GIVEN
        when(mockGameSessionLogFileHelper.configureLogPaths(any(), any())).thenReturn(mockConfiguredLogPaths);
        when(mockConfiguredLogPaths.getValidLogPaths()).thenReturn(logPaths);
        when(mockConfiguredLogPaths.getInvalidLogPaths()).thenReturn(invalidLogPaths);

        // WHEN
        final File zipFile = gameSessionLogsCollector.collectGameSessionLogs(logPaths, GAME_SESSION_ID);

        // THEN
        assertNotNull(zipFile);
        verifyZipFileContents(zipFile, false, true);

        cleanUp();
    }

    @Test
    public void GIVEN_validPaths_WHEN_deleteGameSessionLogs_THEN_deleteFiles() {
        try (
                final MockedStatic<Files> mockedFiles = mockStatic(Files.class);
                final MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class)
        ) {
            // GIVEN
            when(mockGameSessionLogFileHelper.configureLogPaths(any(), any())).thenReturn(mockConfiguredLogPaths);
            when(mockConfiguredLogPaths.getValidLogPaths()).thenReturn(logPaths);

            // WHEN
            gameSessionLogsCollector.deleteGameSessionLogs(logPaths);

            // THEN
            for (final String logPath : logPaths) {
                File logFile;
                if (logPath.equals(dmpPathWildcard)) {
                    // The wildcard doesn't delete *.dmp, it should detect and delete launch.dmp
                    logFile = new File(dmpPath).getAbsoluteFile();
                } else {
                    logFile = new File(logPath).getAbsoluteFile();
                }
                if (folderLogPaths.contains(logPath)) {
                    mockedFileUtils.verify(() -> FileUtils.deleteDirectory(logFile));
                } else if (!logPath.equals(missingLogPath)){
                    mockedFiles.verify(() -> Files.deleteIfExists(logFile.toPath()));
                }
            }
        }
    }

    @Test
    public void GIVEN_failedDeletion_WHEN_deleteGameSessionLogs_THEN_continueToAllFiles() {
        try (
                final MockedStatic<Files> mockedFiles = mockStatic(Files.class);
                final MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class)
        ) {
            // GIVEN
            when(mockGameSessionLogFileHelper.configureLogPaths(any(), any())).thenReturn(mockConfiguredLogPaths);
            when(mockConfiguredLogPaths.getValidLogPaths()).thenReturn(logPaths);
            File normalLogFile = new File(normalLogPath).getAbsoluteFile();
            mockedFiles.when(() -> Files.deleteIfExists(normalLogFile.toPath())).thenThrow(IOException.class);

            // WHEN
            gameSessionLogsCollector.deleteGameSessionLogs(logPaths);

            // THEN
            for (final String logPath : logPaths) {
                File logFile;
                if (logPath.equals(dmpPathWildcard)) {
                    // The wildcard doesn't delete *.dmp, it should detect and delete launch.dmp
                    logFile = new File(dmpPath).getAbsoluteFile();
                } else {
                    logFile = new File(logPath).getAbsoluteFile();
                }
                if (folderLogPaths.contains(logPath)) {
                    mockedFileUtils.verify(() -> FileUtils.deleteDirectory(logFile));
                } else if (!logPath.equals(missingLogPath)){
                    mockedFiles.verify(() -> Files.deleteIfExists(logFile.toPath()));
                }
            }
        }
    }

    private void verifyZipFileContents(final File zipFile, final boolean testServiceLimits,
                                       final boolean testInvalidLogPaths) throws Exception {
        log.info("Unzipping zip file to: {}", ROOT_PATH_DOWNLOADED);
        unzip(zipFile, ROOT_PATH_DOWNLOADED);

        // Check that files either do or do not exist (as expected) for provided log paths
        for (final String logPath: logPaths) {
            File logFile = new File(ROOT_PATH_DOWNLOADED, logPath).getCanonicalFile();

            if (logPath.equals(missingLogPath)
                    || logPath.equals(missingFolderPath)
                    || logPath.equals(wrongPermissionPath)
                    || logPath.equals(extraLargeLogPath)
                    || logPath.equals(largeLogPath2)){
                assertFalse(logFile.exists());
            } else if (logPath.equals(dmpPathWildcard)){
                // This case has a log path with a wildcard - verifying expected file that matches exists in zip
                logFile = new File(ROOT_PATH_DOWNLOADED, dmpPath);
                assertTrue(logFile.exists());
            } else {
                assertTrue(logFile.exists());

                if (logPath.equals(largeLogPath1) || logPath.equals(largeLogPath2)) {
                    // Since content is a bunch of junk, verifying the file size.
                    assertEquals(MEGABYTE, logFile.length());
                    continue;
                }

                if (logPath.equals(extraLargeLogPath)) {
                    // Since content is a bunch of junk, verifying the file size.
                    assertEquals(2 * MEGABYTE, logFile.length());
                    continue;
                }

                if (!folderLogPaths.contains(logPath)){
                    assertEquals(logPath, FileUtils.readFileToString(logFile, StandardCharsets.UTF_8));
                }
            }
        }

        final File logReadMeFile = new File(ROOT_PATH_DOWNLOADED, logReadMePath);
        final String readMeToString = FileUtils.readFileToString(logReadMeFile, StandardCharsets.UTF_8);

        // List of lines / partial lines (where variable dev workspace paths exist) expected to be in ReadMe file in zip
        final List<String> expectedReadMeContentList = new ArrayList<>();
        expectedReadMeContentList.add("Game Server Logs\n");
        expectedReadMeContentList.add("1 File Collected\t\t\t\ttests/logs/logs.txt\n");
        expectedReadMeContentList.add("Error: Missing file/directory\ttests/logs/missingFile.txt\n");
        expectedReadMeContentList.add("0 File(s) Collected\t\t\t\ttests/logs/emptyFolder/\n");
        expectedReadMeContentList.add("Error: Missing file/directory\ttests/logs/missingFolder\n");
        expectedReadMeContentList.add("1 File Collected\t\t\t\ttests/logs/logsFolder/logs/nestedLogs.txt\n");
        expectedReadMeContentList.add("0 File(s) Collected\t\t\t\ttests/logs/logsFolder/logs/\n");
        expectedReadMeContentList.add(dmpPath);

        final String operatingSystem = System.getProperty("os.name").toLowerCase();
        if (!operatingSystem.contains("win")) {
            //In LINUX, this test creates a file with no read permission
            expectedReadMeContentList.add("Error: No Read Permission\ttests/logs/logsFolder/logs/noReadPermission.txt\n");
        } else {
            //This test doesn't work in Windows.
            expectedReadMeContentList.add("Error: Missing file/directory\ttests/logs/logsFolder/logs/noReadPermission.txt\n");
        }

        if (testServiceLimits){
            // Expecting to see files collected for very large files even if they will be removed afterward
            expectedReadMeContentList.add("1 File Collected\t\t\t\ttests/logs/extraLargeLogs.txt\n");
            expectedReadMeContentList.add("1 File Collected\t\t\t\ttests/logs/largeLogs1.txt\n");
            expectedReadMeContentList.add("1 File Collected\t\t\t\ttests/logs/largeLogs2.txt\n");

            // Expecting logs in excess of limit to show up as removed
            expectedReadMeContentList.add("Removed: Logs Service Limit exceeded\t/tests/logs/extraLargeLogs.txt\n");
            expectedReadMeContentList.add("Removed: Logs Service Limit exceeded\t/tests/logs/largeLogs2.txt\n");
        }

        if (testInvalidLogPaths){
            expectedReadMeContentList.add("Error: Invalid Log Path\t/private/stuff.txt\n");
            expectedReadMeContentList.add("Error: Invalid Log Path\t/privater/stuff.txt\n");
        }

        for (final String s : expectedReadMeContentList) {
            assertTrue(readMeToString.contains(s),
                    String.format("Expected ReadMe file to contain line/content %s", s));
        }
    }

    private void createLogFiles() throws IOException {
        for (final String logPath: logPaths) {
            switch(logPath) {
                case emptyFolderPath:
                    new File(emptyFolderPath).mkdir();
                    break;
                case nonEmptyFolderPath:
                    new File(nonEmptyFolderPath).mkdir();
                    new File(nonEmptyFolderPath, "logs.txt").createNewFile();
                    break;
                case wrongPermissionPath:
                    Path wrongPermissionsFilePath = Paths.get(wrongPermissionPath);
                    String operatingSystem = System.getProperty("os.name").toLowerCase();
                    //This unit test doesn't work in Windows.
                    if (!operatingSystem.contains("win"))
                    {
                        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
                        Files.createFile(wrongPermissionsFilePath, PosixFilePermissions.asFileAttribute(perms));
                    }
                    break;
                case parentPath:
                case missingLogPath:
                case missingFolderPath:
                    break;
                case dmpPathWildcard:
                    FileUtils.write(new File(dmpPath), dmpPath, StandardCharsets.UTF_8);
                    break;
                default:
                    FileUtils.write(new File(logPath), logPath, StandardCharsets.UTF_8);
                    break;
            }
        }
    }

    private void unzip(final File file, final String outputDir) throws IOException{
        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
        ZipEntry ze = zis.getNextEntry();

        while(ze != null){
            log.info("{}", ze.getName());
            File entryDestination = new File(outputDir, ze.getName());
            entryDestination.getParentFile().mkdirs();

            if (ze.isDirectory()){
                entryDestination.createNewFile();
            }
            else {
                FileOutputStream fos = new FileOutputStream(entryDestination);
                int length;
                byte[] buffer = new byte[1024];
                while((length = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.close();
            }
            ze = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }
}
