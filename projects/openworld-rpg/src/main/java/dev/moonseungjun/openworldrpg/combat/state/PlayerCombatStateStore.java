package dev.moonseungjun.openworldrpg.combat.state;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-owned state registry for the M0 combat domain.
 *
 * <p>Level-one initialization is canonical (WIL 5). Save serialization is a later dedicated gate;
 * the domain object is deliberately independent from that persistence transport.</p>
 */
public final class PlayerCombatStateStore {
    public static final int LEVEL_ONE_BASE_WILL = 5;

    private final ConcurrentHashMap<UUID, PlayerCombatState> states = new ConcurrentHashMap<>();

    public PlayerCombatState getOrCreate(UUID playerId, long nowTick) {
        return states.computeIfAbsent(playerId, ignored -> new PlayerCombatState(LEVEL_ONE_BASE_WILL, nowTick));
    }

    public void synchronizeWill(UUID playerId, int will, long nowTick) {
        getOrCreate(playerId, nowTick).synchronizeWill(will, nowTick);
    }

    public void synchronizeEndurance(UUID playerId, int endurance, long nowTick) {
        getOrCreate(playerId, nowTick).synchronizeEndurance(endurance, nowTick);
    }


    public void synchronizeResourceModifiers(
            UUID playerId,
            EquipmentResourceModifiers modifiers,
            long nowTick
    ) {
        getOrCreate(playerId, nowTick)
                .synchronizeResourceModifiers(modifiers, nowTick);
    }

    public void synchronizeClassSkillManaCostMultiplier(
            UUID playerId,
            double multiplier,
            long nowTick
    ) {
        getOrCreate(playerId, nowTick)
                .synchronizeClassSkillManaCostMultiplier(
                        multiplier,
                        nowTick
                );
    }

    public void synchronizeClassManaFlatBonus(
            UUID playerId,
            int maxManaFlatBonus,
            long nowTick
    ) {
        getOrCreate(playerId, nowTick)
                .synchronizeClassManaFlatBonus(
                        maxManaFlatBonus,
                        nowTick
                );
    }

    public void synchronizeClassStaminaModifiers(
            UUID playerId,
            int maxStaminaFlatBonus,
            double staminaRecoveryBonus,
            long nowTick
    ) {
        getOrCreate(playerId, nowTick)
                .synchronizeClassStaminaModifiers(
                        maxStaminaFlatBonus,
                        staminaRecoveryBonus,
                        nowTick
                );
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }

    public int size() {
        return states.size();
    }
}
