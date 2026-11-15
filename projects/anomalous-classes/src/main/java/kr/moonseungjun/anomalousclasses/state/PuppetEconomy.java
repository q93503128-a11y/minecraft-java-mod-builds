package kr.moonseungjun.anomalousclasses.state;

import java.util.LinkedHashMap;
import kr.moonseungjun.anomalousclasses.item.PuppetPartItem;
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

    /**
     * Weapon hardware uses ordinary overworld materials. Launcher and shield
     * quantities are explicit first-balance candidates, not approved final costs.
     * All three forms retain real resource expenditure instead of free switching.
     */
    public static Map<String, Integer> weaponMaterials(PuppetBlueprint.Device device) {
        return switch (device) {
            case BLADE -> starterBladeMaterials();
            case LAUNCHER -> Map.of("minecraft:iron_ingot", 1,
                    "minecraft:copper_ingot", 2, "minecraft:redstone", 2,
                    "minecraft:stick", 2);
            case SHIELD -> Map.of("minecraft:iron_ingot", 3,
                    "minecraft:oak_planks", 3, "minecraft:copper_ingot", 1);
        };
    }

    /**
     * Specialist frame and drive-core costs are tunable overworld recipe candidates.
     * The existing light and basic costs are retained exactly.
     */
    public static Map<String,Integer> frameMaterials(PuppetBlueprint.Frame frame,
                                                    PuppetBlueprint.Size size) {
        Map<String,Integer> light = lightFrameMaterials(size);
        int wood = light.get("minecraft:oak_planks");
        int sticks = light.get("minecraft:stick");
        int strings = light.get("minecraft:string");
        int copper = light.get("minecraft:copper_ingot");
        return switch (frame) {
            case LIGHT -> light;
            case BALANCED -> Map.of("minecraft:oak_planks", Math.max(2, wood / 2),
                    "minecraft:iron_ingot", wood, "minecraft:stick", sticks,
                    "minecraft:string", strings, "minecraft:copper_ingot", copper + 1);
            case HEAVY -> Map.of("minecraft:oak_planks", Math.max(2, wood / 2),
                    "minecraft:iron_ingot", wood * 2, "minecraft:stick", sticks + 1,
                    "minecraft:string", strings, "minecraft:copper_ingot", copper + 2);
        };
    }

    public static Map<String,Integer> coreMaterials(PuppetBlueprint.Core core) {
        return switch (core) {
            case BASIC -> starterCoreMaterials();
            case SWIFT -> Map.of("minecraft:redstone", 4, "minecraft:gold_ingot", 2,
                    "minecraft:copper_ingot", 2, "minecraft:glass", 1);
            case STABLE -> Map.of("minecraft:lapis_lazuli", 4,
                    "minecraft:amethyst_shard", 2, "minecraft:copper_ingot", 2,
                    "minecraft:glass", 1);
            case OUTPUT -> Map.of("minecraft:redstone", 4,
                    "minecraft:gold_ingot", 3, "minecraft:iron_ingot", 2,
                    "minecraft:glass", 1);
        };
    }

    public static Map<String,Integer> partMaterials(PuppetPartItem.Kind kind, int variant,
                                                    PuppetBlueprint.Size size) {
        if (kind == null || size == null) throw new IllegalArgumentException("Unknown puppet module");
        return switch (kind) {
            case FRAME -> frameMaterials(PuppetBlueprint.Frame.values()[variant], size);
            case DEVICE -> weaponMaterials(PuppetBlueprint.Device.values()[variant]);
            case CORE -> coreMaterials(PuppetBlueprint.Core.values()[variant]);
        };
    }

    public static Map<String,Integer> partReplacementLedger(PuppetBlueprint original,
                                                               PuppetPartItem.Kind kind, int variant) {
        int current = switch(kind) {
            case FRAME -> original.frame().ordinal();
            case DEVICE -> original.device().ordinal();
            case CORE -> original.core().ordinal();
        };
        if (current == variant) return Map.of();
        PuppetBlueprint.Size size = kind == PuppetPartItem.Kind.FRAME
                ? original.size() : PuppetBlueprint.Size.NORMAL;
        Map<String,Integer> old = partMaterials(kind, current, size);
        Map<String,Integer> fresh = partMaterials(kind, variant, size);
        Map<String,Integer> ledger = new LinkedHashMap<>(original.materialLedger());
        old.forEach((id, count) -> {
            int remaining = ledger.getOrDefault(id, 0) - count;
            if (remaining < 0) throw new IllegalArgumentException("Part ledger missing materials");
            if (remaining == 0) ledger.remove(id); else ledger.put(id, remaining);
        });
        add(ledger, fresh);
        return Map.copyOf(ledger);
    }

    public static Map<String,Integer> extraFrameMaterials(PuppetBlueprint.Frame frame,
                                                           PuppetBlueprint.Size from,
                                                           PuppetBlueprint.Size to) {
        if (to.ordinal() <= from.ordinal()) throw new IllegalArgumentException("Must grow body");
        Map<String,Integer> original = frameMaterials(frame, from);
        Map<String,Integer> next = frameMaterials(frame, to);
        Map<String,Integer> delta = new LinkedHashMap<>();
        next.forEach((id, amount) -> {
            int need = amount - original.getOrDefault(id, 0);
            if (need > 0) delta.put(id, need);
        });
        return Map.copyOf(delta);
    }

    public static Map<String, Integer> starterBlueprintLedger(PuppetBlueprint.Size size,
                                                               PuppetBlueprint.Device device) {
        Map<String, Integer> materials = new LinkedHashMap<>();
        add(materials, lightFrameMaterials(size));
        add(materials, weaponMaterials(device));
        add(materials, starterCoreMaterials());
        materials.merge("minecraft:string", 2, Integer::sum);
        return Map.copyOf(materials);
    }

    /** Backwards-compatible convenience for the original blade baseline. */
    public static Map<String, Integer> starterBlueprintLedger(PuppetBlueprint.Size size) {
        return starterBlueprintLedger(size, PuppetBlueprint.Device.BLADE);
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

    /**
     * Full workshop repair. The consumed frame materials reflect damage and
     * actual size, while the material-use passive reduces the necessary volume.
     * Repair expenditure is NOT an upgrade investment: never add it to refund ledger.
     * This is a tunable balance candidate, using only the initial wood-frame bill.
     */
    public static Map<String, Integer> repairMaterials(PuppetBlueprint puppet, int materialUseRanks) {
        double missing = 1.0 - puppet.healthRatio();
        if (missing <= 0.000001) return Map.of();
        double discount = 1.0 - 0.006 * Math.min(25, Math.max(0, materialUseRanks));
        String primary = puppet.frame() == PuppetBlueprint.Frame.LIGHT
                ? "minecraft:oak_planks" : "minecraft:iron_ingot";
        int structural = frameMaterials(puppet.frame(), puppet.size()).get(primary);
        int units = Math.max(1, (int) Math.ceil(structural * missing * 0.5 * discount));
        int strings = Math.max(1, (int) Math.ceil(2.0 * missing * discount));
        return Map.of(primary, units, "minecraft:string", strings);
    }

    /**
     * Only material added to the physical frame is refundable. Changing size
     * never refunds more than the initial + actual expansion investment.
     */
    public static Map<String, Integer> expandLedger(PuppetBlueprint puppet,
                                                     PuppetBlueprint.Size destination) {
        if (destination.ordinal() <= puppet.size().ordinal()) return Map.of();
        Map<String, Integer> next = new LinkedHashMap<>(puppet.materialLedger());
        extraFrameMaterials(puppet.frame(), puppet.size(), destination).forEach(
                (material, amount) -> next.merge(material, amount, Math::addExact));
        return Map.copyOf(next);
    }

    /**
     * Emergency field stitching: always paid, smaller than full workshop repair.
     * Size and actual structural investment determine material volume; the
     * standard repair discount does not silently make combat restoration free.
     */
    public static Map<String, Integer> emergencyStitchMaterials(PuppetBlueprint puppet,
                                                                  double maxHpFraction) {
        if (puppet == null || !Double.isFinite(maxHpFraction)
                || maxHpFraction <= 0.0 || maxHpFraction > .25) return Map.of();
        Map<String, Integer> structural = frameMaterials(puppet.frame(), puppet.size());
        String primary = puppet.frame() == PuppetBlueprint.Frame.LIGHT
                ? "minecraft:oak_planks" : "minecraft:iron_ingot";
        int count = Math.max(1, (int) Math.ceil(structural.get(primary)
                * maxHpFraction * .5));
        return Map.of(primary, count, "minecraft:string", 1);
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
