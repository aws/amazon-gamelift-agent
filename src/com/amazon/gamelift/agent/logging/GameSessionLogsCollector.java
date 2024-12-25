/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import com.amazon.gamelift.agent.model.ConfiguredLogPaths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Named
@Slf4j
@RequiredArgsConstructor
public class GameSessionLogsCollector {
    private final String fleetId;
    private final String computeName;
    private final String processUUID;
    private final String launchPath;
    private final GameSessionLogFileHelper gameSessionLogFileHelper;

    private static final String LOG_ERROR_README_FILENAME = "ReadMe.txt";

    /**
     * Creates zipped log file based on log paths and game session ID
     * @param logPaths
     * @param gameSessionId
     * @return
     * @throws IOException
     */
    public File collectGameSessionLogs(final List<String> logPaths, final String gameSessionId)
            throws IOException {
        // Prepare log paths
        final ConfiguredLogPaths configuredLogPaths =
                gameSessionLogFileHelper.configureLogPaths(logPaths, launchPath);
        final List<String> validLogPaths = configuredLogPaths.getValidLogPaths();
        final List<String> invalidLogPaths = configuredLogPaths.getInvalidLogPaths();
        final List<GameSessionLogPath> gameSessionLogPaths =
                ConfiguredLogPaths.convertToGameSessionLogPaths(validLogPaths);

        if (StringUtils.isNotBlank(gameSessionId)) {
            log.info("Collecting logs for fleetId {}, computeName {}, processUUID {}, GameSessionId {}",
                    fleetId, computeName, processUUID, gameSessionId);
        } else {
            log.info("Collecting logs for fleetId {}, computeName {}, processUUID {} without a GameSession",
                    fleetId, computeName, processUUID);
        }

        /**
         *  Creates a temporary directory "gameSessionTempLogs-randomAbc123" in an OS-specific temporary location
         *  Creates a temporary "logs" directory within the above for collecting logs
         *  Creates a zip file in the "gameSessionTempLogs-randomAbc123" directory for zipping aggregated logs
         */
        final Path tempLogsBaseDirPath = Files.createTempDirectory("gameSessionTempLogs-");
        if (!Files.exists(tempLogsBaseDirPath)) {
            log.error("Failed to create temp GameSession logs path: {}", tempLogsBaseDirPath);
            throw new RuntimeException(String.format("Failed to create path: %s", tempLogsBaseDirPath));
        }

        final File tempLogsDirectory = new File(tempLogsBaseDirPath.toFile(), "logs" + File.separator);
        final boolean mkdirResult = tempLogsDirectory.mkdir();
        if (!mkdirResult) {
            log.error("Failed to create temp GameSession logs directory: {}", tempLogsDirectory);
            throw new RuntimeException(String.format("Failed to create temp logs directory for %s", processUUID));
        }
        final File logZipFile = new File(tempLogsBaseDirPath.toFile(), "logs.zip");

        final GameSessionLogsErrorReadMeFile errorReadMeFile =
                new GameSessionLogsErrorReadMeFile(tempLogsDirectory, LOG_ERROR_README_FILENAME);

        // Collect logs to zip file
        gameSessionLogPaths.addAll(expandWildcardLogPathsToFileLogPaths(gameSessionLogPaths, errorReadMeFile));
        copyLogFilesToTempDirectory(tempLogsDirectory, gameSessionLogPaths, errorReadMeFile);
        recordInvalidLogPaths(errorReadMeFile, invalidLogPaths);
        errorReadMeFile.close();
        writeZipFile(tempLogsDirectory);

        return logZipFile;
    }

    /**
     * Deletes log files based on log paths.
     * @param logPaths
     */
    public void deleteGameSessionLogs(final List<String> logPaths) {
        final ConfiguredLogPaths configuredLogPaths =
                gameSessionLogFileHelper.configureLogPaths(logPaths, launchPath);
        final List<String> validLogPaths = configuredLogPaths.getValidLogPaths();
        final List<GameSessionLogPath> gameSessionLogPaths =
                ConfiguredLogPaths.convertToGameSessionLogPaths(validLogPaths);
        gameSessionLogPaths.addAll(expandWildcardLogPathsToFileLogPaths(gameSessionLogPaths, null));

        for (final GameSessionLogPath logPath : gameSessionLogPaths) {
            // Skip paths with wildcards when deleting, otherwise this will delete the base folder
            if (StringUtils.isBlank(logPath.getWildcardToGet())) {
                log.info("Deleting GameSession log file: {}", logPath.getSourcePath());
                try {
                    final File logFile = new File(logPath.getSourcePath()).getAbsoluteFile();
                    if (logFile.isFile()) {
                        Files.deleteIfExists(logFile.toPath());
                    } else {
                        FileUtils.deleteDirectory(logFile);
                    }
                } catch (final IOException e) {
                    log.error("Failed to delete GameSession log file: {}", logPath.getSourcePath(), e);
                }
            }
        }
    }

