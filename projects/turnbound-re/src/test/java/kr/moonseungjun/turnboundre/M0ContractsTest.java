package kr.moonseungjun.turnboundre;

import kr.moonseungjun.turnboundre.battle.PureBattleHarness;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionValidator;
import kr.moonseungjun.turnboundre.data.VanillaMobCoverageValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class M0ContractsTest {
    @Test void sameSeedProducesSameEventStream() {
        var fighters = List.of(new PureBattleHarness.Fighter("zombie", 7, 100), new PureBattleHarness.Fighter("skeleton", 9, 80));
        assertEquals(PureBattleHarness.deterministicProbe(42L, fighters), PureBattleHarness.deterministicProbe(42L, fighters));
    }

    @Test void invalidCharacterDefinitionIsRejected() {
        var invalid = new CharacterDefinition("bad", 6, "UNKNOWN", "VOID", List.of("missing"));
        var errors = DefinitionValidator.validateCharacters(List.of(invalid), Set.of());
        assertTrue(errors.size() >= 4, errors.toString());
    }

    @Test void unmappedVanillaMobIsReported() {
        var missing = VanillaMobCoverageValidator.missing(Set.of("minecraft:zombie", "minecraft:skeleton"),
                Map.of("minecraft:zombie", VanillaMobCoverageValidator.Coverage.PLAYABLE));
        assertEquals(List.of("minecraft:skeleton"), missing);
    }
}
