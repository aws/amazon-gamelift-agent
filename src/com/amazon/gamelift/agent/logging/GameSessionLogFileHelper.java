/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import com.amazon.gamelift.agent.model.ConfiguredLogPaths;
import com.amazon.gamelift.agent.model.OperatingSystem;
import com.amazon.gamelift.agent.model.exception.InvalidRequestException;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class GameSessionLogFileHelper {
    private final OperatingSystem operatingSystem;
    @VisibleForTesting static final int MAXIMUM_UNIX_PATH_LENGTH = 4096;
    @VisibleForTesting static final int MAXIMUM_WINDOWS_PATH_LENGTH = 259;

    /**
     * Determines if the provided path has invalid characters see
     * http://msdn.microsoft.com/en-us/library/windows/desktop/aa365247%28v=vs.85%29.aspx
     * does not include '\', '*', '?', or ':'
     **/
    private static final String INVALID_WINDOWS_CHARS = "<>\"/|";

    /**
     * The explicitly invalid characters for file names are '/' and '\0'.
     * However, there are many characters in which it is a bad idea to allow.
     * '/' are part of file paths and so should be allowed.
     * http://www.dwheeler.com/secure-programs/Secure-Programs-HOWTO/file-names.html
     */
    private static final String INVALID_LINUX_CHARS = "<>\"'|*?[]\n\r\\\0";

    /**
     * Configures log paths based on launch path
     * @param logPaths
     * @param launchPath
     */
    public ConfiguredLogPaths configureLogPaths(final List<String> logPaths, final String launchPath) {
        final ConfiguredLogPaths configuredLogPaths = new ConfiguredLogPaths();

        if (CollectionUtils.isNotEmpty(logPaths)) {
            for (final String logPath : makePathsAbsolute(logPaths, launchPath)) {
                try {
                    validateLogPath(logPath);
                    configuredLogPaths.addValidLogPath(logPath);
                } catch (final InvalidRequestException e) {
                    log.error("Invalid GameServer log detected: {}", logPath, e);
                    configuredLogPaths.addInvalidLogPath(logPath);
                }
            }
        }
        return configuredLogPaths;
    }

    /**
     * Processes and validates the given log paths based on the given OperatingSystem.
     *
     * @param path - Game session log path
     * @return String for validated log path with proper slashes and endings
     * @throws InvalidRequestException
     */
    public String validateLogPath(final String path)
            throws InvalidRequestException {
        if (!path.toLowerCase().startsWith(operatingSystem.getLaunchPathPrefix().toLowerCase())) {
            throw new InvalidRequestException(String.format("Log path %s has invalid prefix for OperatingSystem %s",
                    path, operatingSystem.getDisplayName()));
        } else if (!isPathLengthValid(path)) {
            throw new InvalidRequestException(String.format("Log path %s has invalid length %s",
                    path, path.length()));
        } else if (!isPathContainingValidCharacters(path)) {
            throw new InvalidRequestException(String.format("Log path %s contains invalid character(s)", path));
        }
        return FilenameUtils.normalizeNoEndSeparator(path, operatingSystem.isLinux());
    }

    /**
     * This method ensures that all paths are absolute.
     *
     * <p>
     *     In the context of GameLift relative paths are relative to the directory where the server executable is.
     *     This means relative log paths need to be converted to absolute using the executables path.
     *
     *     Otherwise, relative log paths would be relative to where GameLiftAgent is running, which is wrong.
     * </p>
     *
     * @param logPaths - paths of all the logs to be uploaded
     * @param launchPath - launch path of the given executable
     * @return updated list where any log paths that weren't absolute are now absolute relative to launch path
     */
    private List<String> makePathsAbsolute(final List<String> logPaths, final String launchPath) {
        final String basePath = Paths.get(launchPath).getParent().toString();
        return logPaths.stream()
                .map(s -> normalizeSlash(s))
                .map(s -> Paths.get(s).isAbsolute() ? s : Paths.get(basePath, s).toString())
                .collect(Collectors.toList());
    }

    private String normalizeSlash(final String absolutePath) {
        return FilenameUtils.separatorsToSystem(absolutePath);
    }

    private boolean isPathLengthValid(final String path) {
        if (!operatingSystem.isLinux()) {
            return isSmallerThanMaxWindowsPath(path);
        } else {
            return isSmallerThanMaxLinuxPath(path);
        }
    }

    private boolean isPathContainingValidCharacters(final String path) {
        if (!operatingSystem.isLinux()) {
            return hasValidWindowsChars(path);
        } else {
            return hasValidLinuxChars(path);
        }
    }

    /**
     * Determines if the provided path is <= the windows max path length. Max
     * Path length is defined as
     * "drive letter, colon, backslash, followed by at most 256 characters" see
     * http://msdn.microsoft.com/en-us/library/windows/desktop/aa365247%28v=vs.85%29.aspx
     * <p>
     * returns true for null and empty paths
     **/
    private static boolean isSmallerThanMaxWindowsPath(final String path) {
        return isSmallerThanMaxPath(path, MAXIMUM_WINDOWS_PATH_LENGTH);
    }

    /**
     * Determines if the provided path is <= the Linux max path length. Using
     * "getconf NAME_MAX /" and "getconf PATH_MAX /" might be enough.
     * <p>
     * Did not see an obvious/good way to call getconf. 4096 seems to be the
     * standard maximum file path length.
     * <p>
     * returns true for null and empty paths
     **/
    private static boolean isSmallerThanMaxLinuxPath(final String path) {
        return isSmallerThanMaxPath(path, MAXIMUM_UNIX_PATH_LENGTH);
    }

    private static boolean isSmallerThanMaxPath(final String path,
                                                final int maxPathLength) {
        return StringUtils.length(path) <= maxPathLength;
    }

    /**
     * Returns true iff the log file path is valid for Windows.
     */
    private static boolean hasValidWindowsChars(final String logPath) {
        return StringUtils.containsNone(logPath, INVALID_WINDOWS_CHARS);
    }

    /**
     * Returns true iff the log file path is valid for Linux.
     */
    private static boolean hasValidLinuxChars(final String logPath) {
        return StringUtils.containsNone(logPath, INVALID_LINUX_CHARS);
    }
}
