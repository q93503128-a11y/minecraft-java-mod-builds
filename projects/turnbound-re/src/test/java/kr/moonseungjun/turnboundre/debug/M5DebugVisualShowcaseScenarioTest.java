package kr.moonseungjun.turnboundre.debug;

import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.network.BattleNetworkGateway;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5DebugVisualShowcaseScenarioTest {
    @Test
    void representativeShowcaseRunsBothProductionBurstsAsVirtualControlledActors() throws IOException {
        var parsed = ProductionDefinitionFixture.load();
        UUID controller = UUID.randomUUID();
        DebugVisualShowcaseScenario.Scenario scenario = DebugVisualShowcaseScenario.create(
                parsed.registry(), parsed.hash(), controller);

        assertEquals(5, scenario.participants().size());
        assertEquals(2, scenario.controllers().size());
        assertEquals(controller, scenario.controllers().get(DebugVisualShowcaseScenario.ENDERMAN_ACTOR_ID));
        assertEquals(controller, scenario.controllers().get(DebugVisualShowcaseScenario.SKELETON_ACTOR_ID));
        assertEquals(2, scenario.bursts().size());
        assertEquals("turnbound_re:enderman_horizon_break", scenario.bursts().get(0).actionId());
        assertEquals(1, scenario.bursts().get(0).targetIds().size());
        assertEquals("turnbound_re:skeleton_arrow_storm", scenario.bursts().get(1).actionId());
        assertEquals(3, scenario.bursts().get(1).targetIds().size());

        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 77L, scenario.participants());
        DebugVisualShowcaseScenario.prepareBattle(battle, scenario);
        BattleManager manager = new BattleManager();
        manager.register(
                battle,
                List.of(),
                scenario.participants(),
                scenario.context(),
                null,
                scenario.controllers());

        assertTrue(manager.bindings(battleId).isEmpty());
        assertTrue(manager.battleForEntity(controller).isEmpty());
        assertEquals(battleId, manager.battleForController(controller).orElseThrow().battleId());

        battle.start();
        BattleNetworkGateway gateway = new BattleNetworkGateway(manager);
        for (DebugVisualShowcaseScenario.BurstPlan plan : scenario.bursts()) {
            assertEquals(plan.actorId(), battle.currentActorId());
            BattleCommand command = new BattleCommand(
                    battle.revision(),
                    plan.actorId(),
                    plan.actionId(),
                    "showcase-test-" + UUID.randomUUID(),
                    plan.targetIds());
            BattleNetworkGateway.Result result = gateway.submit(
                    controller,
                    BattleNetworkPayloads.BattleCommandC2S.of(battleId, command).decode());
            assertTrue(result.accepted(), () -> result.code() + " / " + result.detail());
        }

        assertEquals(0, battle.combatState(DebugVisualShowcaseScenario.ENDERMAN_ACTOR_ID).energy());
        assertEquals(0, battle.combatState(DebugVisualShowcaseScenario.SKELETON_ACTOR_ID).energy());

        List<BattleEvent> presentation = battle.eventLog().stream()
                .filter(event -> "ACTION_PRESENTATION".equals(event.type()))
                .toList();
        assertEquals(2, presentation.size());
        assertEquals(DebugVisualShowcaseScenario.ENDERMAN_ACTOR_ID, presentation.get(0).actorId());
        assertTrue(presentation.get(0).detail().contains("action=turnbound_re:enderman_horizon_break"));
        assertEquals(DebugVisualShowcaseScenario.SKELETON_ACTOR_ID, presentation.get(1).actorId());
        assertTrue(presentation.get(1).detail().contains("action=turnbound_re:skeleton_arrow_storm"));
        assertTrue(presentation.get(1).detail().contains("targets=showcase_target_1,showcase_target_2,showcase_target_3"));
    }
}
