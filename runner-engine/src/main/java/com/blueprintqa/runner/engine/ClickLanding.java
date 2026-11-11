package com.blueprintqa.runner.engine;

public final class ClickLanding {
    private ClickLanding() {
    }

    public static boolean landed(String beforeUrl, String afterUrl, long beforeMutation, long afterMutation) {
        return urlChanged(beforeUrl, afterUrl) || afterMutation > beforeMutation;
    }

    public static boolean landed(String beforeUrl, String afterUrl, long beforeMutation, long afterMutation, boolean checkedBefore, boolean checkedAfter) {
        return landed(beforeUrl, afterUrl, beforeMutation, afterMutation) || checkedBefore != checkedAfter;
    }

    public static boolean urlChanged(String beforeUrl, String afterUrl) {
        return !text(beforeUrl).equals(text(afterUrl));
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
