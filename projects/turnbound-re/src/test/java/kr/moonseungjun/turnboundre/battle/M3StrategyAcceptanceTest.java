package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M3StrategyAcceptanceTest {
    private static final String ZOMBIE = "turnbound_re:zombie";
    private static final String SKELETON = "turnbound_re:skeleton";

    @Test
    void poiseResponseCreatesARealOpportunityThatBasicSpamDoesNot() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        ActionDefinition basic = registry.actions().get("turnbound_re:zombie_rotten_swing");
        ActionDefinition breaker = registry.actions().get("turnbound_re:zombie_gravebreaker");
        assertNotNull(basic);
        assertNotNull(breaker);

        Fixture basicFight = fixture(registry, 99117L);
        BattleActionExecutor basicExecutor = executor(registry);
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                basicFight.battle.submit(new BattleCommand(
                        basicFight.battle.revision(), "p1", basic.id(), "basic", List.of("e1")), basic));
        basicExecutor.execute(basicFight.battle, "p1", basic, List.of("e1"));
        assertFalse(basicFight.battle.combatState("e1").exposed());
        assertEquals("basic", basicFight.battle.enemyIntent("e1").actionId());

        Fixture breakerFight = fixture(registry, 99117L);
        breakerFight.battle.combatState("p1").adjustEnergy(100);
        BattleActionExecutor breakerExecutor = executor(registry);
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                breakerFight.battle.submit(new BattleCommand(
                        breakerFight.battle.revision(), "p1", breaker.id(), "breaker", List.of("e1")), breaker));
        breakerExecutor.execute(breakerFight.battle, "p1", breaker, List.of("e1"));

        assertTrue(breakerFight.battle.combatState("e1").exposed(), "gravebreaker must open the EXPOSED window");
        assertEquals("recover", breakerFight.battle.enemyIntent("e1").actionId(),
                "Poise break must cancel the published basic Intent into RECOVER");
        assertTrue(breakerFight.battle.eventLog().stream().anyMatch(event ->
                "INTENT_CHANGED".equals(event.type()) && event.detail().contains("poise_break_cancel")));
        assertTrue(breakerFight.battle.combatState("e1").poise() < basicFight.battle.combatState("e1").poise());
    }

    private static Fixture fixture(DefinitionRegistry registry, long seed) {
        CharacterDefinition zombie = registry.characters().get(ZOMBIE);
        CharacterDefinition skeleton = registry.characters().get(SKELETON);
        List<BattleParticipant> participants = List.of(
                participant("p1", BattleTeam.PLAYER, 0, zombie, 40),
                participant("e1", BattleTeam.ENEMY, 1, skeleton, skeleton.baseStats().spd()));
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, seed, participants);
        battle.start();
        return new Fixture(battleId, battle);
    }

    private static BattleActionExecutor executor(DefinitionRegistry registry) {
        return new BattleActionExecutor(registry,
                (battleId, participantId) -> "p1".equals(participantId) ? ZOMBIE : SKELETON);
    }

    private static BattleParticipant participant(
            String id, BattleTeam team, int ordinal, CharacterDefinition character, int speed
    ) {
        CharacterDefinition.Stats stats = character.baseStats();
        return new BattleParticipant(id, team, ordinal, speed, stats.hp(), stats.atk(), stats.def(), stats.poise());
    }

    private record Fixture(UUID battleId, BattleInstance battle) {}
}
