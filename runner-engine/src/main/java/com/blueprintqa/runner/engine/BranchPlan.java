package com.blueprintqa.runner.engine;

import java.util.ArrayList;
import java.util.List;

public final class BranchPlan {
    public enum Session {
        KEEP,
        NEW_SESSION
    }

    public record Entry(int index, Session session) {
        public boolean opensNewSession() {
            return session == Session.NEW_SESSION;
        }

        public boolean sharesDriver() {
            return false;
        }
    }

    private BranchPlan() {
    }

    public static List<Entry> of(int successors) {
        if (successors < 0) {
            throw new IllegalArgumentException("successor count cannot be negative");
        }
        if (successors == 0) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, Session.KEEP));
        for (int index = 1; index < successors; index++) {
            entries.add(new Entry(index, Session.NEW_SESSION));
        }
        return List.copyOf(entries);
    }

    public static int extraSessions(int successors) {
        return (int) of(successors).stream().filter(Entry::opensNewSession).count();
    }
}
