package kr.moonseungjun.riftfrontier.combat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Canonical production phase-composition source for the first Region 01 boss.
 *
 * <p>This class owns no timing, damage, hit volume or presentation assets. It only loads the authored
 * {@link BossCombatSemanticProfile} that scopes already-published attack patterns to boss phases.</p>
 */
public final class Region01BossProductionSemantics {
    public static final String RESOURCE =
        "/data/riftfrontier/riftfrontier/combat/region_01_first_apex_semantics.json";

    private Region01BossProductionSemantics() {
    }

    public static BossCombatSemanticProfile load() {
        try (InputStream stream = Region01BossProductionSemantics.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing packaged Region 01 boss semantics resource: " + RESOURCE);
            }
            return BossCombatSemanticProfile.decode(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Region 01 boss semantics resource: " + RESOURCE, exception);
        }
    }
}
