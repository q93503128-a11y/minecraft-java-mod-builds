package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.server.level.ServerPlayer;

/** @deprecated WorldMagicTracker now renders all casting seals as multiplayer world geometry. */
@Deprecated
public final class SpellSigilService {
    public static final int CHARGE_STAGES = 5;
    private SpellSigilService() {}
    public static void renderChargeStep(ServerPlayer player, SpellDefinition spell, double effectiveRange, int step) {}
    public static void renderRelease(ServerPlayer player, SpellDefinition spell, double effectiveRange) {}
}
