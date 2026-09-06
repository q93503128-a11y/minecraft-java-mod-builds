package kr.moonseungjun.turnboundre.battle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class M2BattleManagerTest {
    private static final UUID PLAYER_ENTITY = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ENEMY_ENTITY = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Test
    void registersLookupAndIdempotentCleanupWithoutOrphans() {
        BattleInstance battle = battle("20000000-0000-0000-0000-000000000001");
        BattleManager manager = new BattleManager();
        manager.register(battle, bindings());

        assertSame(battle, manager.battle(battle.battleId()).orElseThrow());
        assertSame(battle, manager.battleForEntity(PLAYER_ENTITY).orElseThrow());
        assertEquals("p", manager.binding(battle.battleId(), "p").orElseThrow().participantId());
        assertEquals(1, manager.activeBattleCount());
        assertEquals(2, manager.boundEntityCount());

        assertSame(battle, manager.cleanup(battle.battleId()).orElseThrow());
        assertTrue(manager.cleanup(battle.battleId()).isEmpty());
        assertEquals(0, manager.activeBattleCount());
        assertEquals(0, manager.boundEntityCount());
        assertTrue(manager.battleForEntity(PLAYER_ENTITY).isEmpty());
        assertTrue(manager.battleForEntity(ENEMY_ENTITY).isEmpty());
    }

    @Test
    void sameEntityCannotJoinTwoLiveBattles() {
        BattleManager manager = new BattleManager();
        BattleInstance first = battle("20000000-0000-0000-0000-000000000002");
        BattleInstance second = battle("20000000-0000-0000-0000-000000000003");
        manager.register(first, bindings());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> manager.register(second, bindings()));
        assertTrue(error.getMessage().contains("already participates"));
        assertEquals(1, manager.activeBattleCount());
        assertEquals(2, manager.boundEntityCount());
    }

    @Test
    void invalidBindingCannotPartiallyMutateRegistry() {
        BattleManager manager = new BattleManager();
        BattleInstance battle = battle("20000000-0000-0000-0000-000000000004");
        var invalid = List.of(
                new EntityParticipantBinding("p", PLAYER_ENTITY),
                new EntityParticipantBinding("missing", ENEMY_ENTITY));

        assertThrows(IllegalArgumentException.class, () -> manager.register(battle, invalid));
        assertEquals(0, manager.activeBattleCount());
        assertEquals(0, manager.boundEntityCount());
    }

    private static BattleInstance battle(String id) {
        return new BattleInstance(UUID.fromString(id), 991L, List.of(
                new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 100, 20, 10, 30),
                new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 20, 10, 30)
        ));
    }

    private static List<EntityParticipantBinding> bindings() {
        return List.of(
                new EntityParticipantBinding("p", PLAYER_ENTITY),
                new EntityParticipantBinding("e", ENEMY_ENTITY));
    }
}
