package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.server.MinecraftServer;

/**
 * One server-side gate for the shared construction authority. UI/network/commands may pre-check,
 * but they are never trusted as the final invariant: every project service reuses this gate before
 * preview/start mutation so stale previews or future direct callers cannot create parallel projects.
 */
public final class SettlementProjectAuthority {
    private SettlementProjectAuthority() {}

    public static boolean anyActive(MinecraftServer server, SettlementData data) {
        return data.construction().active()
                || data.roadConstruction().active()
                || data.outpostConstruction().active()
                || SettlementCivilWorkData.get(server).project().active();
    }
}
