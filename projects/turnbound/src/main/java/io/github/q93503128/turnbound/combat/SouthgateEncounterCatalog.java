package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** v0.4 canonical Southgate Meadow encounter catalog. */
public final class SouthgateEncounterCatalog {
    public static final String ENC_M01 = "southgate_enc_m01";
    public static final String ENC_M02 = "southgate_enc_m02";
    public static final String ENC_M03 = "southgate_enc_m03";
    public static final String ENC_M04 = "southgate_enc_m04";
    public static final String ENC_M05 = "southgate_enc_m05";
    public static final String B01_GRAUL = "southgate_b01_graul";

    public record EncounterSpec(String id, String label, boolean boss, int level,
                                List<String> enemyDefinitionIds, int rewardXp, int rewardGold) {
        public EncounterSpec {
            enemyDefinitionIds = List.copyOf(enemyDefinitionIds);
            if (id.isBlank() || label.isBlank() || level < 1 || enemyDefinitionIds.isEmpty() || enemyDefinitionIds.size() > 5) {
                throw new IllegalArgumentException("Invalid Southgate encounter " + id);
            }
            if (rewardXp < 0 || rewardGold < 0) throw new IllegalArgumentException("Negative reward");
        }
    }

    private static final Map<String, EncounterSpec> ENCOUNTERS = buildCatalog();
    private static final List<String> NORMAL_IDS = List.of(ENC_M01, ENC_M02, ENC_M03, ENC_M04, ENC_M05);

    private SouthgateEncounterCatalog() {}
    public static boolean contains(String id) { return ENCOUNTERS.containsKey(id); }
    public static EncounterSpec spec(String id) {
        EncounterSpec spec = ENCOUNTERS.get(id);
        if (spec == null) throw new IllegalArgumentException("Unknown Southgate encounter " + id);
        return spec;
    }
    public static List<String> normalEncounterIds() { return NORMAL_IDS; }
    public static List<EncounterSpec> normalEncounters() { return NORMAL_IDS.stream().map(SouthgateEncounterCatalog::spec).toList(); }
    public static EncounterSpec boss() { return spec(B01_GRAUL); }

    public static BattleState createBattle(String id) {
        EncounterSpec spec = spec(id);
        ArrayList<CombatantState> units = new ArrayList<>();
        // Campaign party from v0.4 Prologue canon. P02 remains available in /turnbound battle P0 diagnostics.
        units.add(new CombatantState("ally_kyren", PrototypeRoster.kyren(), CombatantSide.ALLY, 0));
        units.add(new CombatantState("ally_bram", PrototypeRoster.bram(), CombatantSide.ALLY, 1));
        units.add(new CombatantState("ally_elysia", PrototypeRoster.elysia(), CombatantSide.ALLY, 2));
        units.add(new CombatantState("ally_f03", PrototypeRoster.borderHunter(), CombatantSide.ALLY, 3));
        for (int i = 0; i < spec.enemyDefinitionIds().size(); i++) {
            String defId = spec.enemyDefinitionIds().get(i);
            String instanceId = spec.boss() ? "b01_graul" : id + "_enemy_" + i;
            units.add(new CombatantState(instanceId, enemyDefinition(defId), CombatantSide.ENEMY, 4 + i));
        }
        return new BattleState(units);
    }

    public static CombatantDefinition enemyDefinition(String id) {
        return switch (id) {
            case "E001" -> PrototypeRoster.corruptedWalker();
            case "E002" -> PrototypeRoster.boneArcher();
            case "E003" -> PrototypeRoster.unstableExploder();
            case "E004" -> PrototypeRoster.roadsideRaider();
            case "E005" -> PrototypeRoster.fieldMedic();
            case "B01" -> PrototypeRoster.graul();
            default -> throw new IllegalArgumentException("Unknown Southgate enemy " + id);
        };
    }

    private static Map<String, EncounterSpec> buildCatalog() {
        Map<String, EncounterSpec> map = new LinkedHashMap<>();
        putNormal(map, ENC_M01, "무너진 보행자 무리", 1, List.of("E001", "E001"));
        putNormal(map, ENC_M02, "뼈 사수 순찰", 2, List.of("E001", "E002"));
        putNormal(map, ENC_M03, "길목 약탈자", 3, List.of("E004", "E004"));
        putNormal(map, ENC_M04, "불안정 폭발체", 4, List.of("E003", "E002"));
        putNormal(map, ENC_M05, "야전 치유대", 5, List.of("E005", "E001", "E001"));
        put(map, new EncounterSpec(B01_GRAUL, "B01 들이받는 왕 그라울", true, 6,
                List.of("B01"), 5000, 12000));
        return Map.copyOf(map);
    }

    private static void putNormal(Map<String, EncounterSpec> map, String id, String label, int level, List<String> enemies) {
        int xp = enemies.size() * (80 + 90 * level);
        int gold = enemies.size() * (70 + 18 * level);
        put(map, new EncounterSpec(id, label, false, level, enemies, xp, gold));
    }

    private static void put(Map<String, EncounterSpec> map, EncounterSpec spec) {
        if (map.put(spec.id(), spec) != null) throw new IllegalStateException("Duplicate encounter " + spec.id());
    }
}
