package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.network.BattleNetworkGateway;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated M2 preflight soak. This does not replace the required real-client 20x playtest,
 * but it proves that repeated network-enabled encounter lifecycles leave no registry ownership behind.
 */
final class M2EncounterSoakTest {
    @Test
    void twentyNetworkEnabledEncountersCompleteAndLeaveZeroOrphans() {
        BattleManager manager = new BattleManager();

        for (int run = 0; run < 20; run++) {
            UUID battleId = UUID.nameUUIDFromBytes(("m2-soak-battle-" + run).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            UUID playerEntity = UUID.nameUUIDFromBytes(("m2-soak-player-" + run).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            UUID enemyEntity = UUID.nameUUIDFromBytes(("m2-soak-enemy-" + run).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            List<BattleParticipant> participants = List.of(
                    new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 120, 24, 10, 50),
                    new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 18, 8, 40));
            BattleInstance battle = new BattleInstance(battleId, 1000L + run, participants);
            manager.register(battle, List.of(
                    new EntityParticipantBinding("p", playerEntity),
                    new EntityParticipantBinding("e", enemyEntity)), participants);
            battle.start();

            BattleNetworkGateway gateway = new BattleNetworkGateway(manager);
            int safetyTurns = 0;
            while (battle.state() != BattleState.REWARD) {
                assertTrue(safetyTurns++ < 30, "soft lock in soak run " + run + " state=" + battle.state());

                if (battle.state() == BattleState.AWAIT_COMMAND) {
                    assertEquals("p", battle.currentActorId());
                    BattleCommand command = new BattleCommand(
                            battle.revision(), "p", "basic", "soak-" + run + "-" + safetyTurns, List.of("e"));
                    BattleNetworkPayloads.DecodedCommand decoded = BattleNetworkPayloads.BattleCommandC2S.of(battleId, command).decode();
                    BattleNetworkGateway.Result result = gateway.submit(playerEntity, decoded);
                    assertTrue(result.accepted(), "run=" + run + " result=" + result.code() + "/" + result.detail());

                    battle.resolveDamage("p", "e", DamageService.DamageRequest.standard(
                            DamageTag.MELEE, AffinityGrade.NORMAL, 90, 18, 24, 8, false));
                    battle.finishResolution();
                } else if (battle.state() == BattleState.RESOLVING) {
                    assertEquals("e", battle.currentActorId());
                    String intentAction = battle.enemyIntent("e").actionId();
                    battle.resolveEnemyStub();
                    if (!"recover".equals(intentAction)) {
                        battle.resolveDamage("e", "p", DamageService.DamageRequest.standard(
                                DamageTag.MELEE, AffinityGrade.NORMAL, 75, 12, 18, 10, false));
                    }
                    battle.finishResolution();
                } else {
                    fail("unexpected non-terminal state in soak run " + run + ": " + battle.state());
                }
            }

            assertEquals(BattleInstance.Outcome.VICTORY, battle.outcome(), "run=" + run);
            assertTrue(manager.commandService(battleId).isPresent());
            assertEquals(1, manager.activeBattleCount());
            assertEquals(2, manager.boundEntityCount());

            battle.cleanup();
            assertEquals(BattleState.NOT_IN_BATTLE, battle.state());
            assertTrue(manager.cleanup(battleId).isPresent());
            assertTrue(manager.cleanup(battleId).isEmpty());
            assertTrue(manager.commandService(battleId).isEmpty());
            assertTrue(manager.battleForEntity(playerEntity).isEmpty());
            assertTrue(manager.battleForEntity(enemyEntity).isEmpty());
            assertEquals(0, manager.activeBattleCount(), "orphan battle after run " + run);
            assertEquals(0, manager.boundEntityCount(), "orphan binding after run " + run);
        }
    }
}
