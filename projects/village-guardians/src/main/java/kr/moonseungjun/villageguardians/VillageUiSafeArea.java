package kr.moonseungjun.villageguardians;

/**
 * Shared placement contract for translucent in-world screens.
 * Vanilla HUD layers are suppressed while Village Guardians modal screens are open,
 * so this rectangle only needs small edge margins instead of a permanent hotbar reserve.
 */
public final class VillageUiSafeArea {
    private VillageUiSafeArea() {}

    public static Rect screen(int width, int height) {
        int side = clamp(width / 52, 7, 16);
        int topPadding = clamp(height / 80, 6, 12);
        int bottomPadding = clamp(height / 70, 7, 14);
        int bottom = Math.max(topPadding + 1, height - bottomPadding);
        return new Rect(side, topPadding,
                Math.max(side + 1, width - side),
                Math.max(topPadding + 1, bottom));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static record Rect(int left, int top, int right, int bottom) {
        public int width() { return Math.max(1, right - left); }
        public int height() { return Math.max(1, bottom - top); }
        public int centerX() { return left + width() / 2; }
        public int centerY() { return top + height() / 2; }

        public int clampX(int x, int radius) {
            return clamp(x, left + radius, Math.max(left + radius, right - radius));
        }

        public int clampY(int y, int radius) {
            return clamp(y, top + radius, Math.max(top + radius, bottom - radius));
        }
    }
}
