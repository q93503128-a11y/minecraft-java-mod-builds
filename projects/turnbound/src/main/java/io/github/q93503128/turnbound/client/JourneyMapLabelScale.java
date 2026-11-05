package io.github.q93503128.turnbound.client;

/** Pure JourneyMap label-size curve kept free of JourneyMap classes so unit tests do not need the optional API. */
final class JourneyMapLabelScale {
    private JourneyMapLabelScale() {}

    static float forZoom(int zoom) {
        int safeZoom = Math.max(1024, zoom);
        float proportional = 1.15F * (safeZoom / 2048.0F);
        return Math.max(0.90F, Math.min(3.00F, proportional));
    }
}
