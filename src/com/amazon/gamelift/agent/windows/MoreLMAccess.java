/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.windows;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.LMAccess;

import java.util.List;

/**
 * Interface for More LMAccess *
 */
public interface MoreLMAccess extends LMAccess {
    int NERR_Success = 0;

    int LEVEL_USER_INFO_1003 = 1003;

    /**
     * The USER_INFO_1003 structure contains a user password. This information level is valid only when NetUserSetInfo
     * function is called.
     *
     * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/aa370963(v=vs.85).aspx">MSDN</a>
     */
    class USER_INFO_1003 extends Structure {
        public WString usri1003_password;

        public USER_INFO_1003() {
        }

        public USER_INFO_1003(final Pointer memory) {
            super(memory);
            this.read();
        }

        protected List<String> getFieldOrder() {
            return List.of("usri1003_password");
        }
    }
}
