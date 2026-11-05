package io.github.q93503128.turnbound.client;

/** Small layout helper shared by dense management screens. */
final class UiPaging {
    private UiPaging() {}

    static int rowsThatFit(int top, int bottomExclusive, int rowHeight, int preferredMinimum) {
        int available = Math.max(0, bottomExclusive - top);
        int fit = available / Math.max(1, rowHeight);
        // A preferred row count is only a density hint. It must never create controls outside the viewport.
        return Math.max(1, fit);
    }

    static int pageCount(int total, int perPage) {
        return Math.max(1, (Math.max(0, total) + Math.max(1, perPage) - 1) / Math.max(1, perPage));
    }

    static int clampPage(int page, int total, int perPage) {
        return Math.max(0, Math.min(page, pageCount(total, perPage) - 1));
    }
}