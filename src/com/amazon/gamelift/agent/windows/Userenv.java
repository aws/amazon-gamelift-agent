/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

public interface Userenv extends StdCallLibrary  {
    Userenv INSTANCE = Native.load("Userenv",
            Userenv.class,
            W32APIOptions.UNICODE_OPTIONS);

    int PI_NOUI = 1;
    int PI_APPLYPOLICY = 2;

    /**
     * Retrieves the environment variables for the specified user. This block can then be passed to the
     * CreateProcessAsUser function.
     *
     * @param lpEnvironment When this function returns, receives a pointer to the new environment block. The environment
     *                      block is an array of null-terminated Unicode strings. The list ends with two nulls (\0\0).
     * @param hToken        Token for the user, returned from the LogonUser function. If this is a primary token, the
     *                      token must have TOKEN_QUERY and TOKEN_DUPLICATE access. If the token is an impersonation
     *                      token, it must have TOKEN_QUERY access. If this parameter is NULL, the returned environment
     *                      block contains system variables only.
     * @param bInherit      Specifies whether to inherit from the current process' environment. If this value is TRUE,
     *                      the process inherits thecurrent process' environment. If this value is FALSE, the process
     *                      does not inherit the current process' environment.
     * @return TRUE if successful; otherwise, FALSE.
     * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/bb762270(v=vs.85).aspx">CreateEnvironmentBlock</a>
     */
    boolean CreateEnvironmentBlock(final PointerByReference lpEnvironment,
                                   final WinNT.HANDLE hToken,
                                   final boolean bInherit);

    /**
     * Frees environment variables created by the CreateEnvironmentBlock function.
     *
     * @param lpEnvironment Pointer to the environment block created by CreateEnvironmentBlock. The environment block
     *                      is an array of null-terminated Unicode strings. The list ends with two nulls (\0\0).
     * @return TRUE if successful; otherwise, FALSE.
     * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/bb762274(v=vs.85).aspx">DestroyEnvironmentBlock</a>
     */
    boolean DestroyEnvironmentBlock(final Pointer lpEnvironment);

    /**
     * Loads the specified user's profile. The profile can be a local user profile or a roaming user profile.
     *
     * @param hToken        Token for the user, which is returned by the LogonUser, CreateRestrictedToken,
     *                      DuplicateToken, OpenProcessToken, or OpenThreadToken function. The token must have
     *                      TOKEN_QUERY, TOKEN_IMPERSONATE, and TOKEN_DUPLICATE access.
     * @param lpProfileInfo Pointer to a PROFILEINFO structure. LoadUserProfile fails and returns
     *                      ERROR_INVALID_PARAMETER if the dwSize member of the structure is not set to
     *                      sizeof(PROFILEINFO) or if the lpUserName member is NULL.
     * @return TRUE if successful; otherwise, FALSE.
     * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/bb762281(v=vs.85).aspx">LoadUserProfile</a>
     */
    boolean LoadUserProfile(final WinNT.HANDLE hToken, final PROFILEINFO lpProfileInfo);

    /**
     * Contains information used when loading or unloading a user profile.
     *
     * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/bb773378(v=vs.85).aspx">PROFILEINFO</a>
     */
    class PROFILEINFO extends Structure {
        public int dwSize;
        public int dwFlags;
        public String lpUserName;
        public String lpProfilePath;
        public String lpDefaultPath;
        public String lpServerName;
        public String lpPolicyPath;
        public WinNT.HANDLE hProfile;

        public PROFILEINFO() {
        }

        public PROFILEINFO(final Pointer memory) {
            super(memory);
            this.read();
        }

        protected List<String> getFieldOrder() {
            return Arrays.asList("dwSize",
                    "dwFlags",
                    "lpUserName",
                    "lpProfilePath",
                    "lpDefaultPath",
                    "lpServerName",
                    "lpPolicyPath",
                    "hProfile");
        }
    }
}