    /**
     * Checks for log paths with wildcards and expands these into log paths for individual matching files
     * @param logLocations GameSessionLogPath objects - paths for log file, relative path in zip and optional wildcard
     * @param readMeFile
     * @return List of ConfiguredLogPaths to add to the overall list of paths
     */
    private List<GameSessionLogPath> expandWildcardLogPathsToFileLogPaths(
                                                            final List<GameSessionLogPath> logLocations,
                                                            final GameSessionLogsErrorReadMeFile readMeFile) {
        final List<GameSessionLogPath> addedLogPaths = new ArrayList<>();

        for (final GameSessionLogPath path : logLocations) {
            final String wildCard = path.getWildcardToGet();
            if (wildCard != null) {
                final File sourceFolder = new File(path.getSourcePath());

                if (sourceFolder.exists()) {
                    for (final File file : Objects.requireNonNull(sourceFolder.listFiles())) {
                        if (FilenameUtils.wildcardMatch(file.getName(), wildCard, IOCase.INSENSITIVE)) {
                            final String relativePathInZip = path.getRelativePathInZip();
                            String destinationPath = null;
                            if (relativePathInZip != null) {
                                destinationPath = relativePathInZip + File.separator + file.getName();
                            }
                            addedLogPaths.add(new GameSessionLogPath(file.getAbsolutePath(), destinationPath));
                        }
                    }
                } else {
                    if (readMeFile != null) {
                        readMeFile.writeLine("Error: Missing file/directory\t" + sourceFolder);
                    }
                    log.info("Missing file/directory: {}", sourceFolder);
                }
            }
        }
        return addedLogPaths;
    }

    private List<File> copyLogFilesToTempDirectory(final File tempLogsDirectory,
                                                   final List<GameSessionLogPath> logList,
                                                   final GameSessionLogsErrorReadMeFile readMeFile) {
        final List<String> alreadyCollectedPaths = new ArrayList<>();
        final List<File> logFiles = new ArrayList<>();

        for (final GameSessionLogPath logEntry : logList) {
            log.info("Attempting to collect log path: {}", logEntry.getSourcePath());
            final String logPath = logEntry.getSourcePath();
            final String fileName = FilenameUtils.getName(logPath);
            final String relativePath = logEntry.getRelativePathInZip() == null
                    ? fileName : logEntry.getRelativePathInZip();
            final String sourcePath = logEntry.getSourcePath() == null
                    ? fileName : logEntry.getSourcePath();

            try {
                if (StringUtils.isEmpty(logPath)) {
                    continue;
                }

                final File logFile = new File(logPath).getCanonicalFile();
                final String canonicalPath = logFile.getCanonicalPath();
                final File copiedLogFile = new File(tempLogsDirectory, relativePath);

                // Ignore log locations pointing to destination file and log paths searching for wild cards
                if (canonicalPath.equals(tempLogsDirectory.getCanonicalPath()) || logEntry.getWildcardToGet() != null) {
                    continue;
                }

                if (alreadyCollectedPaths.contains(canonicalPath)) {
                    continue;
                }

                if (!logFile.exists()) {
                    readMeFile.writeLine("Error: Missing file/directory\t" + sourcePath);
                    log.debug("Missing file/directory: {}", sourcePath);
                    continue;
                }

                if (!canRead(logFile)) {
                    readMeFile.writeLine("Error: No Read Permission\t" + sourcePath);
                    log.debug("Unreadable log: {}", sourcePath);
                    continue;
                }

                alreadyCollectedPaths.add(canonicalPath);
                if (logFile.isFile()) {
                    logFiles.add(copyLogFile(logFile, copiedLogFile));

                    readMeFile.writeLine("1 File Collected\t\t\t\t" + sourcePath);
                    log.debug("Collected log: {}", sourcePath);
                } else {
                    final List<File> dirFiles = copyDirectory(logFile, copiedLogFile, alreadyCollectedPaths);
                    logFiles.addAll(dirFiles);

                    readMeFile.writeLine(dirFiles.size() + " File(s) Collected\t\t\t\t" + sourcePath);
                }
            } catch (final IOException e) {
                readMeFile.writeLine("Error: Internal Error\t" + sourcePath);
                log.error("Error collecting log '{}': {}", logPath, e.getMessage(), e);
            }
        }

        log.info("Logs successfully collected");
        return logFiles;
    }

    private void recordInvalidLogPaths(final GameSessionLogsErrorReadMeFile readMeFile,
                                       final List<String> invalidLogsPaths) {
        if (CollectionUtils.isNotEmpty(invalidLogsPaths)) {
            for (final String invalidLogPath : invalidLogsPaths) {
                readMeFile.writeLine("Error: Invalid Log Path\t" + invalidLogPath);
                log.debug("Invalid log path '{}'", invalidLogPath);
            }
        }
    }

