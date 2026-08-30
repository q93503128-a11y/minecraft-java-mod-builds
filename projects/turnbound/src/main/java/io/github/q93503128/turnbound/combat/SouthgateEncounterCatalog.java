package io.github.q93503128.turnbound.combat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data-driven Southgate Meadow combat catalog for Chapter 1 P2. */
public final class SouthgateEncounterCatalog {
    public static final String ENC_M01 = "southgate_enc_m01";
    public static final String ENC_M02 = "southgate_enc_m02";
    public static final String ENC_M03 = "southgate_enc_m03";
    public static final String ENC_M04 = "southgate_enc_m04";
    public static final String ENC_M05 = "southgate_enc_m05";
    public static final String B01_GRAUL = "southgate_b01_graul";

    public record EncounterSpec(
            String id,
            String label,
            boolean boss,
            List<String> enemyDefinitionIds,
            int rewardXp,
            int rewardGold
    ) {
        public EncounterSpec {
            enemyDefinitionIds = List.copyOf(enemyDefinitionIds);
            if (id.isBlank() || label.isBlank() || enemyDefinitionIds.isEmpty() || enemyDefinitionIds.size() > 5) {
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
        java.util.ArrayList<CombatantState> units = new java.util.ArrayList<>();
        units.add(new CombatantState("ally_kyren", PrototypeRoster.kyren(), CombatantSide.ALLY, 0));
        units.add(new CombatantState("ally_lumea", PrototypeRoster.lumea(), CombatantSide.ALLY, 1));
        units.add(new CombatantState("ally_bram", PrototypeRoster.bram(), CombatantSide.ALLY, 2));
        units.add(new CombatantState("ally_elysia", PrototypeRoster.elysia(), CombatantSide.ALLY, 3));
        for (int i = 0; i < spec.enemyDefinitionIds().size(); i++) {
            String defId = spec.enemyDefinitionIds().get(i);
            units.add(new CombatantState(id + "_enemy_" + i, enemyDefinition(defId), CombatantSide.ENEMY, 4 + i));
        }
        return new BattleState(units);
    }

    public static CombatantDefinition enemyDefinition(String id) {
        return switch (id) {
            case "E001" -> PrototypeRoster.corruptedWalker();
            case "E002" -> PrototypeRoster.boneArcher();
            case "E003" -> PrototypeRoster.hookTracker();
            case "E004" -> PrototypeRoster.ironSentinel();
            case "E005" -> PrototypeRoster.fieldMedic();
            case "B01" -> PrototypeRoster.graul();
            default -> throw new IllegalArgumentException("Unknown Southgate enemy " + id);
        };
    }

    private static Map<String, EncounterSpec> buildCatalog() {
        Map<String, EncounterSpec> map = new LinkedHashMap<>();
        // Reward amounts are alpha.11 economy-tuning values. P3 persistence/economy may rebalance them.
        put(map, new EncounterSpec(ENC_M01, "무너진 순찰대", false, List.of("E001", "E002", "E005"), 35, 60));
        put(map, new EncounterSpec(ENC_M02, "수로 추적대", false, List.of("E001", "E001", "E003"), 40, 70));
        put(map, new EncounterSpec(ENC_M03, "다리 매복대", false, List.of("E002", "E003", "E005"), 45, 80));
        put(map, new EncounterSpec(ENC_M04, "철갑 호위대", false, List.of("E001", "E004", "E005"), 50, 90));
        put(map, new EncounterSpec(ENC_M05, "남문 봉쇄대", false, List.of("E002", "E003", "E004", "E005"), 60, 110));
        put(map, new EncounterSpec(B01_GRAUL, "B01 그라울", true, List.of("B01"), 180, 300));
        return Map.copyOf(map);
    }

    private static void put(Map<String, EncounterSpec> map, EncounterSpec spec) {
        if (map.put(spec.id(), spec) != null) throw new IllegalStateException("Duplicate encounter " + spec.id());
    }
}
