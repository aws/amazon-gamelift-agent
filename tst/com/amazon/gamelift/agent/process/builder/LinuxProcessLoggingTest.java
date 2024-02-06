/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process.builder;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class does not test a class in this package, but it demonstrates an issue with Java Processes hanging.
 * Testing stdout/stderr is hard with mocks, it's more ideal to use a real file.
 * random_number_printer.sh will be used to print a large number of random integers and verify timings.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class LinuxProcessLoggingTest {

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);

    private static final Path RANDOM_NUMBER_PRINTER = Paths.get(
            Objects.requireNonNull(LinuxProcessLoggingTest.class.getResource("random_number_printer.sh")).getPath());

    /**
     * The default ProcessBuilder uses a separate stdout/stderr for logging.
     * This test floods stdout with data and the process hangs for more than 30 seconds.
     */
    @Disabled // Disabled since it adds 30 seconds to build, remove to run this test
    @Test
    public void GIVEN_defaultProcessBuilder_WHEN_processExecuted_THEN_processHangsDueToStdoutFlooding() {
        // GIVEN
        final ProcessBuilder pb = new ProcessBuilder();
        pb.command("sh", RANDOM_NUMBER_PRINTER.toString(), "-c", "20000");
        final Instant startTime = Instant.now();
        // WHEN
        log.info("Starting default process.");
        final Process process = executeProcess(pb);
        log.info("Default Run length: {}", Duration.between(startTime, Instant.now()));
        // THEN - Verify process had to be terminated (137) and took longer than the timeout to execute (30+ seconds)
        assertEquals(137, process.exitValue());
        assertTrue(Instant.now().isAfter(startTime.plus(TEST_TIMEOUT)));
    }

    /**
     * This test floods stdout with data, but redirects stdout to DISCARD, which avoids hanging the process.
     */
    @Test
    public void GIVEN_redirectedProcessBuilder_WHEN_processExecuted_THEN_processCompletesQuickly()
            throws IOException, InterruptedException {
        // GIVEN
        final ProcessBuilder pb = new ProcessBuilder();
        // This is the key difference, redirecting stdout to DISCARD and stderr to INHERIT
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.command("sh", RANDOM_NUMBER_PRINTER.toString(), "-c", "20000");
        final Instant startTime = Instant.now();
        // WHEN
        log.info("Starting redirect process.");
        final Process process = executeProcess(pb);
        log.info("Redirected Run length: {}", Duration.between(startTime, Instant.now()));
        // THEN - Verify process exited cleanly (0) and took less than the timeout to execute (~1 second)
        assertEquals(0, process.exitValue());
        assertTrue(Instant.now().isBefore(startTime.plus(TEST_TIMEOUT)));
    }

    @SneakyThrows
    private Process executeProcess(final ProcessBuilder processBuilder) {
        final Process process = processBuilder.start();
        final boolean cleanExit = process.waitFor(TEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        if (process.isAlive()) {
            // Destroy and give the process a second to finish exiting
            process.destroyForcibly();
            Thread.sleep(1000);
        }
        final int exitValue = process.exitValue();
        log.info("Clean Exit: {}, Exit value: {}", cleanExit, exitValue);
        return process;
    }
}
