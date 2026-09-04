package io.github.q93503128.turnbound.client;

/** Small layout helper shared by dense management screens. */
final class UiPaging {
    private UiPaging() {}

    static int rowsThatFit(int top, int bottomExclusive, int rowHeight, int minimum) {
        int available = Math.max(0, bottomExclusive - top);
        return Math.max(minimum, Math.max(1, available / Math.max(1, rowHeight)));
    }

    static int pageCount(int total, int perPage) {
        return Math.max(1, (Math.max(0, total) + Math.max(1, perPage) - 1) / Math.max(1, perPage));
    }

    static int clampPage(int page, int total, int perPage) {
        return Math.max(0, Math.min(page, pageCount(total, perPage) - 1));
    }
}