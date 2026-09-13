package kr.moonseungjun.riftfrontier.combat.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Canonical packaged logical presentation profile for the first Region 01 boss. */
public final class Region01BossProductionPresentation {
    public static final String RESOURCE =
        "/data/riftfrontier/riftfrontier/presentation/region_01_first_apex.json";

    private Region01BossProductionPresentation() {
    }

    public static BossPresentationProfile load() {
        try (InputStream stream = Region01BossProductionPresentation.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing packaged Region 01 boss presentation resource: " + RESOURCE);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new BossPresentationProfileCodec().decode(reader);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Region 01 boss presentation resource: " + RESOURCE, exception);
        }
    }
}
