package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.EncounterDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Pure projection of authored world encounter anchors into the read-only expedition reference. */
final class ExpeditionJournalProjection {
    private ExpeditionJournalProjection() {}

    static List<ExpeditionNetworkPayloads.EncounterView> encounters(DefinitionRegistry definitions) {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        return definitions.regions().values().stream()
                .flatMap(region -> region.encounterAnchors().stream())
                .map(anchor -> definitions.encounters().get(anchor.encounter()))
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparingInt(EncounterDefinition::difficulty)
                        .thenComparing(EncounterDefinition::id))
                .map(encounter -> new ExpeditionNetworkPayloads.EncounterView(
                        encounter.id(),
                        encounter.difficulty(),
                        encounter.enemies().size(),
                        encounter.repeatable(),
                        encounter.enemies().stream()
                                .map(enemy -> character(definitions, enemy.character()).sourceEntity())
                                .toList()))
                .toList();
    }

    private static CharacterDefinition character(DefinitionRegistry definitions, String characterId) {
        CharacterDefinition character = definitions.characters().get(characterId);
        if (character == null) {
            throw new IllegalStateException("validated encounter references missing character " + characterId);
        }
        return character;
    }
}
