/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.windows;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Netapi32;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Interface for More Netapi32
 */
public interface MoreNetapi32 extends Netapi32 {
    MoreNetapi32 INSTANCE = MoreNetapi32.class.cast(Native.loadLibrary("Netapi32",
                                                                       MoreNetapi32.class,
                                                                       W32APIOptions.UNICODE_OPTIONS));

    int NetUserSetInfo(String serverName, String userName, int level, Structure buffer, IntByReference error);
}
