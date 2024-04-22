/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.logging;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Encapsulate access to the log error ReadMe file generated when collecting GameServerLogs.
 */
@Slf4j
public class GameSessionLogsErrorReadMeFile {
    private BufferedWriter logErrorReadMeFile;
    private boolean isOpen = false;

    /**
     * Constructor for GameSessionLogsErrorReadMeFile
     * @param directory
     * @param fileName
     */
    public GameSessionLogsErrorReadMeFile(final File directory, final String fileName) {
        try {
            logErrorReadMeFile = Files.newBufferedWriter(new File(directory, fileName).toPath(),
                    UTF_8, StandardOpenOption.CREATE_NEW);
            isOpen = true;
            writeLine("Game Server Logs");
        } catch (final IOException e) {
            log.error("Could not open readMe file for writing", e);
        }

    }

    /**
     * Writes a string to the ErrorReadeFile
     * @param s
     */
    public void writeLine(final String s) {
        if (!isOpen) {
            log.error("Could not write readme, file is not open.  Text to Write: {}", s);
            return;
        }
        try {
            logErrorReadMeFile.write(s);
            logErrorReadMeFile.newLine();
        } catch (final IOException e) {
            log.error("Could not write line {}", s, e);
        }

    }

    /**
     * CLoses ErrorReadMeFile
     */
    public void close() {
        if (!isOpen) {
            return;
        }
        try {
            logErrorReadMeFile.close();
            isOpen = false;
        } catch (final IOException e) {
            log.error("Could not close file, may lose data", e);
        }
    }

}
