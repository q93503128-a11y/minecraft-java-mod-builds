package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BossPresentationGeckoLibResourceIdTest {
    @Test
    void convertsValidatedPhysicalResourcesToGeckoLib5RelativeIds() {
        assertEquals(
            ContentId.rift("entity/region_01_boss"),
            BossPresentationGeckoLibResourceId.model(
                ContentId.rift("geckolib/models/entity/region_01_boss.geo.json")
            ).orElseThrow()
        );
        assertEquals(
            ContentId.rift("entity/region_01_boss"),
            BossPresentationGeckoLibResourceId.animation(
                ContentId.rift("geckolib/animations/entity/region_01_boss.animation.json")
            ).orElseThrow()
        );
    }

    @Test
    void acceptsOptionalShortJsonSuffixWithoutLeakingLibraryRoot() {
        assertEquals(
            ContentId.rift("entity/region_01_boss"),
            BossPresentationGeckoLibResourceId.model(
                ContentId.rift("geckolib/models/entity/region_01_boss.json")
            ).orElseThrow()
        );
        assertEquals(
            ContentId.rift("entity/region_01_boss"),
            BossPresentationGeckoLibResourceId.animation(
                ContentId.rift("geckolib/animations/entity/region_01_boss.json")
            ).orElseThrow()
        );
    }

    @Test
    void rejectsLegacyWrongKindAndUnsuffixedPhysicalPaths() {
        assertTrue(BossPresentationGeckoLibResourceId.model(ContentId.rift("geo/boss/region_01.geo.json")).isEmpty());
        assertTrue(BossPresentationGeckoLibResourceId.model(
            ContentId.rift("geckolib/animations/entity/region_01_boss.animation.json")
        ).isEmpty());
        assertTrue(BossPresentationGeckoLibResourceId.animation(
            ContentId.rift("geckolib/models/entity/region_01_boss.geo.json")
        ).isEmpty());
        assertTrue(BossPresentationGeckoLibResourceId.model(
            ContentId.rift("geckolib/models/entity/region_01_boss")
        ).isEmpty());
    }
}
