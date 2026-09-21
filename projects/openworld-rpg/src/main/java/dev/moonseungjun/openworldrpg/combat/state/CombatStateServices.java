package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;

/**
 * One server-owned access point for transient combat state.
 */
public final class CombatStateServices {
    private static final PlayerCombatStateStore STATES = new PlayerCombatStateStore();

    private CombatStateServices() {
    }

    public static PlayerCombatStateStore states() {
        return STATES;
    }

    public static void markCombatActivity(UUID playerId, long gameTick) {
        STATES.getOrCreate(playerId, gameTick).markCombatActivity(gameTick);
    }

    public static void disconnect(UUID playerId) {
        STATES.remove(playerId);
    }
}
