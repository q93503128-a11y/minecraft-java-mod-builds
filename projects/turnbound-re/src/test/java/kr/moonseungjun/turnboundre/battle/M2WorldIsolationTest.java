package kr.moonseungjun.turnboundre.battle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class M2WorldIsolationTest {
    private static final UUID PLAYER_ENTITY = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ENEMY_ENTITY = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID UNBOUND_ENTITY = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    void boundEntitiesRejectWorldInteractionsAndCleanupRestoresThem() {
        BattleManager manager = new BattleManager();
        BattleWorldIsolation isolation = new BattleWorldIsolation(manager);
        BattleInstance battle = battle();
        manager.register(battle, List.of(
                new EntityParticipantBinding("p", PLAYER_ENTITY),
                new EntityParticipantBinding("e", ENEMY_ENTITY)
        ));

        for (UUID entityId : List.of(PLAYER_ENTITY, ENEMY_ENTITY)) {
            assertTrue(isolation.isBattleOwned(entityId));
            assertFalse(isolation.allowWorldAi(entityId));
            assertFalse(isolation.allowWorldDamage(entityId));
            assertFalse(isolation.allowWorldKnockback(entityId));
            assertFalse(isolation.allowWorldDespawn(entityId));
            assertTrue(isolation.requiresBattleCleanupBeforeRemoval(entityId));
        }

        assertFalse(isolation.isBattleOwned(UNBOUND_ENTITY));
        assertTrue(isolation.allowWorldAi(UNBOUND_ENTITY));
        assertTrue(isolation.allowWorldDamage(UNBOUND_ENTITY));
        assertTrue(isolation.allowWorldKnockback(UNBOUND_ENTITY));
        assertTrue(isolation.allowWorldDespawn(UNBOUND_ENTITY));
        assertFalse(isolation.requiresBattleCleanupBeforeRemoval(UNBOUND_ENTITY));

        manager.cleanup(battle.battleId());

        for (UUID entityId : List.of(PLAYER_ENTITY, ENEMY_ENTITY)) {
            assertFalse(isolation.isBattleOwned(entityId));
            assertTrue(isolation.allowWorldAi(entityId));
            assertTrue(isolation.allowWorldDamage(entityId));
            assertTrue(isolation.allowWorldKnockback(entityId));
            assertTrue(isolation.allowWorldDespawn(entityId));
            assertFalse(isolation.requiresBattleCleanupBeforeRemoval(entityId));
        }
    }

    private static BattleInstance battle() {
        return new BattleInstance(
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                31337L,
                List.of(
                        new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 100, 20, 10, 30),
                        new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 20, 10, 30)
                )
        );
    }
}
