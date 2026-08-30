package io.github.q93503128.turnbound.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical v0.4 character summon pool and economy constants. */
public final class GachaCatalog {
    public static final int SINGLE_COST = 300;
    public static final int TEN_COST = 3_000;
    public static final int HARD_PITY = 80;
    public static final int SOFT_PITY_START = 65;
    public static final double BASE_FIVE_STAR_RATE = 0.03;
    public static final double SOFT_PITY_STEP = 0.03;

    private static final Map<Integer, List<String>> STANDARD_POOL = Map.of(
            5, List.of("P02", "P05", "P06"),
            4, List.of("P01", "P03", "P04", "P07"),
            3, List.of("P08"),
            2, List.of("F03", "F04"),
            1, List.of("F01", "F02"));
    private static final List<String> STARTER_GUARANTEE = List.of("P02", "P05", "P06", "P07");
    private static final Map<String, Integer> NATIVE_STARS = nativeStars();

    private GachaCatalog() {}

    public static List<String> standardPool(int nativeStars) {
        List<String> pool = STANDARD_POOL.get(nativeStars);
        if (pool == null) throw new IllegalArgumentException("Unsupported native stars " + nativeStars);
        return pool;
    }

    public static List<String> starterGuaranteePool() {
        return STARTER_GUARANTEE;
    }

    public static boolean isSummonable(String characterId) {
        return NATIVE_STARS.containsKey(characterId);
    }

    public static int nativeStars(String characterId) {
        Integer value = NATIVE_STARS.get(characterId);
        if (value == null) throw new IllegalArgumentException("Unknown summon character " + characterId);
        return value;
    }

    public static int duplicateEssence(int nativeStars) {
        return switch (nativeStars) {
            case 1 -> 5;
            case 2 -> 15;
            case 3 -> 40;
            case 4 -> 100;
            case 5 -> 250;
            default -> throw new IllegalArgumentException("Unsupported native stars " + nativeStars);
        };
    }

    private static Map<String, Integer> nativeStars() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var entry : STANDARD_POOL.entrySet()) {
            for (String id : entry.getValue()) {
                if (out.put(id, entry.getKey()) != null) throw new IllegalStateException("Duplicate summon id " + id);
            }
        }
        return Map.copyOf(out);
    }
}
