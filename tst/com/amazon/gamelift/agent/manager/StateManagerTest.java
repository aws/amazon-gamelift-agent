/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.gamelift.agent.model.ComputeStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StateManagerTest {

    private StateManager stateManager;

    @BeforeEach
    public void setup() {
        stateManager = new StateManager();
    }

    @Test
    public void GIVEN_stateIsActive_WHEN_reportComputeInterrupted_THEN_computeIsInterrupted() {
        stateManager.reportComputeActive();
        stateManager.reportComputeInterrupted();
        Assertions.assertEquals(ComputeStatus.Interrupted, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_newStateManager_WHEN_reportComputeActivating_THEN_computeIsActivating() {
        stateManager.reportComputeActivating();
        assertEquals(ComputeStatus.Activating, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_stateIsActive_WHEN_reportComputeActivating_THEN_computeIsActive() {
        stateManager.reportComputeActive();
        stateManager.reportComputeActivating();
        assertEquals(ComputeStatus.Active, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_newStateManager_WHEN_reportComputeActive_THEN_computeIsActive() {
        stateManager.reportComputeActive();
        assertEquals(ComputeStatus.Active, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_stateIsActivating_WHEN_reportComputeActive_THEN_computeIsActive() {
        stateManager.reportComputeActivating();
        stateManager.reportComputeActive();
        assertEquals(ComputeStatus.Active, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_stateIsTerminating_WHEN_reportComputeActive_THEN_computeIsTerminating() {
        stateManager.reportComputeTerminating();
        stateManager.reportComputeActive();
        assertEquals(ComputeStatus.Terminating, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_newStateManager_WHEN_reportComputeTerminating_THEN_computeIsTerminating() {
        stateManager.reportComputeTerminating();
        assertEquals(ComputeStatus.Terminating, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_stateIsActivating_WHEN_reportComputeTerminating_THEN_computeIsTerminating() {
        stateManager.reportComputeTerminating();
        assertEquals(ComputeStatus.Terminating, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_stateIsTerminated_WHEN_reportComputeTerminating_THEN_computeIsTerminated() {
        stateManager.reportComputeTerminated();
        stateManager.reportComputeTerminating();
        assertEquals(ComputeStatus.Terminated, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_newStateManager_WHEN_reportComputeTerminated_THEN_computeIsTerminated() {
        stateManager.reportComputeTerminated();
        assertEquals(ComputeStatus.Terminated, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_reportedActiveAfterTerminated_WHEN_reportComputeTerminated_THEN_computeIsTerminated() {
        stateManager.reportComputeTerminated();
        stateManager.reportComputeActive();
        assertEquals(ComputeStatus.Terminated, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_reportedTerminatingAfterTerminated_WHEN_reportComputeTerminated_THEN_computeIsTerminated() {
        stateManager.reportComputeTerminated();
        stateManager.reportComputeTerminating();
        assertEquals(ComputeStatus.Terminated, stateManager.getComputeStatus());
    }

    @Test
    public void GIVEN_stateIsTerminated_WHEN_isComputeTerminatingOrTerminated_THEN_returnsTrue() {
        stateManager.reportComputeTerminated();
        assertTrue(stateManager.isComputeTerminatingOrTerminated());
    }

    @Test
    public void GIVEN_stateIsTerminating_WHEN_isComputeTerminatingOrTerminated_THEN_returnsTrue() {
        stateManager.reportComputeTerminating();
        assertTrue(stateManager.isComputeTerminatingOrTerminated());
    }

    @Test
    public void GIVEN_nonTerminatingStates_WHEN_isComputeTerminatingOrTerminated_THEN_returnsFalse() {
        assertFalse(stateManager.isComputeTerminatingOrTerminated());
        stateManager.reportComputeActivating();
        assertFalse(stateManager.isComputeTerminatingOrTerminated());
        stateManager.reportComputeActive();
        assertFalse(stateManager.isComputeTerminatingOrTerminated());
    }

    @Test
    public void GIVEN_stateIsTerminated_WHEN_isHostTerminated_THEN_returnsTrue() {
        stateManager.reportComputeTerminated();
        assertTrue(stateManager.isComputeTerminated());
    }

    @Test
    public void GIVEN_nonTerminatedStates_WHEN_isHostTerminated_THEN_returnsFalse() {
        assertFalse(stateManager.isComputeTerminated());
        stateManager.reportComputeActivating();
        assertFalse(stateManager.isComputeTerminated());
        stateManager.reportComputeActive();
        assertFalse(stateManager.isComputeTerminated());
        stateManager.reportComputeTerminating();
        assertFalse(stateManager.isComputeTerminated());
        stateManager.reportComputeInterrupted();
        assertFalse(stateManager.isComputeTerminated());
    }
}
