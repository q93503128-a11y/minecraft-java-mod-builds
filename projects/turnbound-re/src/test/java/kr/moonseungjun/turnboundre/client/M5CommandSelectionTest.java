package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M5CommandSelectionTest {
    @Test
    void productionSnapshotPublishesExactServerTargetCandidatesAndBuildsStrictCommand() throws Exception {
        var parsed = ProductionDefinitionFixture.load();
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 8899L, List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 30, 100, 10, 5, 30),
                new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 100, 10, 5, 30),
                new BattleParticipant("e2", BattleTeam.ENEMY, 2, 11, 100, 10, 5, 30),
                new BattleParticipant("e3", BattleTeam.ENEMY, 3, 12, 100, 10, 5, 30)));
        BattleDefinitionContext context = new BattleDefinitionContext(
                parsed.registry(), parsed.hash(),
                Map.of(
                        "p1", "turnbound_re:skeleton",
                        "e1", "turnbound_re:zombie",
                        "e2", "turnbound_re:zombie",
                        "e3", "turnbound_re:zombie"));
        battle.start();
        battle.combatState("p1").gainEnergy(100);

        BattlePresentationModel model = BattlePresentationModel.from(
                BattleNetworkPayloads.BattleSnapshotS2C.from(battle, context).decode());
        BattleNetworkPayloads.SnapshotAction basic = bySlot(model, "BASIC");
        BattleNetworkPayloads.SnapshotAction guard = bySlot(model, "GUARD");
        BattleNetworkPayloads.SnapshotAction burst = bySlot(model, "BURST");

        assertEquals(List.of("e1", "e2", "e3"), basic.eligibleTargetIds());
        assertEquals(List.of("p1"), guard.eligibleTargetIds());
        assertEquals(List.of("e1", "e2", "e3"), burst.eligibleTargetIds());
        assertTrue(burst.usable());
        assertEquals(3, burst.targetCount());

        assertEquals(List.of("p1"), BattleCommandSelection.forcedTargetsIfUnambiguous(model, guard));
        assertEquals(List.of("e1", "e2", "e3"), BattleCommandSelection.forcedTargetsIfUnambiguous(model, burst));
        assertTrue(BattleCommandSelection.forcedTargetsIfUnambiguous(model, basic).isEmpty());

        BattleCommand command = BattleCommandSelection.buildCommand(
                model, burst, List.of("e1", "e2", "e3"), "ui:test:burst");
        assertEquals(battle.revision(), command.expectedRevision());
        assertEquals("p1", command.actorId());
        assertEquals(burst.id(), command.actionId());
        assertEquals(List.of("e1", "e2", "e3"), command.targetIds());

        assertThrows(IllegalArgumentException.class, () -> BattleCommandSelection.buildCommand(
                model, burst, List.of("e1", "e2"), "ui:test:too-few"));
    }

    @Test
    void clientCannotInventTargetFromTeamWhenServerDidNotPublishItsId() {
        UUID battleId = UUID.randomUUID();
        BattleNetworkPayloads.SnapshotParticipant player = participant("p1", "PLAYER", 0);
        BattleNetworkPayloads.SnapshotParticipant enemyOne = participant("e1", "ENEMY", 1);
        BattleNetworkPayloads.SnapshotParticipant enemyTwo = participant("e2", "ENEMY", 2);
        BattleNetworkPayloads.SnapshotAction action = new BattleNetworkPayloads.SnapshotAction(
                "turnbound_re:test_action", "BASIC", "BASIC", 0, 10, 0, "PHYSICAL",
                "ENEMY", "SINGLE", 1, true, "", List.of("e1"));
        BattlePresentationModel model = BattlePresentationModel.from(new BattleNetworkPayloads.DecodedSnapshot(
                battleId, 7L, "AWAIT_COMMAND", 1, "p1",
                List.of(player, enemyOne, enemyTwo), List.of(action)));

        assertEquals(List.of("e1"), BattleCommandSelection.eligibleTargets(model, action).stream()
                .map(BattleNetworkPayloads.SnapshotParticipant::id).toList());
        assertTrue(BattleCommandSelection.validate(model, action, List.of("e1")).ready());
        assertEquals(BattleCommandSelection.ValidationCode.TARGET_NOT_ELIGIBLE,
                BattleCommandSelection.validate(model, action, List.of("e2")).code());
        assertThrows(IllegalArgumentException.class, () -> BattleCommandSelection.buildCommand(
                model, action, List.of("e2"), "ui:test:invented"));
    }

    private static BattleNetworkPayloads.SnapshotAction bySlot(BattlePresentationModel model, String slot) {
        return model.availableActions().stream()
                .filter(action -> slot.equals(action.slot()))
                .findFirst()
                .orElseThrow();
    }

    private static BattleNetworkPayloads.SnapshotParticipant participant(String id, String team, int ordinal) {
        return new BattleNetworkPayloads.SnapshotParticipant(
                id, 100, 100, 20, 20, 0,
                false, false, false, true,
                team, ordinal, "", List.of(), null);
    }
}
