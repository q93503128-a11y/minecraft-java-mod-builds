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
    private static final String VISUAL_SIGNATURE_PREFIX = "VISUAL_SIGNATURE:";

    private CampaignEncounterCatalog() {}

    public static String canonicalId(String id) { return CampaignProgressStore.canonicalEncounterId(id); }
    public static boolean contains(String id) { return id != null && V04Catalogs.hasEncounter(canonicalId(id)); }
    public static V04Catalogs.Encounter spec(String id) { return V04Catalogs.encounter(canonicalId(id)); }
    public static List<V04Catalogs.Encounter> all() { return V04Catalogs.encounters(); }

    public static BattleState createBattle(UUID playerId, String encounterId) {
        V04Catalogs.Encounter encounter = spec(encounterId);
        ArrayList<CombatantState> units = new ArrayList<>();
        int formation = 0;
        for (String characterId : CampaignProgressStore.activeParty(playerId)) {
            units.add(new CombatantState("ally_" + characterId.toLowerCase(), campaignDefinition(playerId, characterId),
                    CombatantSide.ALLY, formation++));
        }
        if (units.isEmpty()) throw new IllegalStateException("TURNBOUND campaign has no active allies");
        if (units.size() > 4) throw new IllegalStateException("TURNBOUND party exceeds four allies");

        for (int index = 0; index < encounter.enemies().size(); index++) {
            String enemyId = encounter.enemies().get(index);
            CombatantDefinition canonical = CanonicalData.definition(enemyId, encounter.level(), 0, false);
            CombatantDefinition definition = tempoAdjustedEnemy(canonical, encounter);
            String instanceId = encounter.boss()
                    ? "boss_" + enemyId.toLowerCase()
                    : canonicalId(encounterId).toLowerCase() + "_enemy_" + index;
            units.add(new CombatantState(instanceId, definition, CombatantSide.ENEMY, 4 + index));
        }
        return new BattleState(units);
    }

    /**
     * Field battles should resolve briskly enough that turn decisions matter more than HP attrition.
     * This is encounter tuning only; canonical character/enemy data and formulas remain untouched.
     */
    private static CombatantDefinition tempoAdjustedEnemy(CombatantDefinition base, V04Catalogs.Encounter encounter) {
        double hpScale;
        if (encounter.id().startsWith("TUTORIAL_")) hpScale = 0.68;
        else if (encounter.boss()) hpScale = 0.88;
        else if (base.elite()) hpScale = 0.86;
        else hpScale = 0.80;
        BattleStats stats = base.stats();
        BattleStats tuned = new BattleStats(Math.max(1, (int)Math.round(stats.maxHp() * hpScale)),
                stats.attack(), stats.defense(), stats.speed());
        return new CombatantDefinition(base.id(), base.name(), tuned, base.basicSkillId(), base.skills(),
                base.nativeStars(), base.rules(), base.params());
    }

    private static CombatantDefinition campaignDefinition(UUID playerId, String characterId) {
        var level = CampaignProgressStore.character(playerId, characterId);
        var growth = CampaignProgressStore.growth(playerId, characterId);
        CombatantDefinition base = CanonicalData.definition(characterId, level.level(), growth.currentStar(), growth.awakened());
        Set<String> rules = new LinkedHashSet<>(base.rules());
        rules.addAll(CampaignProgressStore.equipmentRules(playerId, characterId));
        String visualRule = signatureVisualRule(playerId, characterId);
        if (!visualRule.isBlank()) rules.add(visualRule);
        return new CombatantDefinition(
                base.id(), base.name(), CampaignProgressStore.finalStats(playerId, characterId), base.basicSkillId(), base.skills(),
                base.nativeStars(), List.copyOf(rules), base.params());
    }

    /** Presentation-only tag; equipment gameplay remains in EquipmentInventory.fixedRules(). */
    private static String signatureVisualRule(UUID playerId, String characterId) {
        var equipment = CampaignProgressStore.equipment(playerId);
        var loadout = equipment.loadouts().get(characterId);
        if (loadout == null || loadout.signature().isBlank()) return "";
        var item = equipment.items().get(loadout.signature());
        if (item == null || !item.itemId().startsWith("sig_")) return "";
        return VISUAL_SIGNATURE_PREFIX + item.itemId();
    }
}
