package io.github.q93503128.turnbound.client;

import java.util.Map;

/** Runtime framing on top of already baked production-model portraits. */
final class SummonPortraitSpec {
    record Spec(float scale, float xBias, float yBias) {
        Spec {
            scale = Math.max(0.82F, Math.min(1.18F, scale));
            xBias = Math.max(-0.12F, Math.min(0.12F, xBias));
            yBias = Math.max(-0.10F, Math.min(0.10F, yBias));
        }
    }

    private static final Map<String, Spec> SPECS = Map.ofEntries(
            Map.entry("P01", new Spec(1.00F, 0.00F, 0.00F)),
            Map.entry("P02", new Spec(1.04F, 0.02F, 0.01F)),
            Map.entry("P03", new Spec(0.96F, -0.01F, 0.00F)),
            Map.entry("P04", new Spec(1.02F, 0.02F, 0.00F)),
            Map.entry("P05", new Spec(1.02F, 0.00F, 0.00F)),
            Map.entry("P06", new Spec(1.00F, 0.01F, 0.00F)),
            Map.entry("P07", new Spec(1.05F, 0.02F, 0.01F)),
            Map.entry("P08", new Spec(0.96F, -0.01F, 0.00F))
    );

    private SummonPortraitSpec() {}

    static Spec forCharacter(String id) {
        return SPECS.getOrDefault(id, new Spec(0.96F, 0.0F, 0.0F));
    }
}
