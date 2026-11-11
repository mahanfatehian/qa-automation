package com.blueprintqa.runner.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorBudgetTest {
    @Test
    void firstSelectorGetsTheWaitAndFallbacksStayShort() {
        int[] slices = SelectorBudget.allocate(5, 10);
        assertEquals(5, slices.length);
        int probed = 0;
        for (int index = 1; index < slices.length; index++) {
            assertTrue(slices[index] <= SelectorBudget.PROBE_SECONDS);
            probed += slices[index];
        }
        assertTrue(slices[0] > slices[1]);
        assertEquals(10 - probed, slices[0]);
        assertEquals(slices[0], SelectorBudget.leftover(5, 10));
    }

    @Test
    void singleCandidateKeepsTheWholeBudget() {
        assertEquals(10, SelectorBudget.allocate(1, 10)[0]);
    }
}
