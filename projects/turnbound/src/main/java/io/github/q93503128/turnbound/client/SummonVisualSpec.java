package io.github.q93503128.turnbound.client;

/** Small rarity contract shared by the authored reveal and result-card compositions. */
final class SummonVisualSpec {
    record Spec(int accent, float portraitScale, int breakFlashAlpha) {}

    private SummonVisualSpec() {}

    static Spec forStars(int stars) {
        return switch (Math.max(1, Math.min(5, stars))) {
            case 5 -> new Spec(0xFF865C14, 1.09F, 124);
            case 4 -> new Spec(0xFF61458D, 1.04F, 88);
            case 3 -> new Spec(0xFF246C7D, 1.00F, 54);
            case 2 -> new Spec(0xFF3F795A, 0.97F, 38);
            default -> new Spec(0xFF6F7075, 0.94F, 26);
        };
    }
}
