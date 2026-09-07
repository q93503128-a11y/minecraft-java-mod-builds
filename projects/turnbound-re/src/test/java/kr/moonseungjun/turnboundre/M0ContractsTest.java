package kr.moonseungjun.turnboundre;

import kr.moonseungjun.turnboundre.battle.PureBattleHarness;
import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionValidator;
import kr.moonseungjun.turnboundre.data.VanillaMobCoverageValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class M0ContractsTest {
    @Test void sameSeedProducesSameLongEventStream() {
        var fighters = List.of(new PureBattleHarness.Fighter("zombie", 7, 100), new PureBattleHarness.Fighter("skeleton", 9, 80));
        var first = PureBattleHarness.deterministicProbe(42L, fighters, 200);
        var second = PureBattleHarness.deterministicProbe(42L, fighters, 200);
        assertEquals(400, first.size());
        assertEquals(first, second);
        assertEquals(400L, first.getLast().revision());
    }

    @Test void invalidBattleProbeFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> PureBattleHarness.deterministicProbe(1L, List.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> PureBattleHarness.deterministicProbe(1L,
                List.of(new PureBattleHarness.Fighter("bad", -1, 10)), 1));
    }

    @Test void invalidCharacterDefinitionIsRejected() {
        var invalid = new CharacterDefinition(
                "bad", "also_bad", 6, 0, List.of("UNKNOWN"),
                new CharacterDefinition.Stats(0, -1, -1, -1, 0),
                new CharacterDefinition.Growth(-1, 0, 0, 0, 0), Map.of(), Map.of(),
                "missing", List.of(), "missing", List.of(),
                new CharacterDefinition.Availability("", ""), "bad");
        var errors = DefinitionValidator.validateCharacters(List.of(invalid), Set.of());
        assertTrue(errors.size() >= 8, errors.toString());
    }

    @Test void invalidActionAndDuplicateIdsAreRejected() {
        var invalid = new ActionDefinition("turnbound_re:bad", "UNKNOWN", -1, -2, -3);
        var duplicate = new ActionDefinition("turnbound_re:bad", "BASIC", 0, 1, 1);
        var errors = DefinitionValidator.validateActions(List.of(invalid, duplicate));
        assertTrue(errors.stream().anyMatch(it -> it.contains("duplicate action id")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("unknown action kind")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("hpPower")), errors.toString());
        assertTrue(errors.stream().anyMatch(it -> it.contains("poisePower")), errors.toString());
    }

    @Test void definitionRegistryIsValidatedAndImmutable() {
        var basic = action("turnbound_re:zombie_basic", "BASIC");
        var skill = action("turnbound_re:zombie_skill", "SKILL");
        var burst = action("turnbound_re:zombie_burst", "BURST");
        var character = character("turnbound_re:zombie", basic.id(), skill.id(), burst.id());
        var registry = DefinitionRegistry.create(List.of(basic, skill, burst), List.of(character));
        assertSame(basic, registry.actions().get(basic.id()));
        assertSame(character, registry.characters().get(character.id()));
        assertThrows(UnsupportedOperationException.class, () -> registry.actions().clear());

        var broken = character("turnbound_re:broken", basic.id(), "turnbound_re:missing", burst.id());
        assertThrows(IllegalArgumentException.class,
                () -> DefinitionRegistry.create(List.of(basic, skill, burst), List.of(broken)));
    }

    @Test void unmappedVanillaMobIsReported() {
        var missing = VanillaMobCoverageValidator.missing(Set.of("minecraft:zombie", "minecraft:skeleton"),
                Map.of("minecraft:zombie", VanillaMobCoverageValidator.Coverage.PLAYABLE));
        assertEquals(List.of("minecraft:skeleton"), missing);
    }

    private static ActionDefinition action(String id, String kind) {
        int energyDelta = switch (kind) {
            case "BASIC" -> 10;
            case "BURST" -> -100;
            default -> -20;
        };
        return new ActionDefinition(
                id, kind, energyDelta, 90, 20, "MELEE",
                new ActionDefinition.Targeting("ENEMY", "SINGLE", 1), 0,
                List.of(new ActionDefinition.Effect("DAMAGE", "", 1.0D, 0, 1.0D)));
    }

    private static CharacterDefinition character(String id, String basic, String skill, String burst) {
        return new CharacterDefinition(
                id, "minecraft:zombie", 2, 2, List.of("VANGUARD"),
                new CharacterDefinition.Stats(140, 32, 28, 18, 100),
                new CharacterDefinition.Growth(8, 2.1, 1.8, 0.35, 0.5), Map.of(),
                Map.of(
                        "MELEE", "NORMAL", "PROJECTILE", "NORMAL", "FIRE", "WEAK",
                        "BLAST", "NORMAL", "ARCANE", "NORMAL", "VOID", "RESIST"),
                basic, List.of(skill), burst, List.of(),
                new CharacterDefinition.Availability("ENCOUNTER_SHARD", "turnbound_re:test"), id);
    }
}
