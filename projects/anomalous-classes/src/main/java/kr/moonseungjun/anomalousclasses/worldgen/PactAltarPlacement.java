package kr.moonseungjun.anomalousclasses.worldgen;

/** Pure deterministic placement math; callable without Minecraft registries or live worlds. */
public final class PactAltarPlacement {
    private PactAltarPlacement() {}
    static long mix(long value) {
        value ^= value >>> 30; value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27; value *= 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }
    /** The same placement cell is shared by both enabled tiers. */
    public static String selectLandPatron(int chunkX, int chunkZ) {
        long hash=mix(chunkX*0x632be59bd9b4e019L ^ chunkZ*0x85157af5L);
        return switch((int)Math.floorMod(hash,10)) {
            case 0,1 -> "karda";
            case 2,3 -> "shero";
            case 4,5 -> "hastel";
            case 6 -> "sevenwing";
            case 7 -> "merzad";
            case 8 -> "taron";
            default -> "arken";
        };
    }
    public static int[] targetChunk(long x, long z) {
        long h = mix(x * 0x9e3779b97f4a7c15L ^ z * 0xc2b2ae3d27d4eb4fL ^ 0x8ff2a52L);
        return new int[]{(int) (x*40 + 7 + Math.floorMod(h,26)),
                         (int) (z*40 + 7 + Math.floorMod(h>>>26,26))};
    }
}
