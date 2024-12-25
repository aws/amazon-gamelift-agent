/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process;

import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.websocket.NotifyServerProcessTerminationRequest;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketResponse;
import com.amazon.gamelift.agent.utils.RetryHelper;
import com.amazon.gamelift.agent.websocket.AgentWebSocket;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ProcessTerminationEventManager {

    @VisibleForTesting static final int NORMAL_EXIT_CODE = 0;
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(1);

    private final WebSocketConnectionProvider webSocketConnectionProvider;

    /**
     * Reports a process has terminated on the compute by calling the NotifyServerProcessTermination API.
     * This is required primarily for scenarios where processes crash or are terminated forcefully, in which case
     * the GameLift Server SDK may not send a message that the process has terminated.
     *
     * @param processUuid the process UUID used to register through the GameLift Server SDK
     * @param processExitCode the numeric process exit code for the server process
     * @param unvalidatedTerminationReason the reason for the termination, which may be null
     * @throws AgentException if the call to NotifyServerProcessTermination fails
     */
    public void notifyServerProcessTermination(final String processUuid,
                                               final int processExitCode,
                                               final ProcessTerminationReason unvalidatedTerminationReason)
            throws AgentException {

        final ProcessTerminationReason validatedReason =
                validateTerminationReason(processExitCode, unvalidatedTerminationReason);
        log.info("Reporting process termination for process ID {} with reason: {}", processUuid, validatedReason);
        final NotifyServerProcessTerminationRequest notifyRequest = NotifyServerProcessTerminationRequest.builder()
                .processId(processUuid)
                .eventCode(validatedReason.getEventCode())
                .terminationReason(validatedReason.name())
                .build();

        final AgentWebSocket client = webSocketConnectionProvider.getCurrentConnection();
        RetryHelper.runRetryable(() -> client.sendRequest(notifyRequest, WebsocketResponse.class, REQUEST_TIMEOUT));
    }

    /**
     * Null checks the termination reason and defaults to certain values based on the process' numeric exit code
     * @param processExitCode exit code of the server process
     * @param terminationReason termination reason marked for the server process, which may be null
     * @return a non-null termination reason for the server process
     */
    private ProcessTerminationReason validateTerminationReason(final int processExitCode,
                                                               final ProcessTerminationReason terminationReason) {
        if (terminationReason == null) {
            if (processExitCode == NORMAL_EXIT_CODE) {
                return ProcessTerminationReason.NORMAL_TERMINATION;
            } else {
                return ProcessTerminationReason.SERVER_PROCESS_CRASHED;
            }
        }

        return terminationReason;
    }
}
