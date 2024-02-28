/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent;

import com.amazon.gamelift.agent.logging.GameLiftAgentLogUploader;
import com.amazon.gamelift.agent.manager.HeartbeatSender;
import com.amazon.gamelift.agent.manager.InstanceTerminationMonitor;
import com.amazon.gamelift.agent.manager.ShutdownOrchestrator;
import com.amazon.gamelift.agent.manager.StateManager;
import com.amazon.gamelift.agent.process.GameProcessMonitor;
import com.amazon.gamelift.agent.websocket.WebSocketConnectionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AgentTest {
    @Mock
    private WebSocketConnectionManager webSocketConnectionManager;
    @Mock
    private GameProcessMonitor gameProcessMonitor;
    @Mock
    private StateManager stateManager;
    @Mock
    private HeartbeatSender heartbeatSender;
    @Mock
    private InstanceTerminationMonitor instanceTerminationMonitor;
    @Mock
    private ShutdownOrchestrator shutdownOrchestrator;
    @Mock
    private GameLiftAgentLogUploader gameLiftAgentLogUploader;

    @InjectMocks
    private Agent agent;

    @Test
    public void GIVEN_validParams_WHEN_start_THEN_startsAndFinishes() throws Exception {
        // WHEN
        agent.start();

        // THEN
        verify(webSocketConnectionManager).connect();
        verify(heartbeatSender).start();
        verify(instanceTerminationMonitor).start();
        verify(gameProcessMonitor).start();
        verify(gameLiftAgentLogUploader).start();
    }

    @Test
    public void GIVEN_exceptionThrown_WHEN_start_THEN_reraisesException() throws Exception {
        // GIVEN
        doThrow(RuntimeException.class).when(webSocketConnectionManager).connect();

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> agent.start());
    }

    @Test
    public void GIVEN_nothing_WHEN_shutdown_THEN_executesShutdownLogic() {
        // WHEN
        agent.shutdown();

        // THEN
        verify(shutdownOrchestrator).completeTermination();
    }

    @Test
    public void GIVEN_exceptionThrown_WHEN_shutdown_THEN_reraisesException() throws InterruptedException {
        // GIVEN
        doThrow(RuntimeException.class).when(shutdownOrchestrator).completeTermination();

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> agent.shutdown());
    }
}
