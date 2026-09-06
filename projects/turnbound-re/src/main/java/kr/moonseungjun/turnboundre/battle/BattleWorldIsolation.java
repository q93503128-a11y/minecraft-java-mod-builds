package kr.moonseungjun.turnboundre.battle;

import java.util.Objects;
import java.util.UUID;

/**
 * Server-authoritative policy for normal-world interactions involving an entity
 * currently owned by a TURNBOUND battle.
 *
 * <p>This class deliberately does not mutate vanilla entity flags. Instead,
 * Minecraft/NeoForge event adapters ask this policy whether a normal-world
 * effect should proceed. Cleanup in {@link BattleManager} therefore restores
 * normal behavior immediately without having to reconstruct prior AI, damage,
 * knockback or despawn state.</p>
 */
public final class BattleWorldIsolation {
    private final BattleManager battleManager;

    public BattleWorldIsolation(BattleManager battleManager) {
        this.battleManager = Objects.requireNonNull(battleManager, "battleManager");
    }

    /** Normal vanilla/goal AI must not drive an entity while battle-owned. */
    public boolean allowWorldAi(UUID entityId) {
        return !isBattleOwned(entityId);
    }

    /** Damage outside the deterministic battle resolver must be rejected. */
    public boolean allowWorldDamage(UUID entityId) {
        return !isBattleOwned(entityId);
    }

    /** Vanilla knockback would desync world position from deterministic battle state. */
    public boolean allowWorldKnockback(UUID entityId) {
        return !isBattleOwned(entityId);
    }

    /** Normal despawn/removal policy must not silently orphan a live battle entity. */
    public boolean allowWorldDespawn(UUID entityId) {
        return !isBattleOwned(entityId);
    }

    /**
     * Removal requests for battle-owned entities require lifecycle cleanup first.
     * Adapters can use this to distinguish ordinary despawn from an exceptional
     * removal/disconnect/dimension path that must tear down the battle.
     */
    public boolean requiresBattleCleanupBeforeRemoval(UUID entityId) {
        return isBattleOwned(entityId);
    }

    public boolean isBattleOwned(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId");
        return battleManager.battleForEntity(entityId).isPresent();
    }
}
