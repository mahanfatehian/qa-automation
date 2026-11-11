package com.blueprintqa.runner.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickLandingTest {
    @Test
    void urlChangeOrMutationCountsAsLanded() {
        assertTrue(ClickLanding.landed("https://app.example/start", "https://app.example/next", 4, 4));
        assertTrue(ClickLanding.landed("https://app.example/start", "https://app.example/start", 4, 5));
        assertFalse(ClickLanding.landed("https://app.example/start", "https://app.example/start", 4, 4));
    }

    @Test
    void checkedChangeCountsAsLanded() {
        assertTrue(ClickLanding.landed("https://app.example/start", "https://app.example/start", 1, 1, false, true));
        assertFalse(ClickLanding.landed("https://app.example/start", "https://app.example/start", 1, 1, true, true));
    }

    @Test
    void blankCloneIsNotTheForkPointPage() {
        assertFalse(BrowserCheckpoint.pageLanded("https://app.example/start", "about:blank"));
        assertFalse(BrowserCheckpoint.pageLanded("https://app.example/start", ""));
        assertFalse(BrowserCheckpoint.pageLanded("about:blank", "about:blank"));
        assertTrue(BrowserCheckpoint.pageLanded("https://app.example/start", "https://app.example/start"));
        assertFalse(BrowserCheckpoint.pageLanded("https://app.example/start", "https://app.example/other"));
    }
}
