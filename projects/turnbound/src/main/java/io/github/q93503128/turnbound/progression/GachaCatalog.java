package io.github.q93503128.turnbound.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical v0.4 character summon pool and economy constants. */
public final class GachaCatalog {
    public static final int SINGLE_COST = 300;
    public static final int TEN_COST = 3_000;
    public static final int HARD_PITY = 60;
    public static final int SOFT_PITY_START = 45;
    public static final int HISTORY_LIMIT = 50;
    public static final double BASE_FIVE_STAR_RATE = 0.02;
    public static final double FOUR_STAR_RATE = 0.15;
    public static final double THREE_STAR_RATE = 0.83;
    public static final double SOFT_PITY_STEP = 0.03;

    private static final Map<Integer, List<String>> STANDARD_POOL = Map.of(
            5, List.of("P02", "P06"),
            4, List.of("P01", "P03", "P04", "P05", "P07"),
            3, List.of("P08"));
    private static final List<String> STARTER_GUARANTEE = List.of("P02", "P05", "P06", "P07");
    private static final Map<String, Integer> LEGACY_NATIVE_STARS = Map.of(
            "F01", 1, "F02", 1, "F03", 2, "F04", 2);
    private static final Map<String, Integer> NATIVE_STARS = nativeStars();
    private static final Map<String, Integer> KNOWN_NATIVE_STARS = knownNativeStars();

    private GachaCatalog() {}
    public static List<String> standardPool(int nativeStars) { List<String> pool=STANDARD_POOL.get(nativeStars); if(pool==null) throw new IllegalArgumentException("Unsupported native stars "+nativeStars); return pool; }
    public static List<String> starterGuaranteePool(){return STARTER_GUARANTEE;}
    public static boolean isSummonable(String characterId){return NATIVE_STARS.containsKey(characterId);}
    /** Save-compatibility only: legacy F01-F04 may remain owned but can never be newly summoned. */
    public static boolean isKnownCharacter(String characterId){return KNOWN_NATIVE_STARS.containsKey(characterId);}
    public static int nativeStars(String characterId){Integer value=KNOWN_NATIVE_STARS.get(characterId);if(value==null) throw new IllegalArgumentException("Unknown character "+characterId);return value;}
    public static double baseRarityRate(int nativeStars){return switch(nativeStars){case 5->BASE_FIVE_STAR_RATE;case 4->FOUR_STAR_RATE;case 3->THREE_STAR_RATE;default->throw new IllegalArgumentException("Unsupported summon rarity "+nativeStars);};}
    public static double standardCharacterWeight(String characterId){if(!isSummonable(characterId))throw new IllegalArgumentException("Character is not in the summon pool "+characterId);int stars=nativeStars(characterId);return baseRarityRate(stars)/standardPool(stars).size();}
    public static int duplicateEssence(int nativeStars){return switch(nativeStars){case 1->5;case 2->15;case 3->15;case 4->60;case 5->250;default->throw new IllegalArgumentException("Unsupported native stars "+nativeStars);};}
    private static Map<String,Integer> nativeStars(){Map<String,Integer> out=new LinkedHashMap<>();for(var entry:STANDARD_POOL.entrySet())for(String id:entry.getValue())if(out.put(id,entry.getKey())!=null)throw new IllegalStateException("Duplicate summon id "+id);return Map.copyOf(out);}
    private static Map<String,Integer> knownNativeStars(){Map<String,Integer> out=new LinkedHashMap<>(NATIVE_STARS);out.putAll(LEGACY_NATIVE_STARS);return Map.copyOf(out);}
}
