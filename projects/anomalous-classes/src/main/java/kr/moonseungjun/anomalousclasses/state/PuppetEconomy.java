package kr.moonseungjun.anomalousclasses.state;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Source-grounded starting assembly economy. The light wood frame has an
 * explicit size cost table; other frame costs are not silently invented.
 * Inventory material ledger is canonical for all later refunds.
 */
public final class PuppetEconomy {
    private PuppetEconomy() {}

    private static final int[][] LIGHT_FRAME = {
        {3,2,2,1}, // 1.0 blocks
        {4,2,2,1}, // 1.5 blocks
        {7,4,4,2}, // 2.0 blocks
        {10,5,5,3}, // 2.5 blocks
        {15,8,8,4} // 3.0 blocks
    };

    public static Map<String, Integer> lightFrameMaterials(PuppetBlueprint.Size size) {
        int[] q = LIGHT_FRAME[size.ordinal()];
        return Map.of("minecraft:oak_planks", q[0], "minecraft:stick", q[1],
                "minecraft:string", q[2], "minecraft:copper_ingot", q[3]);
    }

    public static Map<String, Integer> starterBladeMaterials() {
        return Map.of("minecraft:iron_nugget", 4, "minecraft:stick", 2,
                "minecraft:flint", 1);
    }

    public static Map<String, Integer> starterCoreMaterials() {
        return Map.of("minecraft:redstone", 1, "minecraft:copper_ingot", 2,
                "minecraft:glass", 1);
    }

    /** Frame, blade and base core are distinct parts, then 2 strings to assemble. */
    public static Map<String, Integer> starterBlueprintLedger(PuppetBlueprint.Size size) {
        Map<String, Integer> materials = new LinkedHashMap<>();
        add(materials, lightFrameMaterials(size));
        add(materials, starterBladeMaterials());
        add(materials, starterCoreMaterials());
        materials.merge("minecraft:string", 2, Integer::sum);
        return Map.copyOf(materials);
    }

    private static void add(Map<String, Integer> into, Map<String, Integer> from) {
        from.forEach((id, count) -> into.merge(id, count, Math::addExact));
    }

    public static Map<String, Integer> extraLightFrameMaterials(PuppetBlueprint.Size from,
                                                                 PuppetBlueprint.Size to) {
        if (to.ordinal() < from.ordinal()) throw new IllegalArgumentException("Expansion must increase size");
        Map<String, Integer> old = lightFrameMaterials(from);
        Map<String, Integer> next = lightFrameMaterials(to);
        Map<String, Integer> addition = new LinkedHashMap<>();
        next.forEach((id, count) -> {
            int delta = count - old.getOrDefault(id, 0);
            if (delta > 0) addition.put(id, delta);
        });
        return Map.copyOf(addition);
    }

    /** No material can be refunded twice or above what was actually paid. */
    public static Map<String, Integer> dismantleReturn(PuppetBlueprint blueprint, boolean broken,
                                                        double materialUseBonus) {
        double rate = (broken ? .45 : .75) * (1.0 + Math.max(0, Math.min(.20, materialUseBonus)));
        Map<String, Integer> results = new LinkedHashMap<>();
        blueprint.materialLedger().forEach((id, paid) -> {
            int returned = (int) Math.floor(paid * Math.min(1.0, rate));
            if (returned > 0) results.put(id, returned);
        });
        return Map.copyOf(results);
    }

    public static boolean refundNeverExceedsInput(PuppetBlueprint blueprint, Map<String,Integer> refund) {
        for (var result : refund.entrySet()) {
            if (result.getValue() == null || result.getValue() < 0 ||
                    result.getValue() > blueprint.materialLedger().getOrDefault(result.getKey(), 0))
                return false;
        }
        return true;
    }
}
