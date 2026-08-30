package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.world.CampaignProgressStore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** v0.4 campaign encounter factory for every authored field/boss encounter. */
public final class CampaignEncounterCatalog {
    private static final List<String> DEFAULT_PARTY = List.of("P01", "P03", "P04", "F03");

    private CampaignEncounterCatalog() {}

    public static String canonicalId(String id) { return CampaignProgressStore.canonicalEncounterId(id); }
    public static boolean contains(String id) { return id != null && V04Catalogs.hasEncounter(canonicalId(id)); }
    public static V04Catalogs.Encounter spec(String id) { return V04Catalogs.encounter(canonicalId(id)); }
    public static List<V04Catalogs.Encounter> all() { return V04Catalogs.encounters(); }

    public static BattleState createBattle(UUID playerId, String encounterId) {
        V04Catalogs.Encounter encounter = spec(encounterId);
        ArrayList<CombatantState> units = new ArrayList<>();
        int formation = 0;
        for (String characterId : DEFAULT_PARTY) {
            if (!CampaignProgressStore.ownedCharacters(playerId).contains(characterId)) continue;
            units.add(new CombatantState("ally_" + characterId.toLowerCase(), campaignDefinition(playerId, characterId), CombatantSide.ALLY, formation++));
        }
        if (units.isEmpty()) throw new IllegalStateException("TURNBOUND campaign has no active allies");
        if (units.size() > 4) throw new IllegalStateException("TURNBOUND party exceeds four allies");

        for (int index = 0; index < encounter.enemies().size(); index++) {
            String enemyId = encounter.enemies().get(index);
            CombatantDefinition definition = CanonicalData.definition(enemyId, encounter.level(), 0, false);
            String instanceId = encounter.boss()
                    ? "boss_" + enemyId.toLowerCase()
                    : canonicalId(encounterId).toLowerCase() + "_enemy_" + index;
            units.add(new CombatantState(instanceId, definition, CombatantSide.ENEMY, 4 + index));
        }
        return new BattleState(units);
    }

    private static CombatantDefinition campaignDefinition(UUID playerId, String characterId) {
        var level = CampaignProgressStore.character(playerId, characterId);
        var growth = CampaignProgressStore.growth(playerId, characterId);
        CombatantDefinition base = CanonicalData.definition(characterId, level.level(), growth.currentStar(), growth.awakened());
        Set<String> rules = new LinkedHashSet<>(base.rules());
        rules.addAll(CampaignProgressStore.equipmentRules(playerId, characterId));
        return new CombatantDefinition(
                base.id(), base.name(), CampaignProgressStore.finalStats(playerId, characterId), base.basicSkillId(), base.skills(),
                base.nativeStars(), List.copyOf(rules), base.params());
    }
}
