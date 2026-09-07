package kr.moonseungjun.turnboundre.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class M3DefinitionCrossReferenceTest {
    @Test
    void characterSlotsRejectWrongActionKindsBeforeContentCanEnterRegistry() {
        var basic = action("turnbound_re:basic", "SKILL", List.of());
        var skill = action("turnbound_re:skill", "BASIC", List.of());
        var burst = action("turnbound_re:burst", "SKILL", List.of());
        var passive = action("turnbound_re:passive", "BURST", List.of());
        var character = character(basic.id(), skill.id(), burst.id(), passive.id());

        var errors = DefinitionCrossReferenceValidator.validate(
                List.of(basic, skill, burst, passive), List.of(character), List.of());

        assertTrue(errors.stream().anyMatch(it -> it.contains("basicAction requires BASIC")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("skills requires SKILL")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("burst requires BURST")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("passives requires PASSIVE")), errors.toString());
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> DefinitionRegistry.create(List.of(basic, skill, burst, passive), List.of(character)));
        assertTrue(thrown.getMessage().contains("basicAction requires BASIC"));
    }

    @Test
    void statusEffectsMustReferenceLoadedCanonicalStatusDefinitions() {
        var effect = new ActionDefinition.Effect(
                "APPLY_STATUS", "turnbound_re:burn", 0.0D, 2, 1.0D);
        var action = action("turnbound_re:burn_skill", "SKILL", List.of(effect));

        var missing = DefinitionCrossReferenceValidator.validate(List.of(action), List.of(), List.of());
        assertTrue(missing.stream().anyMatch(it -> it.contains("references missing status turnbound_re:burn")), missing.toString());

        var burn = new StatusDefinition(
                "turnbound_re:burn", "NEGATIVE", "TURN", 2, 3,
                "REFRESH_DURATION", List.of("DEBUFF", "FIRE"),
                List.of(new StatusDefinition.Hook(
                        "TURN_END", new StatusDefinition.Effect("DAMAGE_MAX_HP_PERCENT", 0.03D))));
        assertTrue(DefinitionCrossReferenceValidator.validate(List.of(action), List.of(), List.of(burn)).isEmpty());
    }

    @Test
    void healEffectsCannotSilentlyResolveToZero() {
        var inertHeal = new ActionDefinition(
                "turnbound_re:inert_heal", "SKILL", -20, 0, 0, "ARCANE",
                new ActionDefinition.Targeting("ALLY", "SINGLE", 1), 0,
                List.of(new ActionDefinition.Effect("HEAL", "", 1.0D, 0, 1.0D)));

        var errors = DefinitionCrossReferenceValidator.validate(List.of(inertHeal), List.of(), List.of());
        assertTrue(errors.stream().anyMatch(it -> it.contains("HEAL effect requires hpPower > 0")), errors.toString());
    }

    private static ActionDefinition action(String id, String kind, List<ActionDefinition.Effect> effects) {
        int energyDelta = switch (kind) {
            case "BASIC" -> 10;
            case "SKILL" -> -20;
            case "BURST" -> -100;
            default -> 0;
        };
        return new ActionDefinition(
                id, kind, energyDelta, 50, 10, "MELEE",
                new ActionDefinition.Targeting("ENEMY", "SINGLE", 1), 0, effects);
    }

    private static CharacterDefinition character(String basic, String skill, String burst, String passive) {
        return new CharacterDefinition(
                "turnbound_re:tester", "minecraft:zombie", 2, 2, List.of("VANGUARD"),
                new CharacterDefinition.Stats(100, 20, 20, 20, 30),
                new CharacterDefinition.Growth(5, 1, 1, 0.2, 0.5), Map.of(),
                Map.of(
                        "MELEE", "NORMAL", "PROJECTILE", "NORMAL", "FIRE", "NORMAL",
                        "BLAST", "NORMAL", "ARCANE", "NORMAL", "VOID", "NORMAL"),
                basic, List.of(skill), burst, List.of(passive),
                new CharacterDefinition.Availability("DEBUG", "turnbound_re:test"), "turnbound_re:tester");
    }
}
