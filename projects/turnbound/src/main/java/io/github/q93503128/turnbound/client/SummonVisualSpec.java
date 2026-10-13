package io.github.q93503128.turnbound.client;

/** Rarity-specific presentation intensity without duplicating the reveal timeline. */
final class SummonVisualSpec {
    record Spec(int accent, float glowScale, float portraitScale, boolean rareFx, boolean legendaryFx) {}

    private SummonVisualSpec() {}

    static Spec forStars(int stars) {
        return switch (Math.max(1, Math.min(5, stars))) {
            case 5 -> new Spec(0xFFFFC857, 1.18F, 1.05F, true, true);
            case 4 -> new Spec(0xFFC794FF, 1.10F, 1.02F, true, false);
            case 3 -> new Spec(0xFF6DC6FF, 1.04F, 1.00F, false, false);
            case 2 -> new Spec(0xFF62D39A, 1.00F, 0.98F, false, false);
            default -> new Spec(0xFFAEB7C6, 0.96F, 0.96F, false, false);
        };
    }
}
