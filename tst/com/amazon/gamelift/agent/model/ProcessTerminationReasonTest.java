package com.amazon.gamelift.agent.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessTerminationReasonTest {

    @Test
    public void GIVEN_reasonWithEventCode_WHEN_getEventCode_THEN_returnsCodeString() {
        assertEquals("SERVER_PROCESS_CRASHED", ProcessTerminationReason.SERVER_PROCESS_CRASHED.getEventCode());
    }

    @Test
    public void GIVEN_reasonWithoutEventCode_WHEN_getEventCode_THEN_returnsNull() {
        assertEquals(null, ProcessTerminationReason.COMPUTE_SHUTTING_DOWN.getEventCode());
    }
}
