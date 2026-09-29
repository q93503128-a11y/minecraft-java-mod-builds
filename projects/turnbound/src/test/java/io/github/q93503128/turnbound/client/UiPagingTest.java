package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiPagingTest {
    @Test
    void preferredMinimumNeverForcesRowsOutsideAvailableSpace() {
        assertEquals(1, UiPaging.rowsThatFit(142, 176, 50, 2),
                "320x240 codex counterexample must render one safe row, not force two");
        assertEquals(1, UiPaging.rowsThatFit(115, 176, 50, 2));
        assertEquals(3, UiPaging.rowsThatFit(115, 300, 50, 2));
    }

    @Test
    void pagingRemainsStableForEmptyAndPartialPages() {
        assertEquals(1, UiPaging.pageCount(0, 4));
        assertEquals(3, UiPaging.pageCount(9, 4));
        assertEquals(2, UiPaging.clampPage(99, 9, 4));
        assertEquals(0, UiPaging.clampPage(-4, 9, 4));
    }
}
