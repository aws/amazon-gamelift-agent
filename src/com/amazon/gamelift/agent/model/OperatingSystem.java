/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.model;

import org.apache.commons.lang3.SystemUtils;

import lombok.Getter;

/**
 *  Specifies and describes Amazon GameLift's supported Operating Systems and their properties.
 */
@Getter
public enum OperatingSystem {
    WIN_2012("Windows Server 2012 R2", OperatingSystemFamily.WINDOWS),
    WINDOWS_2016("Windows Server 2016", OperatingSystemFamily.WINDOWS),
    WINDOWS_2019("Windows Server 2019", OperatingSystemFamily.WINDOWS),
    WINDOWS_2022("Windows Server 2022", OperatingSystemFamily.WINDOWS),
    UNKNOWN_WINDOWS("Unknown Windows", OperatingSystemFamily.WINDOWS),
    AMAZON_LINUX_2("Amazon Linux 2", OperatingSystemFamily.LINUX),
    AMAZON_LINUX_2023("Amazon Linux 2023", OperatingSystemFamily.LINUX),
    UNKNOWN_LINUX("Unknown Linux", OperatingSystemFamily.LINUX),
    INVALID("Invalid OperatingSystem", OperatingSystemFamily.INVALID);

    public static final OperatingSystem DEFAULT_OS = AMAZON_LINUX_2;
    private static final String AMAZON_LINUX_2_VERSION = "amzn2";
    private static final String AMAZON_LINUX_2023_VERSION = "amzn2023";
    private static final String WINDOWS_SERVER_2016_NAME = "Windows Server 2016";
    private static final String WINDOWS_SERVER_2019_NAME = "Windows Server 2019";
    private static final String WINDOWS_SERVER_2022_NAME = "Windows Server 2022";
    private static final String OS_NAME_SYSTEM_PROPERTY = "os.name";

    private final String displayName;
    private final OperatingSystemFamily operatingSystemFamily;
    private String launchPathPrefix;
    private String agentLogsFolder;
    private String gameMetadataFolder;
    private String gameServerCertificatesFolder;
    private String pathSeparator;

    OperatingSystem(final String displayName,
                    final OperatingSystemFamily osFamily) {
        this.displayName = displayName;
        this.operatingSystemFamily = osFamily;

        // NOTE: These constants are set here instead of static-final constants because
        // enum constructors are not allowed to access static members.
        switch (osFamily) {
            case WINDOWS:
                this.launchPathPrefix = "C:\\Game\\";
                this.agentLogsFolder = "C:\\GameLiftAgent\\Logs\\";
                this.gameMetadataFolder = "C:\\GameMetadata\\";
                this.gameServerCertificatesFolder = this.gameMetadataFolder + "Certificates\\";
                this.pathSeparator = "\\";
                break;
            case LINUX:
                this.launchPathPrefix = "/local/game/";
                this.agentLogsFolder = "/local/gameliftagent/logs/";
                this.gameMetadataFolder = "/local/gamemetadata/";
                this.gameServerCertificatesFolder = this.gameMetadataFolder + "certificates/";
                this.pathSeparator = "/";
                break;
            case INVALID:
                // do nothing
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported operating system family: %s", osFamily));

        }
    }

    public boolean isLinux() {
        return OperatingSystemFamily.LINUX == getOperatingSystemFamily();
    }

    /**
     * Get an OperatingSystem constant from a string.
     *
     * @param desiredOS String representation of an OS.
     * @return The matching {@link OperatingSystem} value, or {@link OperatingSystem.INVALID} if no match is found.
     */
    public static OperatingSystem fromString(final String desiredOS) {
        for (final OperatingSystem os : values()) {
            if (os.name().equalsIgnoreCase(desiredOS)) {
                return os;
            }
        }
        return INVALID;
    }

    /**
     * Get an {@link OperatingSystem} from the system's OS.
     *
     * @return the matching OperatingSystem or {@link OperatingSystem.INVALID} if no match is found.
     */
    public static OperatingSystem fromSystemOperatingSystem() {
        final OperatingSystem operatingSystem;

        if (SystemUtils.IS_OS_WINDOWS_2012) {
            operatingSystem = WIN_2012;
        } else if (WINDOWS_SERVER_2016_NAME.equals(System.getProperty(OS_NAME_SYSTEM_PROPERTY))) {
            operatingSystem = WINDOWS_2016;
        } else if (WINDOWS_SERVER_2019_NAME.equals(System.getProperty(OS_NAME_SYSTEM_PROPERTY))) {
            // similar method doesn't exist in SystemUtils
            operatingSystem = WINDOWS_2019;
        } else if (WINDOWS_SERVER_2022_NAME.equals(System.getProperty(OS_NAME_SYSTEM_PROPERTY))) {
            // similar method doesn't exist in SystemUtils
            operatingSystem = WINDOWS_2022;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            operatingSystem = UNKNOWN_WINDOWS;
        } else if (SystemUtils.IS_OS_LINUX && SystemUtils.OS_VERSION.contains(AMAZON_LINUX_2023_VERSION)) {
            operatingSystem = AMAZON_LINUX_2023;
        } else if (SystemUtils.IS_OS_LINUX && SystemUtils.OS_VERSION.contains(AMAZON_LINUX_2_VERSION)) {
            operatingSystem = AMAZON_LINUX_2;
        } else if (SystemUtils.IS_OS_LINUX) {
            operatingSystem = UNKNOWN_LINUX;
        } else {
            operatingSystem = INVALID;
        }

        return operatingSystem;
    }
}
