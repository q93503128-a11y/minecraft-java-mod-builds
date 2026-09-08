package kr.moonseungjun.turnboundre.client.ui;

/** Pure responsive geometry for the compact post-battle result presentation. */
public final class BattleResultLayout {
    public static final int MIN_WIDTH = 480;
    public static final int MIN_HEIGHT = 270;
    private static final int MAX_ROOT_WIDTH = 520;
    private static final int MAX_ROOT_HEIGHT = 230;
    private static final int MARGIN = 16;

    private BattleResultLayout() {}

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (width < 0 || height < 0) throw new IllegalArgumentException("negative rect size");
        }
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean fitsInside(int outerWidth, int outerHeight) {
            return x >= 0 && y >= 0 && right() <= outerWidth && bottom() <= outerHeight;
        }
    }

    public record Layout(Rect root, Rect header, Rect rewards, Rect footer) {}

    public static boolean supports(int width, int height) {
        return width >= MIN_WIDTH && height >= MIN_HEIGHT;
    }

    public static Layout calculate(int width, int height) {
        if (!supports(width, height)) throw new IllegalArgumentException("unsupported battle result canvas");
        int rootWidth = Math.min(MAX_ROOT_WIDTH, width - MARGIN * 2);
        int rootHeight = Math.min(MAX_ROOT_HEIGHT, height - MARGIN * 2);
        int x = (width - rootWidth) / 2;
        int y = (height - rootHeight) / 2;
        Rect root = new Rect(x, y, rootWidth, rootHeight);
        int headerHeight = 32;
        int footerHeight = 24;
        int gap = 6;
        Rect header = new Rect(x, y, rootWidth, headerHeight);
        Rect footer = new Rect(x, y + rootHeight - footerHeight, rootWidth, footerHeight);
        Rect rewards = new Rect(x, header.bottom() + gap, rootWidth,
                Math.max(0, footer.y() - gap - (header.bottom() + gap)));
        return new Layout(root, header, rewards, footer);
    }
}
