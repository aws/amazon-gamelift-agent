/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstanceTerminationMonitorTest {
    private static final Instant TERMINATION_TIME = Instant.now().plus(Duration.ofMinutes(1));

    @Mock
    private TerminationNoticeReader terminationNoticeReader;
    @Mock
    private ScheduledExecutorService executorService;
    @Mock
    private ShutdownOrchestrator shutdownOrchestrator;

    @InjectMocks
    private InstanceTerminationMonitor monitor;

    @Test
    public void GIVEN_executor_WHEN_start_THEN_schedulesMonitor() {
        // WHEN
        monitor.start();

        // THEN
        verify(executorService).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
    }

    @Test
    public void GIVEN_terminatingNoticeFromFile_WHEN_run_THEN_startsTermination() {
        // GIVEN
        when(terminationNoticeReader.getTerminationNoticeFromLocalFile())
                .thenReturn(Optional.of(TERMINATION_TIME));

        // WHEN
        monitor.run();

        // THEN
        verify(shutdownOrchestrator).startTermination(any(), eq(true));
    }

    @Test
    public void GIVEN_terminatingNoticeFromEc2_WHEN_run_THEN_startsTermination() {
        // GIVEN
        when(terminationNoticeReader.getTerminationNoticeFromEC2Metadata())
                .thenReturn(Optional.of(TERMINATION_TIME));

        // WHEN
        monitor.run();

        // THEN
        verify(shutdownOrchestrator).startTermination(any(), eq(true));
    }

    @Test
    public void GIVEN_noTerminatingNotice_WHEN_run_THEN_noop() {
        // GIVEN
        when(terminationNoticeReader.getTerminationNoticeFromEC2Metadata()).thenReturn(Optional.empty());
        when(terminationNoticeReader.getTerminationNoticeFromLocalFile()).thenReturn(Optional.empty());

        // WHEN
        monitor.run();

        // THEN
        verify(shutdownOrchestrator, never()).startTermination(any(), anyBoolean());
    }

    @Test
    public void GIVEN_duplicateTerminationRuns_WHEN_run_THEN_startsTerminationOnce() {
        // GIVEN
        when(terminationNoticeReader.getTerminationNoticeFromEC2Metadata())
                .thenReturn(Optional.of(TERMINATION_TIME));

        // WHEN
        monitor.run();
        monitor.run();

        // THEN
        verify(shutdownOrchestrator).startTermination(any(), eq(true));
    }

    @Test
    public void GIVEN_terminationNoticeReaderThrows_WHEN_run_THEN_silentlyFails() {
        // GIVEN
        when(terminationNoticeReader.getTerminationNoticeFromEC2Metadata())
                .thenThrow(RuntimeException.class);

        // WHEN
        monitor.run();

        // THEN
        verify(shutdownOrchestrator, never()).startTermination(any(), anyBoolean());
    }
}