package kr.moonseungjun.turnboundre.client.ui;

/** Pure logical-coordinate layout contracts for TURNBOUND: RE production UI. */
public final class UiLayoutMetrics {
    public static final int SPACE_2 = 2;
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_24 = 24;

    public static final int MIN_BATTLE_HUD_WIDTH = 480;
    public static final int MIN_BATTLE_HUD_HEIGHT = 270;
    public static final int MIN_PARTY_SCREEN_WIDTH = 480;
    public static final int MIN_PARTY_SCREEN_HEIGHT = 270;

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

    /**
     * Party status stays one row on normal layouts. At the minimum supported width, four members become a 2x2 grid
     * so names and core resources are not crushed into ~64 logical pixels each.
     */
    public record PartyGridLayout(
            int columns,
            int rows,
            int cellWidth,
            int cellHeight,
            boolean compact
    ) {
        public PartyGridLayout {
            if (columns <= 0 || rows <= 0 || cellWidth <= 0 || cellHeight <= 0) {
                throw new IllegalArgumentException("invalid party grid");
            }
        }
    }

    /**
     * Standalone Party Formation follows the canonical three-region structure:
     * scan-friendly roster, four active slots, and a persistent selected-character detail pane.
     */
    public record PartyFormationLayout(
            Rect root,
            Rect header,
            Rect tabs,
            Rect roster,
            Rect activeParty,
            Rect selectedDetail,
            Rect footer
    ) {}

    /**
     * Target selection reuses the battle command region rather than opening a center-screen modal.
     * The header owns back/confirm/page controls and the grid owns only compact target choices.
     */
    public record TargetChooserLayout(
            Rect region,
            Rect header,
            Rect grid,
            int columns,
            int rows,
            int pageSize
    ) {
        public TargetChooserLayout {
            if (columns <= 0 || rows <= 0 || pageSize != columns * rows) {
                throw new IllegalArgumentException("invalid target chooser grid");
            }
        }
    }

    /** Rendering quietly defers at extreme GUI scales instead of throwing every frame. */
    public static boolean supportsBattleHud(int screenWidth, int screenHeight) {
        return screenWidth >= MIN_BATTLE_HUD_WIDTH && screenHeight >= MIN_BATTLE_HUD_HEIGHT;
    }

    public static boolean supportsPartyScreen(int screenWidth, int screenHeight) {
        return screenWidth >= MIN_PARTY_SCREEN_WIDTH && screenHeight >= MIN_PARTY_SCREEN_HEIGHT;
    }

    public static BattleHudLayout battleHud(int screenWidth, int screenHeight) {
        if (!supportsBattleHud(screenWidth, screenHeight)) {
            throw new IllegalArgumentException(
                    "battle HUD requires at least " + MIN_BATTLE_HUD_WIDTH + "x" + MIN_BATTLE_HUD_HEIGHT
                            + " logical pixels, got " + screenWidth + "x" + screenHeight);
        }

        int margin = SPACE_8;
        int bottomHeight = screenWidth < 600 ? 82 : clamp(screenHeight / 5, 58, 82);
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

    public static PartyFormationLayout partyFormation(int screenWidth, int screenHeight) {
        if (!supportsPartyScreen(screenWidth, screenHeight)) {
            throw new IllegalArgumentException(
                    "party screen requires at least " + MIN_PARTY_SCREEN_WIDTH + "x" + MIN_PARTY_SCREEN_HEIGHT
                            + " logical pixels, got " + screenWidth + "x" + screenHeight);
        }

        int rootWidth = Math.min(screenWidth - SPACE_16, 960);
        int rootX = (screenWidth - rootWidth) / 2;
        int rootY = SPACE_8;
        int rootHeight = screenHeight - SPACE_16;
        Rect root = new Rect(rootX, rootY, rootWidth, rootHeight);

        int headerHeight = 24;
        int tabsHeight = 20;
        int footerHeight = 24;
        Rect header = new Rect(root.x(), root.y(), root.width(), headerHeight);
        Rect tabs = new Rect(root.x(), header.bottom() + SPACE_4, root.width(), tabsHeight);
        Rect footer = new Rect(root.x(), root.bottom() - footerHeight, root.width(), footerHeight);

        int contentY = tabs.bottom() + SPACE_8;
        int contentHeight = footer.y() - SPACE_8 - contentY;
        int gap = SPACE_8;
        int rosterWidth = clamp((root.width() * 35) / 100, 170, 300);
        int activeWidth = clamp((root.width() * 25) / 100, 120, 220);
        int detailWidth = root.width() - rosterWidth - activeWidth - gap * 2;
        if (detailWidth < 150) {
            int deficit = 150 - detailWidth;
            int rosterShrink = Math.min(deficit, Math.max(0, rosterWidth - 160));
            rosterWidth -= rosterShrink;
            deficit -= rosterShrink;
            activeWidth -= Math.min(deficit, Math.max(0, activeWidth - 110));
            detailWidth = root.width() - rosterWidth - activeWidth - gap * 2;
        }

        Rect roster = new Rect(root.x(), contentY, rosterWidth, contentHeight);
        Rect active = new Rect(roster.right() + gap, contentY, activeWidth, contentHeight);
        Rect detail = new Rect(active.right() + gap, contentY, detailWidth, contentHeight);
        return new PartyFormationLayout(root, header, tabs, roster, active, detail, footer);
    }

    public static PartyGridLayout partyGrid(Rect region, int partySize) {
        if (partySize <= 0) throw new IllegalArgumentException("partySize must be > 0");
        int count = Math.min(4, partySize);
        boolean compact = region.width() < 360 && count > 2;
        int columns = compact ? 2 : count;
        int rows = (count + columns - 1) / columns;
        return new PartyGridLayout(
                columns,
                rows,
                Math.max(1, region.width() / columns),
                Math.max(1, region.height() / rows),
                compact);
    }

    public static TargetChooserLayout targetChooser(int screenWidth, int screenHeight) {
        BattleHudLayout hud = battleHud(screenWidth, screenHeight);
        Rect region = hud.commandStrip();
        int headerHeight = 18;
        int gridY = region.y() + headerHeight + SPACE_2;
        int gridHeight = region.bottom() - gridY;
        Rect header = new Rect(region.x(), region.y(), region.width(), headerHeight);
        Rect grid = new Rect(region.x(), gridY, region.width(), gridHeight);

        int columns = region.width() >= 280 ? 3 : 2;
        int targetRowHeight = 20;
        int rows = clamp((grid.height() + SPACE_2) / (targetRowHeight + SPACE_2), 1, 2);
        return new TargetChooserLayout(region, header, grid, columns, rows, columns * rows);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
