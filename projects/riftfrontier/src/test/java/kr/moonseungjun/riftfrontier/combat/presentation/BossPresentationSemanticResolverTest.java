package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.BossCombatSemanticProfile;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.combat.CombatRuntimeCatalog;
import kr.moonseungjun.riftfrontier.combat.ValidatedBossCombatSemantics;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BossPresentationSemanticResolverTest {
    private static final ContentId BOSS = ContentId.parse("riftfrontier:semantic_resolver_boss");
    private static final ContentId ATTACK_A = ContentId.parse("riftfrontier:semantic_attack_a");
    private static final ContentId ATTACK_B = ContentId.parse("riftfrontier:semantic_attack_b");

    @Test
    void validatedResolverCrossChecksPatternCueAndDeliveryBeforeEmittingAssetKeys() {
        BossPresentationProfile profile = presentation();
        ValidatedBossCombatSemantics semantics = validated(profile);
        BossPresentationResolver resolver = BossPresentationResolver.validated(semantics);

        BossPresentationSemanticState good = active(ATTACK_A, "swipe", "melee", AttackTimeline.Phase.ACTIVE, true);
        var resolved = resolver.resolve(BOSS, "default", good).orElseThrow();
        assertEquals(ContentId.parse("riftfrontier:a_active"), resolved.animationKey());
        assertTrue(resolver.isValidatedBy(semantics));

        BossPresentationSemanticState spoofedCue = active(ATTACK_A, "slam", "melee", AttackTimeline.Phase.ACTIVE, true);
        BossPresentationResolver generic = new BossPresentationResolver(List.of(profile));
        assertEquals(
            ContentId.parse("riftfrontier:b_active"),
            generic.resolve(BOSS, "default", spoofedCue).orElseThrow().animationKey(),
            "generic resolver demonstrates why publication must not accept a free-standing resolver"
        );
        assertTrue(resolver.resolve(BOSS, "default", spoofedCue).isEmpty(),
            "validated resolver must reject cue semantics belonging to another attack pattern");

        BossPresentationSemanticState spoofedDelivery = active(ATTACK_A, "swipe", "projectile", AttackTimeline.Phase.ACTIVE, true);
        assertTrue(resolver.resolve(BOSS, "default", spoofedDelivery).isEmpty());
    }

    @Test
    void validatedResolverRejectsAttackOutsideSemanticPoolAndWrongContext() {
        ValidatedBossCombatSemantics semantics = validated(presentation());
        BossPresentationResolver resolver = BossPresentationResolver.validated(semantics);

        BossPresentationSemanticState unknownAttack = active(
            ContentId.parse("riftfrontier:not_in_boss_semantics"),
            "swipe",
            "melee",
            AttackTimeline.Phase.TELEGRAPH,
            false
        );
        assertTrue(resolver.resolve(BOSS, "default", unknownAttack).isEmpty());
        assertTrue(resolver.resolve(ContentId.parse("riftfrontier:other_boss"), "default",
            active(ATTACK_A, "swipe", "melee", AttackTimeline.Phase.TELEGRAPH, false)).isEmpty());
        assertTrue(resolver.resolve(BOSS, "phase2",
            active(ATTACK_A, "swipe", "melee", AttackTimeline.Phase.TELEGRAPH, false)).isEmpty());

        ValidatedBossCombatSemantics otherCapability = validated(presentation());
        assertFalse(resolver.isValidatedBy(otherCapability),
            "semantic resolver provenance is capability identity, not merely equal-looking data");
    }

    private static BossPresentationSemanticState active(
        ContentId attack,
        String cue,
        String delivery,
        AttackTimeline.Phase phase,
        boolean hitWindowOpen
    ) {
        return new BossPresentationSemanticState(
            41,
            200L,
            true,
            1,
            attack.toString(),
            phase.name(),
            0.5D,
            cue,
            delivery,
            List.of("dodge"),
            hitWindowOpen
        );
    }

    private static ValidatedBossCombatSemantics validated(BossPresentationProfile profile) {
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(ATTACK_A, "melee", 2, 1, 1, Set.of("dodge"), "swipe"));
        registry.register(new CoreDefinition.AttackPattern(ATTACK_B, "melee", 2, 1, 1, Set.of("dodge"), "slam"));
        registry.register(new CoreDefinition.BossProfile(BOSS, 2, Set.of(ATTACK_A, ATTACK_B), "sealed_arena"));
        BossCombatSemanticProfile semanticProfile = new BossCombatSemanticProfile(
            BOSS,
            Map.of(1, Set.of(ATTACK_A), 2, Set.of(ATTACK_B))
        );
        return ValidatedBossCombatSemantics.validate(new CombatRuntimeCatalog(registry), semanticProfile, profile);
    }

    private static BossPresentationProfile presentation() {
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new LinkedHashMap<>();
        add(bindings, "swipe", "melee", "a");
        add(bindings, "slam", "melee", "b");
        return new BossPresentationProfile(
            ContentId.parse("riftfrontier:semantic_resolver_presentation"),
            BOSS,
            "default",
            ContentId.parse("riftfrontier:semantic_resolver_model"),
            bindings
        );
    }

    private static void add(
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings,
        String cue,
        String delivery,
        String prefix
    ) {
        for (AttackTimeline.Phase phase : List.of(
            AttackTimeline.Phase.TELEGRAPH,
            AttackTimeline.Phase.ACTIVE,
            AttackTimeline.Phase.RECOVERY
        )) {
            String suffix = phase.name().toLowerCase(Locale.ROOT);
            bindings.put(
                new BossPresentationProfile.BindingKey(cue, delivery, phase),
                new BossPresentationProfile.AssetBinding(
                    ContentId.parse("riftfrontier:" + prefix + "_" + suffix),
                    ContentId.parse("riftfrontier:" + prefix + "_vfx_" + suffix),
                    ContentId.parse("riftfrontier:" + prefix + "_sound_" + suffix)
                )
            );
        }
    }
}
