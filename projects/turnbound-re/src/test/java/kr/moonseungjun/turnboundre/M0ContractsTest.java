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
        var invalid = new CharacterDefinition("bad", 6, "UNKNOWN", "VOID", List.of("missing"));
        var errors = DefinitionValidator.validateCharacters(List.of(invalid), Set.of());
        assertTrue(errors.size() >= 4, errors.toString());
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
        var action = new ActionDefinition("turnbound_re:zombie_basic", "BASIC", 0, 2, 10);
        var character = new CharacterDefinition("turnbound_re:zombie", 1, "VANGUARD", "STONE", List.of("turnbound_re:zombie_basic"));
        var registry = DefinitionRegistry.create(List.of(action), List.of(character));
        assertSame(action, registry.actions().get("turnbound_re:zombie_basic"));
        assertSame(character, registry.characters().get("turnbound_re:zombie"));
        assertThrows(UnsupportedOperationException.class, () -> registry.actions().clear());
        assertThrows(IllegalArgumentException.class, () -> DefinitionRegistry.create(
                List.of(action), List.of(new CharacterDefinition("turnbound_re:broken", 1, "VANGUARD", "STONE", List.of("turnbound_re:missing")))));
    }

    @Test void unmappedVanillaMobIsReported() {
        var missing = VanillaMobCoverageValidator.missing(Set.of("minecraft:zombie", "minecraft:skeleton"),
                Map.of("minecraft:zombie", VanillaMobCoverageValidator.Coverage.PLAYABLE));
        assertEquals(List.of("minecraft:skeleton"), missing);
    }
}
