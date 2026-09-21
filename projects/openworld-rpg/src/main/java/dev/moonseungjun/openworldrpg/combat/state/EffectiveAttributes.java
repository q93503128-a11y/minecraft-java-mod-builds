package dev.moonseungjun.openworldrpg.combat.state;

public record EffectiveAttributes(
        double vit,
        double end,
        double str,
        double dex,
        double intel,
        double wil
) {
    public EffectiveAttributes {
        requireFiniteNonNegative("vit", vit);
        requireFiniteNonNegative("end", end);
        requireFiniteNonNegative("str", str);
        requireFiniteNonNegative("dex", dex);
        requireFiniteNonNegative("intel", intel);
        requireFiniteNonNegative("wil", wil);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }
}
