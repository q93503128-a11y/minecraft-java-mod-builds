package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.resources.Identifier;

public enum MusicTheme {
    CLEAN("Clean", "clean", 0xFFF3F6F8, 0xFFB7C0C9),
    NEON("Neon", "neon", 0xFF5CEBFF, 0xFF8CB7C4),
    OCEAN("Ocean", "ocean", 0xFF7FE3F2, 0xFF9CC6CF),
    DESERT("Desert", "desert", 0xFFFFCC7A, 0xFFD0B17D),
    ROUGH("Rough", "rough", 0xFFFF836D, 0xFFC09A92);

    private final String displayName;
    private final String assetKey;
    private final int accent;
    private final int muted;

    MusicTheme(String displayName, String assetKey, int accent, int muted) {
        this.displayName = displayName;
        this.assetKey = assetKey;
        this.accent = accent;
        this.muted = muted;
    }

    public String displayName() { return displayName; }
    public String assetKey() { return assetKey; }
    public int accent() { return accent; }
    public int text() { return 0xFFF7F9FA; }
    public int muted() { return muted; }

    public Identifier panelSprite() { return sprite(assetKey + "_panel"); }
    public Identifier cardSprite() { return sprite(assetKey + "_card"); }
    public Identifier buttonSprite() { return sprite(assetKey + "_button"); }

    private static Identifier sprite(String file) {
        return Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, "music/theme/" + file);
    }
}
