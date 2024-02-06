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
    MoreAdvapi32 INSTANCE = MoreAdvapi32.class.cast(Native.loadLibrary("Advapi32",
                                                                       MoreAdvapi32.class,
                                                                       W32APIOptions.UNICODE_OPTIONS));

    boolean CreateProcessAsUser(WinNT.HANDLE hToken,
                                String lpApplicationName,
                                String lpCommandLine,
                                WinBase.SECURITY_ATTRIBUTES lpProcessAttributes,
                                WinBase.SECURITY_ATTRIBUTES lpThreadAttributes,
                                boolean bInheritHandles,
                                int dwCreationFlags,
                                Pointer lpEnvironment,
                                String lpCurrentDirectory,
                                WinBase.STARTUPINFO lpStartupInfo,
                                WinBase.PROCESS_INFORMATION lpProcessInformation);
}
