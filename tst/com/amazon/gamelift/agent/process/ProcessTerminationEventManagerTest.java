/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.gamelift.agent.model.ProcessTerminationReason;
import com.amazon.gamelift.agent.model.exception.AgentException;
import com.amazon.gamelift.agent.model.websocket.NotifyServerProcessTerminationRequest;
import com.amazon.gamelift.agent.model.websocket.base.WebsocketRequest;
import com.amazon.gamelift.agent.websocket.AgentWebSocket;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionProvider;

import java.time.Duration;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessTerminationEventManagerTest {

    private static final String TEST_PROCESS_UUID = RandomStringUtils.randomAlphanumeric(10);

    @Mock private WebSocketConnectionProvider mockWebSocketConnectionProvider;
    @Mock private AgentWebSocket mockAgentWebSocket;

    @Captor private ArgumentCaptor<NotifyServerProcessTerminationRequest> requestCaptor;

    @InjectMocks private ProcessTerminationEventManager manager;

    @BeforeEach
    public void setup() {
        when(mockWebSocketConnectionProvider.getCurrentConnection()).thenReturn(mockAgentWebSocket);
    }

    @Test
    public void GIVEN_normalExitCodeNoReason_WHEN_notifyServerProcessTermination_THEN_usesCorrectEventCode() throws AgentException {
        // GIVEN/WHEN
        manager.notifyServerProcessTermination(TEST_PROCESS_UUID, 0, null);

        // THEN
        verify(mockAgentWebSocket).sendRequest(requestCaptor.capture(), any(), any(Duration.class));
        final NotifyServerProcessTerminationRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest.getAction());
        assertNotNull(capturedRequest.getRequestId());
        assertEquals(TEST_PROCESS_UUID, capturedRequest.getProcessId());
        assertEquals(ProcessTerminationReason.NORMAL_TERMINATION.getEventCode(), capturedRequest.getEventCode());
        assertEquals(ProcessTerminationReason.NORMAL_TERMINATION.name(), capturedRequest.getTerminationReason());
    }

    @Test
    public void GIVEN_normalExitCodeWithReason_WHEN_notifyServerProcessTermination_THEN_usesGivenReason() throws AgentException {
        // GIVEN/WHEN
        manager.notifyServerProcessTermination(TEST_PROCESS_UUID, 0, ProcessTerminationReason.SERVER_PROCESS_INVALID_PATH);

        // THEN
        verify(mockAgentWebSocket).sendRequest(requestCaptor.capture(), any(), any(Duration.class));
        final NotifyServerProcessTerminationRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest.getAction());
        assertNotNull(capturedRequest.getRequestId());
        assertEquals(TEST_PROCESS_UUID, capturedRequest.getProcessId());
        assertEquals(ProcessTerminationReason.SERVER_PROCESS_INVALID_PATH.getEventCode(), capturedRequest.getEventCode());
        assertEquals(ProcessTerminationReason.SERVER_PROCESS_INVALID_PATH.name(), capturedRequest.getTerminationReason());
    }

    @Test
    public void GIVEN_abnormalExitCodeNoReason_WHEN_notifyServerProcessTermination_THEN_usesCrashedReason() throws AgentException {
        // GIVEN/WHEN
        manager.notifyServerProcessTermination(TEST_PROCESS_UUID, -12345, null);

        // THEN
        verify(mockAgentWebSocket).sendRequest(requestCaptor.capture(), any(), any(Duration.class));
        final NotifyServerProcessTerminationRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest.getAction());
        assertNotNull(capturedRequest.getRequestId());
        assertEquals(TEST_PROCESS_UUID, capturedRequest.getProcessId());
        assertEquals(ProcessTerminationReason.SERVER_PROCESS_CRASHED.getEventCode(), capturedRequest.getEventCode());
        assertEquals(ProcessTerminationReason.SERVER_PROCESS_CRASHED.name(), capturedRequest.getTerminationReason());
    }

    @Test
    public void GIVEN_abnormalExitCodeWithReason_WHEN_notifyServerProcessTermination_THEN_usesGivenReason() throws AgentException {
        // GIVEN/WHEN
        manager.notifyServerProcessTermination(TEST_PROCESS_UUID, -12345, ProcessTerminationReason.COMPUTE_SHUTTING_DOWN);

        // THEN
        verify(mockAgentWebSocket).sendRequest(requestCaptor.capture(), any(), any(Duration.class));
        final NotifyServerProcessTerminationRequest capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest.getAction());
        assertNotNull(capturedRequest.getRequestId());
        assertEquals(TEST_PROCESS_UUID, capturedRequest.getProcessId());
        assertEquals(ProcessTerminationReason.COMPUTE_SHUTTING_DOWN.getEventCode(), capturedRequest.getEventCode());
        assertEquals(ProcessTerminationReason.COMPUTE_SHUTTING_DOWN.name(), capturedRequest.getTerminationReason());
    }

    @Test
    public void GIVEN_failureToSendRequest_WHEN_notifyServerProcessTermination_THEN_attemptsRetries() throws AgentException {
        // GIVEN
        doThrow(RuntimeException.class).when(mockAgentWebSocket).sendRequest(any(WebsocketRequest.class), any(), any());

        // WHEN
        assertThrows(RuntimeException.class, () ->
                manager.notifyServerProcessTermination(TEST_PROCESS_UUID, -12345, ProcessTerminationReason.COMPUTE_SHUTTING_DOWN));

        // THEN
        verify(mockAgentWebSocket, times(3)).sendRequest(any(WebsocketRequest.class), any(), any(Duration.class));
    }
}
