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
 * Encapsulate access to the log error ReadMe file generated for the customer when collecting GameServerLogs.
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
    public GameSessionLogsErrorReadMeFile(File directory, String fileName) {
        try {
            logErrorReadMeFile = Files.newBufferedWriter(new File(directory, fileName).toPath(),
                    UTF_8, StandardOpenOption.CREATE_NEW);
            isOpen = true;
            writeLine("Game Server Logs");
        } catch (IOException e) {
            log.error("Could not open customer readMe file for writing", e);
        }

    }

    /**
     * Writes a string to the ErrorReadeFile
     * @param s
     */
    public void writeLine(String s) {
        if (!isOpen) {
            log.error("Could not write customer readme, file is not open.  Text to Write: {}", s);
            return;
        }
        try {
            logErrorReadMeFile.write(s);
            logErrorReadMeFile.newLine();
        } catch (IOException e) {
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
        } catch (IOException e) {
            log.error("Could not close file, may lose data", e);
        }
    }

}
