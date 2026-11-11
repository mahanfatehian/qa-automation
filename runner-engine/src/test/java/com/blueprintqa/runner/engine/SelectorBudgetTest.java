package com.blueprintqa.runner.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorBudgetTest {
    @Test
    void lastCandidateGetsLeftoverInsteadOfAnEvenSlice() {
        int[] slices = SelectorBudget.allocate(5, 10);
        assertEquals(5, slices.length);
        int even = Math.max(2, 10 / 5);
        int probed = 0;
        for (int index = 0; index < 4; index++) {
            assertTrue(slices[index] <= SelectorBudget.PROBE_SECONDS);
            probed += slices[index];
        }
        assertTrue(slices[4] > slices[0]);
        assertTrue(slices[4] != even || slices[0] != even);
        assertEquals(10 - probed, slices[4]);
        assertEquals(slices[4], SelectorBudget.leftover(5, 10));
    }

    @Test
    void singleCandidateKeepsTheWholeBudget() {
        assertEquals(10, SelectorBudget.allocate(1, 10)[0]);
    }
}
