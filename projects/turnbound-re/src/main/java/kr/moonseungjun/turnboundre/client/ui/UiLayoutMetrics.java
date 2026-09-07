package kr.moonseungjun.turnboundre.client.ui;

/** Pure logical-coordinate layout contract for the M5 battle HUD. */
public final class UiLayoutMetrics {
    public static final int SPACE_2 = 2;
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_24 = 24;

    private static final int MIN_WIDTH = 480;
    private static final int MIN_HEIGHT = 270;

    private UiLayoutMetrics() {}

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (x < 0 || y < 0) throw new IllegalArgumentException("rect origin must be >= 0");
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("rect size must be > 0");
        }

        public int right() { return x + width; }
        public int bottom() { return y + height; }

        public boolean inside(int screenWidth, int screenHeight) {
            return right() <= screenWidth && bottom() <= screenHeight;
        }

        public boolean intersects(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }

    public record BattleHudLayout(
            Rect turnRail,
            Rect enemySummary,
            Rect partyStatus,
            Rect commandStrip,
            Rect reservedWorldViewport
    ) {}

    public static BattleHudLayout battleHud(int screenWidth, int screenHeight) {
        if (screenWidth < MIN_WIDTH || screenHeight < MIN_HEIGHT) {
            throw new IllegalArgumentException(
                    "battle HUD requires at least " + MIN_WIDTH + "x" + MIN_HEIGHT
                            + " logical pixels, got " + screenWidth + "x" + screenHeight);
        }

        int margin = SPACE_8;
        int bottomHeight = clamp(screenHeight / 5, 58, 82);
        int railWidth = clamp(screenWidth / 10, 72, 112);
        int railHeight = clamp(screenHeight - bottomHeight - SPACE_24 - margin, 120, 228);
        int enemyWidth = clamp(screenWidth / 3, 180, 300);
        int enemyHeight = 44;
        int commandWidth = clamp((screenWidth * 2) / 5, 200, 340);
        int partyWidth = screenWidth - commandWidth - margin * 3;

        Rect turnRail = new Rect(margin, margin, railWidth, railHeight);
        Rect enemySummary = new Rect(screenWidth - enemyWidth - margin, margin, enemyWidth, enemyHeight);
        Rect partyStatus = new Rect(margin, screenHeight - bottomHeight - margin, partyWidth, bottomHeight);
        Rect commandStrip = new Rect(partyStatus.right() + margin, partyStatus.y(), commandWidth, bottomHeight);

        int viewportX = turnRail.right() + SPACE_16;
        int viewportY = enemySummary.bottom() + SPACE_12;
        int viewportRight = screenWidth - margin;
        int viewportBottom = partyStatus.y() - SPACE_16;
        Rect reservedWorldViewport = new Rect(
                viewportX,
                viewportY,
                viewportRight - viewportX,
                viewportBottom - viewportY);

        return new BattleHudLayout(turnRail, enemySummary, partyStatus, commandStrip, reservedWorldViewport);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
