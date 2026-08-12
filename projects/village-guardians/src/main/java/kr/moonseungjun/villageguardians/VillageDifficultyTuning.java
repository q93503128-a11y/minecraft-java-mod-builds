package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;

/** Centralized raid difficulty curve shared by combat, structures and previews. */
public final class VillageDifficultyTuning {
    private VillageDifficultyTuning() {}

    /**
     * Early nights are deliberately forgiving so a fresh solo player is not deleted in three hits.
     * The protection fades smoothly and reaches the normal late-game curve on day 12.
     */
    public static float playerDamageMultiplier(int day) {
        int d = Math.max(1, day);
        if (d >= 12) return 1.0f;
        return Math.min(1.0f, 0.56f + (d - 1) * 0.04f);
    }

    /** Early structures get breathing room; by day 10 the campaign uses the full structure damage. */
    public static float earlyStructureMultiplier(int day) {
        int d = Math.max(1, day);
        if (d >= 10) return 1.0f;
        return Math.min(1.0f, 0.52f + (d - 1) * 0.06f);
    }

    /**
     * Death must keep its strategic cost. Downed/spectating defenders never receive a structure-damage discount.
     * Kept as a compatibility hook for the raid call site until that large subsystem is next refactored.
     */
    public static float defenderStateStructureMultiplier(MinecraftServer server) {
        return 1.0f;
    }

    /** One player is the baseline. Every additional player adds exactly 30% of the solo roster. */
    public static int scaleEnemyCount(int soloCount, int players) {
        int safeSolo = Math.max(1, soloCount);
        int extraPlayers = Math.max(0, players - 1);
        float multiplier = 1.0f + extraPlayers * 0.30f;
        return Math.max(1, Math.round(safeSolo * multiplier));
    }
}
