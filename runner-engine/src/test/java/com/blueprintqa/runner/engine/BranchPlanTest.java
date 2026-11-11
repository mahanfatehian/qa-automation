package com.blueprintqa.runner.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchPlanTest {
    @Test
    void oneSuccessorKeepsTheLiveSession() {
        List<BranchPlan.Entry> plan = BranchPlan.of(1);
        assertEquals(0, BranchPlan.extraSessions(1));
        assertEquals(1, plan.size());
        assertEquals(0, plan.get(0).index());
        assertEquals(BranchPlan.Session.KEEP, plan.get(0).session());
        assertFalse(plan.get(0).opensNewSession());
        assertNoSharedDriver(plan);
    }

    @Test
    void twoSuccessorsOpenExactlyOneNewSession() {
        List<BranchPlan.Entry> plan = BranchPlan.of(2);
        assertEquals(1, BranchPlan.extraSessions(2));
        assertEquals(2, plan.size());
        assertEquals(BranchPlan.Session.KEEP, plan.get(0).session());
        assertEquals(BranchPlan.Session.NEW_SESSION, plan.get(1).session());
        assertEquals(1, plan.stream().filter(BranchPlan.Entry::opensNewSession).count());
        assertNoSharedDriver(plan);
    }

    @Test
    void threeSuccessorsOpenExactlyTwoNewSessions() {
        List<BranchPlan.Entry> plan = BranchPlan.of(3);
        assertEquals(2, BranchPlan.extraSessions(3));
        assertEquals(3, plan.size());
        assertEquals(0, plan.get(0).index());
        assertEquals(1, plan.get(1).index());
        assertEquals(2, plan.get(2).index());
        assertEquals(2, plan.stream().filter(BranchPlan.Entry::opensNewSession).count());
        assertNoSharedDriver(plan);
    }

    private static void assertNoSharedDriver(List<BranchPlan.Entry> plan) {
        for (BranchPlan.Entry entry : plan) {
            assertFalse(entry.sharesDriver());
            String label = entry.session().name().toLowerCase();
            assertFalse(label.contains("reuse"));
            assertFalse(label.contains("serial"));
            assertFalse(label.contains("same"));
            assertTrue(entry.session() == BranchPlan.Session.KEEP || entry.session() == BranchPlan.Session.NEW_SESSION);
        }
    }
}
