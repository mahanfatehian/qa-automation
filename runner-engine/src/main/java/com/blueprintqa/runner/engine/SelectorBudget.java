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
        int each = Math.max(1, timeout / candidates);
        int used = 0;
        for (int index = 0; index < candidates; index++) {
            int left = timeout - used;
            int give = index == candidates - 1 ? left : Math.min(each, left);
            slices[index] = Math.max(0, give);
            used += slices[index];
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
