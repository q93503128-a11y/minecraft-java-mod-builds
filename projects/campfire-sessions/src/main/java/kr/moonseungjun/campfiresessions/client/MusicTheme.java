package kr.moonseungjun.campfiresessions.client;

public enum MusicTheme {
    CLEAN("Clean", 0xEC111418, 0xD61D2228, 0xFFF0CF92, 0xFFF7F4EE, 0xFFA8A39A),
    NEON("Neon", 0xEC080E18, 0xD6112034, 0xFF57DDF7, 0xFFF4FDFF, 0xFF88A9B7),
    OCEAN("Ocean", 0xEC07171B, 0xD60D2C32, 0xFF66DCCB, 0xFFF1FFFC, 0xFF8DBBB5),
    DESERT("Desert", 0xEC1B1209, 0xD63A2918, 0xFFFFBC5A, 0xFFFFF5E5, 0xFFD1B889),
    ROUGH("Rough", 0xEC180D0F, 0xD6372022, 0xFFFF716B, 0xFFFFEFED, 0xFFC8A09E);

    private final String displayName;
    private final int background;
    private final int surface;
    private final int accent;
    private final int text;
    private final int muted;

    MusicTheme(String displayName, int background, int surface, int accent, int text, int muted) {
        this.displayName = displayName;
        this.background = background;
        this.surface = surface;
        this.accent = accent;
        this.text = text;
        this.muted = muted;
    }

    public String displayName() { return displayName; }
    public int background() { return background; }
    public int surface() { return surface; }
    public int accent() { return accent; }
    public int text() { return text; }
    public int muted() { return muted; }
}
