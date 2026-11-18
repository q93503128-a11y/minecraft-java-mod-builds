package kr.moonseungjun.anomalousclasses.worldgen;

/** Pure deterministic placement math; callable without Minecraft registries or live worlds. */
public final class PactAltarPlacement {
    private PactAltarPlacement() {}
    static long mix(long value) {
        value ^= value >>> 30; value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27; value *= 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }
    /** One shared grid across tiers 2, 3 and 4, with tier-one ruins pending.
     * Temporary non-ocean distribution: 50% tier 2, 33% tier 3, 17% tier 4.
     * All patrons can appear without increasing the overall worldgen density.
     */
    public static String selectLandPatron(int chunkX,int chunkZ) {
        long hash=mix(chunkX*0x632be59bd9b4e019L ^ chunkZ*0x85157af5L);
        int roll=(int)Math.floorMod(hash,100);
        int pick=(int)Math.floorMod(hash>>>8,4);
        int commonPick=(int)Math.floorMod(hash>>>12,3);
        if(roll<50)return switch(commonPick) {
            case 0 -> "karda";
            case 1 -> "shero";
            default -> "hastel";
        };
        if(roll<83)return switch(pick) {
            case 0 -> "sevenwing";
            case 1 -> "merzad";
            case 2 -> "taron";
            default -> "arken";
        };
        return switch(pick) {
            case 0 -> "nahar";
            case 1 -> "rakiel";
            case 2 -> "ormas";
            default -> "viar";
        };
    }
    public static int[] targetChunk(long x, long z) {
        long h = mix(x * 0x9e3779b97f4a7c15L ^ z * 0xc2b2ae3d27d4eb4fL ^ 0x8ff2a52L);
        return new int[]{(int) (x*40 + 7 + Math.floorMod(h,26)),
                         (int) (z*40 + 7 + Math.floorMod(h>>>26,26))};
    }
}
