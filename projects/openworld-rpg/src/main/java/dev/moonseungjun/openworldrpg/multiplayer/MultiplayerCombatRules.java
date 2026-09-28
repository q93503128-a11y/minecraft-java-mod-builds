package dev.moonseungjun.openworldrpg.multiplayer;

/** Canonical multiplayer combat constants shared by R01 and later regions. */
public final class MultiplayerCombatRules {
    public static final int MIN_FORMAL_PARTY_SIZE = 2;
    public static final int MAX_FORMAL_PARTY_SIZE = 4;
    public static final boolean DIRECT_PLAYER_PVP_ENABLED = false;

    private MultiplayerCombatRules() {
    }

    public static boolean validFormalPartySize(int players) {
        return players >= MIN_FORMAL_PARTY_SIZE
                && players <= MAX_FORMAL_PARTY_SIZE;
    }

    public static double bossHpScale(int engagedPlayers) {
        requireEngagedPlayers(engagedPlayers);
        if (engagedPlayers <= 4) {
            return 1.0 + 0.65 * (engagedPlayers - 1);
        }
        return 2.95 + 0.45 * (engagedPlayers - 4);
    }

    public static double bossPoiseScale(int engagedPlayers) {
        requireEngagedPlayers(engagedPlayers);
        if (engagedPlayers <= 4) {
            return 1.0 + 0.40 * (engagedPlayers - 1);
        }
        return 2.20 + 0.30 * (engagedPlayers - 4);
    }

    public static double ordinaryEnemyHpScale(int nearbyOrOnlinePlayers) {
        if (nearbyOrOnlinePlayers < 1) {
            throw new IllegalArgumentException(
                    "Ordinary-enemy player context must contain at least one player."
            );
        }
        return 1.0;
    }

    private static void requireEngagedPlayers(int engagedPlayers) {
        if (engagedPlayers < 1) {
            throw new IllegalArgumentException(
                    "Engaged-player count must be at least one."
            );
        }
    }
}
