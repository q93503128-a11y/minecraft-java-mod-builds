package kr.moonseungjun.turnboundre.client.ui;

/** Pure layout math for fitting a living entity beside readable character facts. */
public final class EntityPreviewLayout {
    private static final int MIN_REGION_WIDTH = 210;
    private static final int MIN_REGION_HEIGHT = 120;
    private static final int MIN_TEXT_WIDTH = 118;
    private static final int MAX_RENDER_SCALE = 48;
    private static final int COMPACT_INSET = 2;

    private EntityPreviewLayout() {}

    public record PreviewSpec(
            boolean visible,
            int xOffset,
            int yOffset,
            int width,
            int height,
            int renderScale,
            int textWidth
    ) {
        public PreviewSpec {
            if (xOffset < 0 || yOffset < 0 || width < 1 || height < 1 || renderScale < 1 || textWidth < 1) {
                throw new IllegalArgumentException("invalid entity preview geometry");
            }
        }

        public static PreviewSpec hidden(int regionWidth) {
            return new PreviewSpec(false, 0, 0, 1, 1, 1, Math.max(1, regionWidth));
        }

        public boolean fitsInside(int regionWidth, int regionHeight) {
            return !visible || xOffset + width <= regionWidth && yOffset + height <= regionHeight;
        }
    }

    /** Character-detail layout that preserves a readable text column. */
    public static PreviewSpec fit(int regionWidth, int regionHeight, float entityWidth, float entityHeight) {
        if (regionWidth < MIN_REGION_WIDTH || regionHeight < MIN_REGION_HEIGHT
                || !validEntityDimensions(entityWidth, entityHeight)) {
            return PreviewSpec.hidden(regionWidth);
        }

        int boxWidth = clamp(regionWidth / 3, 72, 104);
        int boxHeight = clamp(regionHeight - 32, 80, 116);
        int textWidth = regionWidth - boxWidth - UiLayoutMetrics.SPACE_8;
        if (textWidth < MIN_TEXT_WIDTH) return PreviewSpec.hidden(regionWidth);

        int innerWidth = Math.max(1, boxWidth - 12);
        int innerHeight = Math.max(1, boxHeight - 8);
        int scale = fitScale(innerWidth, innerHeight, entityWidth, entityHeight);

        return new PreviewSpec(true, regionWidth - boxWidth, 18, boxWidth, boxHeight, scale, textWidth);
    }

    /**
     * Small source-backed lineup layout for places such as Expedition Journal encounter rows.
     * This contract reserves the whole slot for the entity and intentionally carries no text column.
     */
    public static PreviewSpec fitCompact(int regionWidth, int regionHeight, float entityWidth, float entityHeight) {
        if (regionWidth < COMPACT_INSET * 2 + 1 || regionHeight < COMPACT_INSET * 2 + 1
                || !validEntityDimensions(entityWidth, entityHeight)) {
            return PreviewSpec.hidden(regionWidth);
        }
        int innerWidth = Math.max(1, regionWidth - COMPACT_INSET * 2);
        int innerHeight = Math.max(1, regionHeight - COMPACT_INSET * 2);
        int scale = fitScale(innerWidth, innerHeight, entityWidth, entityHeight);
        return new PreviewSpec(true, 0, 0, regionWidth, regionHeight, scale, 1);
    }

    private static int fitScale(int innerWidth, int innerHeight, float entityWidth, float entityHeight) {
        int scale = (int) Math.floor(Math.min(innerWidth / entityWidth, innerHeight / entityHeight));
        return clamp(scale, 1, MAX_RENDER_SCALE);
    }

    private static boolean validEntityDimensions(float entityWidth, float entityHeight) {
        return Float.isFinite(entityWidth) && Float.isFinite(entityHeight)
                && entityWidth > 0.0F && entityHeight > 0.0F;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
