package kr.moonseungjun.riftfrontier.content;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossCombatSemanticProfile;
import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.ValidatedBossCombatSemantics;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PublishedBossPresentationGenerationTest {
    @Test
    void validatedBossSemanticsRetainAndEnforcePublishedSnapshotGeneration() {
        ContentId attack = ContentId.rift("test/published_generation_attack");
        ContentId boss = ContentId.rift("test/published_generation_boss");
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(
            attack,
            "line_charge",
            2,
            1,
            2,
            Set.of("sidestep"),
            "published_generation_cue"
        ));
        registry.register(new CoreDefinition.BossProfile(boss, 1, Set.of(attack), "keep_escape_lane"));

        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new LinkedHashMap<>();
        for (AttackTimeline.Phase phase : List.of(
            AttackTimeline.Phase.TELEGRAPH,
            AttackTimeline.Phase.ACTIVE,
            AttackTimeline.Phase.RECOVERY
        )) {
            String suffix = phase.name().toLowerCase(Locale.ROOT);
            bindings.put(
                new BossPresentationProfile.BindingKey("published_generation_cue", "line_charge", phase),
                new BossPresentationProfile.AssetBinding(
                    ContentId.rift("test/published_generation_anim_" + suffix),
                    ContentId.rift("test/published_generation_vfx_" + suffix),
                    ContentId.rift("test/published_generation_sound_" + suffix)
                )
            );
        }
        BossPresentationProfile presentation = new BossPresentationProfile(
            ContentId.rift("test/published_generation_presentation"),
            boss,
            "default",
            ContentId.rift("test/published_generation_model"),
            bindings
        );

        ContentRuntimeSnapshot snapshot = new ContentRuntimeSnapshot(
            42L,
            Instant.EPOCH,
            List.of("test"),
            registry,
            List.of(presentation),
            Optional.empty()
        );
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(snapshot);
        ValidatedBossCombatSemantics semantics = ValidatedBossCombatSemantics.validate(
            catalog,
            new BossCombatSemanticProfile(boss, Map.of(1, Set.of(attack))),
            presentation
        );

        assertEquals(42L, semantics.publishedGeneration().orElseThrow());
        assertDoesNotThrow(() -> semantics.requirePublishedGeneration(42L));
        assertThrows(IllegalStateException.class, () -> semantics.requirePublishedGeneration(43L));
        assertThrows(IllegalStateException.class, () -> semantics.requirePublishedGeneration(-1L));
    }
}
