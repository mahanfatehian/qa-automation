package com.blueprintqa.runner.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorBudgetTest {
    @Test
    void everySelectorSharesTheDeadline() {
        int[] slices = SelectorBudget.allocate(5, 10);
        assertEquals(5, slices.length);
        int sum = 0;
        for (int slice : slices) {
            assertTrue(slice >= 1);
            assertTrue(slice < 10);
            sum += slice;
        }
        assertEquals(10, sum);
        assertEquals(slices[0], SelectorBudget.leftover(5, 10));
    }

    @Test
    void singleCandidateKeepsTheWholeBudget() {
        assertEquals(10, SelectorBudget.allocate(1, 10)[0]);
    }
}
