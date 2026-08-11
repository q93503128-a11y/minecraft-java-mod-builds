package kr.moonseungjun.villageguardians;

/** Shared bounds used by every transparent Village Guardians screen.
 *  The bottom reserve keeps content away from vanilla health, hunger and hotbar UI.
 */
public final class VillageUiSafeArea {
    private VillageUiSafeArea() {}

    public static Rect screen(int width, int height) {
        int side = clamp(width / 90, 8, 18);
        int top = clamp(height / 70, 8, 16);
        int bottomReserve = clamp(height / 9, 78, 108);
        int right = Math.max(side + 1, width - side);
        int bottom = Math.max(top + 150, height - bottomReserve);
        bottom = Math.min(height - 8, bottom);
        return new Rect(side, top, right, bottom);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Rect(int left, int top, int right, int bottom) {
        public int width() { return right - left; }
        public int height() { return bottom - top; }
        public int centerX() { return (left + right) / 2; }
        public int centerY() { return (top + bottom) / 2; }
        public boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
        public Rect inset(int amount) {
            int safe = Math.max(0, amount);
            return new Rect(left + safe, top + safe, right - safe, bottom - safe);
        }
    }
}