    private List<File> copyDirectory(final File srcDir,
                                     final File destDir,
                                     final List<String> alreadyCopiedPaths) throws IOException {
        // Code taken from FileUtils.copyDirectory but modified to track copied directories
        final File[] srcFiles = srcDir.listFiles();
        if (srcFiles == null) {
            // null if abstract pathname does not denote a directory, or if an I/O error occurs
            throw new IOException("Failed to list contents of " + srcDir);
        }
        if (destDir.exists()) {
            if (!destDir.isDirectory()) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (!destDir.mkdirs() && !destDir.isDirectory()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        }
        if (!destDir.canWrite()) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }

        final List<File> logFiles = new ArrayList<>();

        for (final File srcFile : srcFiles) {
            if (!canRead(srcFile)) {
                continue;
            }

            final String canPath = srcFile.getCanonicalPath();
            if (alreadyCopiedPaths.contains(canPath)) {
                continue;
            }
            alreadyCopiedPaths.add(canPath);

            final File dstFile = new File(destDir, srcFile.getName());
            if (srcFile.isDirectory()) {
                logFiles.addAll(copyDirectory(srcFile, dstFile, alreadyCopiedPaths));
            } else {
                logFiles.add(copyLogFile(srcFile, dstFile));
                log.debug("Collected log: {}", srcFile.getAbsolutePath());
            }
        }
        return logFiles;
    }

    private boolean canRead(final File file) throws IOException {
        final Path path = Paths.get(file.getCanonicalPath());

        if (SystemUtils.IS_OS_WINDOWS) {
            final AclFileAttributeView attrs = Files.getFileAttributeView(path, AclFileAttributeView.class);

            for (final AclEntry entry: attrs.getAcl()) {
                if (entry.principal().getName().equals("WHITEWATER\\WhitewaterDeveloper")
                        && entry.type() == AclEntryType.DENY) {
                    return !entry.permissions().contains(AclEntryPermission.READ_DATA);
                }
            }
            return true;
        } else {
            final PosixFileAttributes attrs = Files.getFileAttributeView(path,
                    PosixFileAttributeView.class).readAttributes();

            final boolean isInUsersGroup = attrs.group().getName()
                    .equalsIgnoreCase("Users");

            return (isInUsersGroup && attrs.permissions().contains(
                    PosixFilePermission.GROUP_READ))
                    || attrs.permissions()
                    .contains(PosixFilePermission.OTHERS_READ);
        }
    }

    private File copyLogFile(final File logFile, final File copiedLogFile) throws IOException {
        // Copy File to temp directory
        FileUtils.copyFile(logFile, copiedLogFile);
        return copiedLogFile;
    }

    private void writeZipFile(final File directoryToZip) {
        try (
                FileOutputStream fos = new FileOutputStream(directoryToZip.getCanonicalPath() + ".zip");
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            final int baseDirLength = directoryToZip.getCanonicalPath().length() + 1;
            addToZip(directoryToZip, baseDirLength, zos);
            log.debug("Zip file successfully created");
        } catch (final FileNotFoundException e) {
            log.error("{}.zip file not found: {}", directoryToZip.getPath(), e.getMessage(), e);
        } catch (final IOException e) {
            log.error("getCanonicalFile() failed for '{}': {}", directoryToZip.getPath(), e.getMessage(), e);
        }
    }

    private void addToZip(final File file, final int baseDirLength, final ZipOutputStream zos) {
        try {
            if (file.isFile() || (file.isDirectory() && Objects.requireNonNull(file.list()).length == 0)) {
                // Add this entry to the zip.
                String zipPath = StringUtils.substring(file.getCanonicalPath(), baseDirLength);
                if (file.isDirectory()) {
                    zipPath += File.separator;
                }

                final ZipEntry zipEntry = new ZipEntry(zipPath);
                zos.putNextEntry(zipEntry);

                if (file.isFile()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        final long bytesCopied = IOUtils.copyLarge(fis, zos);
                        log.debug("Added to zip file: '{}' ({} byte(s))", file.getPath(), bytesCopied);
                    } catch (final IOException e) {
                        log.error("Error writing data for '{}': {}", file.getPath(), e.getMessage(), e);
                    }
                }
                zos.closeEntry();
            } else {
                // Recurse directory.
                for (final File subFile : Objects.requireNonNull(file.listFiles())) {
                    addToZip(subFile, baseDirLength, zos);
                }
            }
        } catch (final IOException e) {
            log.error("Failed to add '{}': {}", file.getPath(), e.getMessage(), e);
        }
    }
}
