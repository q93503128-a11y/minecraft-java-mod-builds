package kr.moonseungjun.turnboundre.battle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M5BattleResultManagerContractTest {
    @Test
    void terminalRewardStateAndBindingsRemainServerOwnedUntilCleanup() {
        UUID battleId = UUID.fromString("00000000-0000-0000-0000-000000009201");
        UUID playerEntity = UUID.fromString("00000000-0000-0000-0000-000000009211");
        UUID enemyEntity = UUID.fromString("00000000-0000-0000-0000-000000009212");
        var participants = List.of(
                new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 100, 100, 100, 20),
                new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 100, 100, 20));
        var battle = new BattleInstance(battleId, 9201L, participants);
        var manager = new BattleManager();
        manager.register(battle, List.of(
                new EntityParticipantBinding("e", enemyEntity),
                new EntityParticipantBinding("p", playerEntity)), participants);

        assertEquals(List.of("e", "p"), manager.bindings(battleId).stream()
                .map(EntityParticipantBinding::participantId).toList());
        assertTrue(manager.terminalRewardStateBattleIds().isEmpty());

        battle.start();
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                battle.submit(new BattleCommand(battle.revision(), "p", "basic")));
        battle.resolveDamage("p", "e", new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.NORMAL, 1000, 0, 100, 100,
                false, 0.0D, 1.5D, 1.0D));
        battle.finishResolution();

        assertEquals(BattleState.REWARD, battle.state());
        assertEquals(List.of(battleId), manager.terminalRewardStateBattleIds());
        assertEquals(battle, manager.battleForEntity(playerEntity).orElseThrow());

        battle.cleanup();
        manager.cleanup(battleId);
        assertTrue(manager.terminalRewardStateBattleIds().isEmpty());
        assertTrue(manager.battleForEntity(playerEntity).isEmpty());
    }
}
