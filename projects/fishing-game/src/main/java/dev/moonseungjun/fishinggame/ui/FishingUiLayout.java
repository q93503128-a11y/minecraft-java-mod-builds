package dev.moonseungjun.fishinggame.ui;

public final class FishingUiLayout {
    private static final int SCREEN_MARGIN_X = 12;
    private static final int SCREEN_MARGIN_Y = 10;

    private FishingUiLayout() {
    }

    public static Size hud(int guiWidth, int guiHeight) {
        return new Size(
                fit(guiWidth, 138, 8),
                fit(guiHeight, 46, 8)
        );
    }

    public static Size catchBag(int guiWidth, int guiHeight) {
        return new Size(
                fit(guiWidth, 340, SCREEN_MARGIN_X),
                fit(guiHeight, 220, SCREEN_MARGIN_Y)
        );
    }

    public static Size bestiary(int guiWidth, int guiHeight) {
        return new Size(
                fit(guiWidth, 348, SCREEN_MARGIN_X),
                fit(guiHeight, 220, SCREEN_MARGIN_Y)
        );
    }

    public static Size travel(int guiWidth, int guiHeight) {
        return new Size(
                fit(guiWidth, 320, SCREEN_MARGIN_X),
                fit(guiHeight, 206, SCREEN_MARGIN_Y)
        );
    }

    private static int fit(int available, int preferred, int margin) {
        return Math.max(1, Math.min(preferred, Math.max(1, available - margin * 2)));
    }

    public record Size(int width, int height) {
    }
}
