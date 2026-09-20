package io.github.q93503128.turnbound.client;

/** The server already owns the readable 3D outcome outro; the result screen only needs a short handoff beat. */
final class BattleResultTiming {
    private BattleResultTiming() {}

    static int revealTicks(String outcome) {
        return "ALLY_VICTORY".equals(outcome) ? 4 : 2;
    }
}
