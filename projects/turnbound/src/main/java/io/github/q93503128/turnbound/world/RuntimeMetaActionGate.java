package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

/**
 * Minecraft-runtime wrapper around the pure MetaActionGate.
 *
 * <p>Keeping this separate preserves the Minecraft-free rule layer for unit tests while selecting the correct
 * production-world progression policy at the network boundary.</p>
 */
final class RuntimeMetaActionGate {
    private RuntimeMetaActionGate() {}

    static String denial(ServerPlayer player, String rawCommand) {
        if (player == null) return "";
        if (!ExternalWorldBootstrap.active(player)) {
            return MetaActionGate.denial(player.getUUID(), rawCommand);
        }
        if (rawCommand == null || rawCommand.isBlank()) return "";

        String action = rawCommand.split("\\|", -1)[0];
        if ("SUMMON1".equals(action) || "SUMMON10".equals(action) || "STARTER".equals(action)) {
            return DrehmalContentUnlocks.summonUnlocked(player.getUUID())
                    ? ""
                    : "소환은 Capital Valley의 첫 강적 또는 New Drabyel 진입로 전투를 넘은 뒤 개방됩니다.";
        }
        if ("ENHANCE".equals(action)) {
            // First-hub equipment/upgrade is unlocked by reaching the physical New Drabyel blacksmith.
            return "";
        }
        return MetaActionGate.denial(player.getUUID(), rawCommand);
    }
}
