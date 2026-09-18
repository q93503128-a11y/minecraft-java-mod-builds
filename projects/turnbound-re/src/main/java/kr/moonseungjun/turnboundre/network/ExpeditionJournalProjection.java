package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.EncounterDefinition;
import kr.moonseungjun.turnboundre.data.ExternalWorldProfileDefinition;
import kr.moonseungjun.turnboundre.data.RegionDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Projects enabled authored-world encounter routes into the Expedition Journal without granting menu combat entry. */
final class ExpeditionJournalProjection {
    private record RouteBinding(String dimension, ExternalWorldProfileDefinition.Anchor anchor) {}

    private ExpeditionJournalProjection() {}

    static List<ExpeditionNetworkPayloads.EncounterView> encounters(DefinitionRegistry definitions) {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");

        List<ExpeditionNetworkPayloads.EncounterView> out = new ArrayList<>();
        for (RegionDefinition region : definitions.regions().values()) {
            for (RegionDefinition.EncounterAnchor anchor : region.encounterAnchors()) {
                RouteBinding route = enabledRoute(definitions, region.dimension(), anchor.locator());
                if (route == null) continue;

                EncounterDefinition encounter = definitions.encounters().get(anchor.encounter());
                if (encounter == null) continue;
                List<String> sources = encounter.enemies().stream()
                        .map(enemy -> character(definitions, enemy.character()).sourceEntity())
                        .toList();
                out.add(new ExpeditionNetworkPayloads.EncounterView(
                        encounter.id(),
                        encounter.difficulty(),
                        encounter.enemies().size(),
                        encounter.repeatable(),
                        sources,
                        anchor.locator(),
                        route.dimension(),
                        route.anchor().x(),
                        route.anchor().y(),
                        route.anchor().z()));
            }
        }

        return out.stream()
                .sorted(Comparator.comparingInt(ExpeditionNetworkPayloads.EncounterView::difficulty)
                        .thenComparing(ExpeditionNetworkPayloads.EncounterView::id)
                        .thenComparing(ExpeditionNetworkPayloads.EncounterView::locator))
                .toList();
    }

    private static RouteBinding enabledRoute(DefinitionRegistry definitions, String dimension, String locator) {
        return definitions.externalWorldProfiles().values().stream()
                .filter(profile -> dimension.equals(profile.dimension()))
                .sorted(Comparator.comparing(ExternalWorldProfileDefinition::id))
                .flatMap(profile -> profile.anchors().stream()
                        .filter(anchor -> anchor.enabled()
                                && ExternalWorldProfileDefinition.ENCOUNTER.equals(anchor.kind())
                                && locator.equals(anchor.locator()))
                        .map(anchor -> new RouteBinding(profile.dimension(), anchor)))
                .findFirst()
                .orElse(null);
    }

    private static CharacterDefinition character(DefinitionRegistry definitions, String characterId) {
        CharacterDefinition character = definitions.characters().get(characterId);
        if (character == null) {
            throw new IllegalStateException("validated encounter references missing character " + characterId);
        }
        return character;
    }
}
