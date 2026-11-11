package com.blueprintqa.runner.engine;

public final class SelectorBudget {
    public static final int PROBE_SECONDS = 1;

    private SelectorBudget() {
    }

    public static int[] allocate(int candidates, int timeoutSeconds) {
        if (candidates <= 0) {
            return new int[0];
        }
        int timeout = Math.max(1, timeoutSeconds);
        int[] slices = new int[candidates];
        if (candidates == 1) {
            slices[0] = timeout;
            return slices;
        }
        int others = candidates - 1;
        int probe = PROBE_SECONDS;
        int maxProbeSpend = Math.max(0, timeout - 1);
        if (probe * others > maxProbeSpend) {
            probe = Math.max(0, maxProbeSpend / others);
        }
        int spent = probe * others;
        slices[0] = Math.max(1, timeout - spent);
        for (int index = 1; index < candidates; index++) {
            slices[index] = probe;
        }
        return slices;
    }

    public static int leftover(int candidates, int timeoutSeconds) {
        int[] slices = allocate(candidates, timeoutSeconds);
        if (slices.length == 0) {
            return Math.max(0, timeoutSeconds);
        }
        return slices[0];
    }
}
