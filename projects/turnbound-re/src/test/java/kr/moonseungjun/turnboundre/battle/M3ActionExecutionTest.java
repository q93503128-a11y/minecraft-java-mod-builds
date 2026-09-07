package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.StatusDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M3ActionExecutionTest {
    private static final String BASIC = "turnbound_re:test_basic";
    private static final String SKILL = "turnbound_re:test_skill";
    private static final String BURST = "turnbound_re:test_burst";
    private static final String PASSIVE = "turnbound_re:test_passive";
    private static final String HERO = "turnbound_re:test_hero";
    private static final String TARGET = "turnbound_re:test_target";

    @Test
    void damageStatusTurnTicksAndExpiryExecuteDeterministically() {
        StatusDefinition burn = new StatusDefinition(
                "turnbound_re:test_burn", "NEGATIVE", "TURN", 2, 3, "ADD_STACK", List.of("DEBUFF"),
                List.of(new StatusDefinition.Hook("TURN_END",
                        new StatusDefinition.Effect("DAMAGE_MAX_HP_PERCENT", 0.03D))));
        ActionDefinition skill = new ActionDefinition(
                SKILL, "SKILL", 0, 80, 0, "FIRE",
                new ActionDefinition.Targeting("ENEMY", "SINGLE", 1), 0,
                List.of(
                        new ActionDefinition.Effect("DAMAGE", "", 1.0D, 0, 1.0D),
                        new ActionDefinition.Effect("APPLY_STATUS", burn.id(), 0.0D, 2, 1.0D)));
        DefinitionRegistry registry = registry(skill, List.of(burn));
        Fixture f = fixture(registry);
        BattleActionExecutor executor = executor(registry, f.battleId);

        int beforeHp = f.battle.combatState("e1").hp();
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                f.battle.submit(new BattleCommand(f.battle.revision(), "p1", SKILL, "cmd-1", List.of("e1")), skill));
        executor.execute(f.battle, "p1", skill, List.of("e1"));
        assertTrue(f.battle.combatState("e1").hp() < beforeHp);
        assertEquals(2, f.battle.combatState("e1").statuses().remaining(burn.id()));
        f.battle.finishResolution();

        int afterDirectHit = f.battle.combatState("e1").hp();
        f.battle.resolveEnemyStub();
        f.battle.finishResolution();
        assertEquals(afterDirectHit - 3, f.battle.combatState("e1").hp());
        assertEquals(1, f.battle.combatState("e1").statuses().remaining(burn.id()));

        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                f.battle.submit(new BattleCommand(f.battle.revision(), "p1", "guard", "cmd-2", List.of("p1"))));
        f.battle.finishResolution();
        int beforeSecondTick = f.battle.combatState("e1").hp();
        f.battle.resolveEnemyStub();
        f.battle.finishResolution();
        assertEquals(beforeSecondTick - 3, f.battle.combatState("e1").hp());
        assertFalse(f.battle.combatState("e1").statuses().has(burn.id()));
        assertTrue(f.battle.eventLog().stream().anyMatch(e -> "STATUS_EXPIRED".equals(e.type()) && e.detail().contains(burn.id())));
    }

    @Test
    void healAndIntentDelayMutateBattleStateInsteadOfRemainingJsonOnly() {
        ActionDefinition heal = new ActionDefinition(
                SKILL, "SKILL", 0, 100, 0, "ARCANE",
                new ActionDefinition.Targeting("SELF", "SINGLE", 1), 0,
                List.of(new ActionDefinition.Effect("HEAL", "", 1.0D, 0, 1.0D)));
        DefinitionRegistry healRegistry = registry(heal, List.of());
        Fixture healFixture = fixture(healRegistry);
        healFixture.battle.combatState("p1").applyHpDamage(40);
        BattleActionExecutor healExecutor = executor(healRegistry, healFixture.battleId);
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                healFixture.battle.submit(new BattleCommand(
                        healFixture.battle.revision(), "p1", SKILL, "heal", List.of("p1")), heal));
        healExecutor.execute(healFixture.battle, "p1", heal, List.of("p1"));
        assertEquals(100, healFixture.battle.combatState("p1").hp());
        assertTrue(healFixture.battle.eventLog().stream().anyMatch(e -> "HEAL".equals(e.type())));

        ActionDefinition delay = new ActionDefinition(
                SKILL, "SKILL", 0, 0, 0, "ARCANE",
                new ActionDefinition.Targeting("ENEMY", "SINGLE", 1), 0,
                List.of(new ActionDefinition.Effect("INTENT_DELAY", "", 0.0D, 1, 1.0D)));
        DefinitionRegistry delayRegistry = registry(delay, List.of());
        Fixture delayFixture = fixture(delayRegistry);
        BattleActionExecutor delayExecutor = executor(delayRegistry, delayFixture.battleId);
        assertEquals("basic", delayFixture.battle.enemyIntent("e1").actionId());
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                delayFixture.battle.submit(new BattleCommand(
                        delayFixture.battle.revision(), "p1", SKILL, "delay", List.of("e1")), delay));
        delayExecutor.execute(delayFixture.battle, "p1", delay, List.of("e1"));
        assertEquals("recover", delayFixture.battle.enemyIntent("e1").actionId());
        assertTrue(delayFixture.battle.eventLog().stream().anyMatch(e ->
                "INTENT_CHANGED".equals(e.type()) && e.detail().contains("action_delay")));
    }

    @Test
    void dataStatusRefreshRulesAndContinuousMultipliersAreRuntimeData() {
        StatusDefinition ward = new StatusDefinition(
                "turnbound_re:test_ward", "POSITIVE", "TURN", 2, 1, "REFRESH_DURATION", List.of("BUFF"),
                List.of(new StatusDefinition.Hook("ON_DAMAGE_TAKEN",
                        new StatusDefinition.Effect("DAMAGE_TAKEN_MULTIPLIER", 0.8D))));
        StatusDefinition venom = new StatusDefinition(
                "turnbound_re:test_venom", "NEGATIVE", "TURN", 3, 3, "ADD_STACK", List.of("DEBUFF"), List.of());
        StatusRuntime runtime = new StatusRuntime();
        StatusService.apply(runtime, ward, 1);
        assertEquals(0.8D, runtime.multiplier("DAMAGE_TAKEN_MULTIPLIER"), 1.0e-9);
        assertEquals(2, runtime.remaining(ward.id()));
        assertTrue(runtime.tick("TURN").isEmpty());
        assertEquals(1, runtime.remaining(ward.id()));
        StatusService.apply(runtime, ward, 1);
        assertEquals(2, runtime.remaining(ward.id()));

        StatusService.apply(runtime, venom, 1);
        StatusService.apply(runtime, venom, 1);
        assertEquals(2, runtime.stacks(venom.id()));
        assertEquals(3, runtime.remaining(venom.id()));
    }

    private static Fixture fixture(DefinitionRegistry registry) {
        UUID battleId = UUID.randomUUID();
        List<BattleParticipant> participants = List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 20, 100, 50, 10, 50),
                new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 100, 20, 10, 50));
        BattleInstance battle = new BattleInstance(battleId, 20260907L, participants);
        battle.start();
        return new Fixture(battleId, battle);
    }

    private static BattleActionExecutor executor(DefinitionRegistry registry, UUID battleId) {
        return new BattleActionExecutor(registry,
                (id, participantId) -> "p1".equals(participantId) ? HERO : TARGET);
    }

    private static DefinitionRegistry registry(ActionDefinition skill, List<StatusDefinition> statuses) {
        ActionDefinition basic = new ActionDefinition(
                BASIC, "BASIC", 10, 10, 0, "MELEE",
                new ActionDefinition.Targeting("ENEMY", "SINGLE", 1), 0,
                List.of(new ActionDefinition.Effect("DAMAGE", "", 1.0D, 0, 1.0D)));
        ActionDefinition burst = new ActionDefinition(
                BURST, "BURST", -100, 10, 0, "MELEE",
                new ActionDefinition.Targeting("ENEMY", "SINGLE", 1), 0,
                List.of(new ActionDefinition.Effect("DAMAGE", "", 1.0D, 0, 1.0D)));
        ActionDefinition passive = new ActionDefinition(
                PASSIVE, "PASSIVE", 0, 0, 0, "MELEE",
                new ActionDefinition.Targeting("SELF", "SINGLE", 1), 0,
                List.of(new ActionDefinition.Effect("NONE", "", 0.0D, 0, 1.0D)));
        CharacterDefinition hero = character(HERO, BASIC, SKILL, BURST, PASSIVE);
        CharacterDefinition target = character(TARGET, BASIC, SKILL, BURST, PASSIVE);
        return DefinitionRegistry.create(List.of(basic, skill, burst, passive), List.of(hero, target), statuses);
    }

    private static CharacterDefinition character(String id, String basic, String skill, String burst, String passive) {
        return new CharacterDefinition(
                id, "minecraft:zombie", 3, 3, List.of("STRIKER"),
                new CharacterDefinition.Stats(100, 50, 10, 20, 50),
                new CharacterDefinition.Growth(1, 1, 1, 0.1, 0.1), Map.of(),
                Map.of(
                        "MELEE", "NORMAL", "PROJECTILE", "NORMAL", "FIRE", "NORMAL",
                        "BLAST", "NORMAL", "ARCANE", "NORMAL", "VOID", "NORMAL"),
                basic, List.of(skill), burst, List.of(passive),
                new CharacterDefinition.Availability("DEBUG", "turnbound_re:test"), id);
    }

    private record Fixture(UUID battleId, BattleInstance battle) {}
}
