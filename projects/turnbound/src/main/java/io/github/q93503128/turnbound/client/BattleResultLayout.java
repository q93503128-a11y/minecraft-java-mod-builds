package io.github.q93503128.turnbound.client;

/** Pure result-screen geometry decisions so compact GUI scales can be regression-tested without a live client. */
final class BattleResultLayout {
    static final int FOOTER_RESERVE = 34;
    static final int FULL_ROW_HEIGHT = 28;
    static final int COMPACT_CELL_HEIGHT = 25;

    private BattleResultLayout() {}

    static boolean useCompactPartyGrid(int panelHeight, int partyRowTopOffset, int partyCount) {
        if (partyCount <= 2) return false;
        int available = Math.max(0, panelHeight - FOOTER_RESERVE - partyRowTopOffset);
        return partyCount * FULL_ROW_HEIGHT > available;
    }

    static int partyBlockHeight(int partyCount, boolean compact) {
        int count = Math.max(0, Math.min(4, partyCount));
        if (!compact) return count * FULL_ROW_HEIGHT;
        return ((count + 1) / 2) * COMPACT_CELL_HEIGHT;
    }

    static int estimatedPartyHeight(int viewportHeight, int partyCount) {
        boolean compact = viewportHeight < 300 && partyCount > 2;
        return partyBlockHeight(partyCount, compact);
    }
}
