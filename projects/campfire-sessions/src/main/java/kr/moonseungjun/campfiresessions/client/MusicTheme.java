package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.resources.Identifier;

public enum MusicTheme {
    CLEAN("Clean", 0xD8171A1F, 0xD92A3037, 0xFFE8D7B7, 0xFFF6F2EA, 0xFFB9B3A8, "music/metal_panel"),
    NEON("Neon", 0xD90A1220, 0xD9162E49, 0xFF5BE7FF, 0xFFF2FCFF, 0xFFA8C8D8, "music/metal_blue"),
    OCEAN("Ocean", 0xD9081B22, 0xD9143B43, 0xFF63E0D2, 0xFFEFFFFC, 0xFFA6CDC8, "music/metal_green"),
    DESERT("Desert", 0xD923180C, 0xD94B3520, 0xFFFFC15A, 0xFFFFF5E2, 0xFFD8BE93, "music/metal_yellow"),
    ROUGH("Rough", 0xD91E1012, 0xD9472527, 0xFFFF7068, 0xFFFFECEA, 0xFFD0AAA7, "music/metal_red");

    private final String displayName;
    private final int background;
    private final int surface;
    private final int accent;
    private final int text;
    private final int muted;
    private final Identifier rowSprite;

    MusicTheme(String displayName, int background, int surface, int accent, int text, int muted, String spritePath) {
        this.displayName = displayName;
        this.background = background;
        this.surface = surface;
        this.accent = accent;
        this.text = text;
        this.muted = muted;
        this.rowSprite = Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, spritePath);
    }

    public String displayName() { return displayName; }
    public int background() { return background; }
    public int surface() { return surface; }
    public int accent() { return accent; }
    public int text() { return text; }
    public int muted() { return muted; }
    public Identifier rowSprite() { return rowSprite; }
}
