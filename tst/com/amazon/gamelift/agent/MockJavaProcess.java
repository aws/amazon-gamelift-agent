/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent;

import java.io.InputStream;
import java.io.OutputStream;

public class MockJavaProcess extends Process {

    private boolean isShutDown = false;
    private final Integer exitCode;

    public MockJavaProcess(int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public OutputStream getOutputStream() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public InputStream getErrorStream() {
        return null;
    }

    @Override
    public int waitFor() throws InterruptedException {
        while (!isShutDown) {
            Thread.sleep(1000);
        }
        return exitCode;
    }

    @Override
    public int exitValue() {
        return exitCode;
    }

    @Override
    public void destroy() {
        isShutDown = true;
    }

    @Override
    public boolean isAlive() {
        return !isShutDown;
    }
}
