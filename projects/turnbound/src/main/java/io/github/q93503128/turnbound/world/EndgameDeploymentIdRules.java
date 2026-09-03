package io.github.q93503128.turnbound.world;

/**
 * Pure identifier rules for the Radia replay/endgame deployment surface.
 *
 * <p>This class deliberately has no Minecraft/NeoForge dependencies so the contract can be unit-tested on the
 * ordinary JVM test runtime. The server-facing {@link EndgameDeploymentService} additionally validates identifiers
 * against the canonical encounter catalogs before granting any authority.</p>
 */
final class EndgameDeploymentIdRules {
    private EndgameDeploymentIdRules() { }

    static boolean normalBossRematch(String encounterId) {
        return encounterId != null && encounterId.matches("BATTLE_B0[1-5]");
    }

    static boolean hardBoss(String encounterId) {
        return encounterId != null && encounterId.matches("HARD_B0[1-5]");
    }

    static boolean rift(String encounterId) {
        if (encounterId == null || !encounterId.matches("RIFT_F\\d{2}")) return false;
        try {
            int floor = Integer.parseInt(encounterId.substring("RIFT_F".length()));
            return floor >= 1 && floor <= 30;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    static boolean supported(String encounterId) {
        return normalBossRematch(encounterId) || hardBoss(encounterId) || rift(encounterId);
    }
}
