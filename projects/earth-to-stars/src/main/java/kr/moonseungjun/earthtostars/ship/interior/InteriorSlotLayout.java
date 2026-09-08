package kr.moonseungjun.earthtostars.ship.interior;

public final class InteriorSlotLayout {
    public static final int CELL_SIZE = 2048;
    public static final int GRID_WIDTH = 8192;
    public static final int HALF_GRID = GRID_WIDTH / 2;
    public static final int FLOOR_Y = 64;
    public static final long MAX_SLOTS = (long) GRID_WIDTH * GRID_WIDTH;

    private InteriorSlotLayout() {
    }

    public static InteriorAnchor anchor(long slot) {
        requireValidSlot(slot);
        long row = slot / GRID_WIDTH;
        long column = slot % GRID_WIDTH;
        double x = (column - HALF_GRID) * (double) CELL_SIZE;
        double z = (row - HALF_GRID) * (double) CELL_SIZE;
        return new InteriorAnchor(x, FLOOR_Y + 1.0D, z);
    }

    public static long slotAt(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("interior position must be finite");
        }
        long column = Math.round(x / CELL_SIZE) + HALF_GRID;
        long row = Math.round(z / CELL_SIZE) + HALF_GRID;
        if (column < 0 || column >= GRID_WIDTH || row < 0 || row >= GRID_WIDTH) {
            throw new IllegalArgumentException("position is outside interior allocation grid");
        }
        return row * GRID_WIDTH + column;
    }

    public static boolean contains(long slot, double x, double z) {
        requireValidSlot(slot);
        try {
            return slotAt(x, z) == slot;
        } catch (IllegalArgumentException outside) {
            return false;
        }
    }

    public static void requireValidSlot(long slot) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            throw new IllegalArgumentException("invalid interior slot: " + slot);
        }
    }
}
