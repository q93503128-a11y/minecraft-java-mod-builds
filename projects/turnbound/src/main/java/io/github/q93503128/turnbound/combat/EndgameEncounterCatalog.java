package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.world.CampaignProgressStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Canonical v0.4 Hard boss rematches and Rift Gate 1..30 battle factory. */
public final class EndgameEncounterCatalog {
    private static final Map<String, Integer> BOSS_BASE_LEVEL = Map.of(
            "B01", 6, "B02", 10, "B03", 13, "B04", 16, "B05", 20);

    private EndgameEncounterCatalog() {}

    public static String hardId(String bossId) { return "HARD_" + bossId; }
    public static String riftId(int floor) { return "RIFT_F" + String.format("%02d", floor); }

    public static boolean contains(String id) {
        if (id == null) return false;
        if (id.matches("HARD_B0[1-5]")) return true;
        if (!id.matches("RIFT_F\\d{2}")) return false;
        try {
            int floor = Integer.parseInt(id.substring("RIFT_F".length()));
            return floor >= 1 && floor <= 30;
        } catch (NumberFormatException ignored) { return false; }
    }

    public static boolean hardBoss(String id) { return id != null && id.matches("HARD_B0[1-5]"); }
    public static boolean rift(String id) { return id != null && id.matches("RIFT_F\\d{2}") && contains(id); }
    public static boolean bossBattle(String id) { return hardBoss(id) || (rift(id) && V04Catalogs.riftFloor(riftFloorNumber(id)).hardBossPattern()); }
    public static boolean autoAllowed(String id) { return rift(id); }
    public static boolean speedAllowed(String id) { return rift(id); }
    public static boolean fleeAllowed(String id) { return false; }

    public static boolean unlocked(UUID playerId, String id) {
        Set<String> cleared = CampaignProgressStore.snapshot(playerId).clearedEncounters();
        if (hardBoss(id)) return cleared.contains("BATTLE_" + id.substring("HARD_".length()));
        if (rift(id)) return cleared.contains("BATTLE_B05");
        return false;
    }

    public static BattleState createBattle(UUID playerId, String id) {
        if (!contains(id)) throw new IllegalArgumentException("Unknown endgame encounter " + id);
        if (!unlocked(playerId, id)) throw new IllegalStateException("Endgame encounter is locked: " + id);
        ArrayList<CombatantState> units = new ArrayList<>(party(playerId));
        if (hardBoss(id)) {
            String bossId = id.substring("HARD_".length());
            int baseLevel = BOSS_BASE_LEVEL.get(bossId);
            units.add(new CombatantState("boss_" + bossId.toLowerCase() + "_hard",
                    hardBossDefinition(bossId, baseLevel, 1.0), CombatantSide.ENEMY, 4));
            return new BattleState(units);
        }

        V04Catalogs.RiftFloor floor = V04Catalogs.riftFloor(riftFloorNumber(id));
        if (floor.hardBossPattern()) {
            String bossId = floor.enemies().getFirst();
            int baseLevel = BOSS_BASE_LEVEL.get(bossId);
            double extraHp = 1.0 + 0.02 * Math.max(0, floor.level() - baseLevel);
            units.add(new CombatantState("rift_f" + floor.floor() + "_" + bossId.toLowerCase(),
                    hardBossDefinition(bossId, floor.level(), extraHp), CombatantSide.ENEMY, 4));
        } else {
            for (int index = 0; index < floor.enemies().size(); index++) {
                String enemyId = floor.enemies().get(index);
                units.add(new CombatantState("rift_f" + floor.floor() + "_enemy_" + index,
                        CanonicalData.definition(enemyId, floor.level(), 0, false), CombatantSide.ENEMY, 4 + index));
            }
        }
        return new BattleState(units);
    }

    public static int riftFloorNumber(String id) {
        if (!rift(id)) throw new IllegalArgumentException("Not a Rift encounter " + id);
        return Integer.parseInt(id.substring("RIFT_F".length()));
    }

    public static String bossId(String id) {
        if (hardBoss(id)) return id.substring("HARD_".length());
        if (rift(id)) {
            V04Catalogs.RiftFloor floor = V04Catalogs.riftFloor(riftFloorNumber(id));
            return floor.hardBossPattern() ? floor.enemies().getFirst() : "";
        }
        return "";
    }

    private static List<CombatantState> party(UUID playerId) {
        ArrayList<CombatantState> units = new ArrayList<>();
        int formation = 0;
        for (String characterId : CampaignProgressStore.activeParty(playerId)) {
            units.add(new CombatantState("ally_" + characterId.toLowerCase(), campaignDefinition(playerId, characterId),
                    CombatantSide.ALLY, formation++));
        }
        if (units.isEmpty()) throw new IllegalStateException("TURNBOUND campaign has no active allies");
        return List.copyOf(units);
    }

    private static CombatantDefinition campaignDefinition(UUID playerId, String characterId) {
        var level = CampaignProgressStore.character(playerId, characterId);
        var growth = CampaignProgressStore.growth(playerId, characterId);
        CombatantDefinition base = CanonicalData.definition(characterId, level.level(), growth.currentStar(), growth.awakened());
        Set<String> rules = new LinkedHashSet<>(base.rules());
        rules.addAll(CampaignProgressStore.equipmentRules(playerId, characterId));
        return new CombatantDefinition(base.id(), base.name(), CampaignProgressStore.finalStats(playerId, characterId),
                base.basicSkillId(), base.skills(), base.nativeStars(), List.copyOf(rules), base.params());
    }

    private static CombatantDefinition hardBossDefinition(String bossId, int encounterLevel, double extraHpFactor) {
        int baseLevel = BOSS_BASE_LEVEL.get(bossId);
        CombatantDefinition base = CanonicalData.definition(bossId, encounterLevel + 5, 0, false);
        BattleStats stats = base.stats();
        int hp = Math.max(1, (int)Math.floor(stats.maxHp() * 1.65 * extraHpFactor));
        int atk = Math.max(1, (int)Math.floor(stats.attack() * 1.25));
        int def = Math.max(0, (int)Math.floor(stats.defense() * 1.15));
        int spd = stats.speed() + 8;
        Map<String, Double> params = new LinkedHashMap<>(base.params());
        params.put("hardBoss", 1.0);
        params.put("bossBaseLevel", (double)baseLevel);
        params.put("encounterLevel", (double)encounterLevel);
        params.put("level", (double)(encounterLevel + 5));
        return new CombatantDefinition(base.id(), base.name() + " [Hard]", new BattleStats(hp, atk, def, spd),
                base.basicSkillId(), base.skills(), base.nativeStars(), base.rules(), Map.copyOf(params));
    }
}
