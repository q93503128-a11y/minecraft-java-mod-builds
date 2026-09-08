package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleResultNetworkPayloads;

import java.util.Optional;

/** Client-only presentation cache for server-authored terminal battle results. */
public final class BattleResultClientState {
    private static BattleResultNetworkPayloads.ResultView result;
    private static String closeError = "";
    private static long generation;

    private BattleResultClientState() {}

    public static void accept(BattleResultNetworkPayloads.ResultS2C payload) {
        if (payload == null) return;
        result = payload.decode();
        closeError = "";
        generation++;
    }

    public static void accept(BattleResultNetworkPayloads.ResultClosedS2C payload) {
        if (payload == null) return;
        BattleResultNetworkPayloads.DecodedClose close = payload.decode();
        if (result == null || !result.battleId().equals(close.battleId())) return;
        if (close.accepted()) {
            result = null;
            closeError = "";
        } else {
            closeError = close.code();
        }
        generation++;
    }

    public static Optional<BattleResultNetworkPayloads.ResultView> result() {
        return Optional.ofNullable(result);
    }

    public static String closeError() { return closeError; }
    public static long generation() { return generation; }

    public static void clear() {
        result = null;
        closeError = "";
        generation++;
    }
}
