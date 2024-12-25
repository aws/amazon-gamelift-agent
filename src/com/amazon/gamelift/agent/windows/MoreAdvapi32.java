/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.W32APIOptions;

public interface MoreAdvapi32 extends Advapi32 {
    MoreAdvapi32 INSTANCE = Native.load("Advapi32",
            MoreAdvapi32.class,
            W32APIOptions.UNICODE_OPTIONS);

    boolean CreateProcessAsUser(final WinNT.HANDLE hToken,
                                final String lpApplicationName,
                                final String lpCommandLine,
                                final WinBase.SECURITY_ATTRIBUTES lpProcessAttributes,
                                final WinBase.SECURITY_ATTRIBUTES lpThreadAttributes,
                                final boolean bInheritHandles,
                                final int dwCreationFlags,
                                final Pointer lpEnvironment,
                                final String lpCurrentDirectory,
                                final WinBase.STARTUPINFO lpStartupInfo,
                                final WinBase.PROCESS_INFORMATION lpProcessInformation);
}
