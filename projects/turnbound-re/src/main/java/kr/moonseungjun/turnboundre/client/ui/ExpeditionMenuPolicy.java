package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.network.ExpeditionNetworkPayloads;

/** Pure UI policy for the server-authoritative expedition launch handshake. */
final class ExpeditionMenuPolicy {
    private ExpeditionMenuPolicy() {}

    static boolean canSelectEncounter(ExpeditionNetworkPayloads.JournalView view, boolean startPending) {
        return view != null && !view.party().isEmpty() && !startPending;
    }

    static boolean shouldEnterBattle(boolean startPending, boolean authoritativeBattlePresent) {
        return startPending && authoritativeBattlePresent;
    }
}
