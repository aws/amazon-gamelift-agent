/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class WindowsProcessCommons {
    static final int FORCE_TERMINATED_EXIT_CODE = -1;

    /**
     * Returns all processes that are a child of a given process on Windows-based machines
     *
     * @param processId The parent process ID
     * @return A stream of child processes handles
     */
    public static Stream<ProcessHandle> getChildren(final long processId) {
        final List<ProcessHandle> output = new ArrayList<>();

        /*
        We make a call to the Win32 kernel to snapshot the current list of processes on the machine. Then iterate over them, and if a given process's
        parent is the passed 'processId', then store and return it. There is almost no documentation for the JNA library in Java, so instead it's best
        to look up C++ documentation, and adapt the code to Java. This code is adapted from:
        https://stackoverflow.com/questions/1173342/terminate-a-process-tree-c-for-windows
         */

        WinNT.HANDLE handleSnapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));
        Tlhelp32.PROCESSENTRY32.ByReference processEntry32Ref = new Tlhelp32.PROCESSENTRY32.ByReference();

        if (!Kernel32.INSTANCE.Process32First(handleSnapshot, processEntry32Ref)) {
            return Stream.empty();
        }
        do {
            if (processEntry32Ref.th32ParentProcessID.longValue() == processId) {
                final WinDef.DWORD childProcessId = processEntry32Ref.th32ProcessID;
                final WinNT.HANDLE childProcessHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, childProcessId.intValue());
                if (childProcessHandle != null) {
                    output.add(new WindowsProcessHandle(childProcessId, childProcessHandle));
                }
            }
        } while (Kernel32.INSTANCE.Process32Next(handleSnapshot, processEntry32Ref));

        Kernel32.INSTANCE.CloseHandle(handleSnapshot);

        return output.stream();
    }
}
