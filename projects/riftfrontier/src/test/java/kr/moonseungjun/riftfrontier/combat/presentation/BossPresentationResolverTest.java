package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class BossPresentationResolverTest {
    private static final ContentId BOSS = ContentId.parse("riftfrontier:resolver_boss");
    private static final ContentId ATTACK = ContentId.parse("riftfrontier:resolver_attack");

    @Test
    void resolverUsesAuthoritativeCueDeliveryAndPhaseWithoutOwningTiming() {
        BossPresentationProfile profile = completeProfile();
        BossPresentationResolver resolver = new BossPresentationResolver(List.of(profile));
        var state = activeState("ACTIVE", true);

        var resolved = resolver.resolve(BOSS, "base", state).orElseThrow();
        assertEquals(ContentId.parse("riftfrontier:models/resolver_boss"), resolved.modelKey());
        assertEquals(ContentId.parse("riftfrontier:animations/charge_active"), resolved.animationKey());
        assertEquals(ContentId.parse("riftfrontier:vfx/charge_active"), resolved.vfxKey());
        assertEquals(ContentId.parse("riftfrontier:sounds/charge_active"), resolved.soundKey());
        assertEquals(state.phaseProgress(), resolved.phaseProgress(), 0.000001D);
        assertTrue(resolved.hitWindowOpen());
    }

    @Test
    void missingVariantOrSelectorFailsClosed() {
        BossPresentationResolver resolver = new BossPresentationResolver(List.of(completeProfile()));
        assertTrue(resolver.resolve(BOSS, "elite", activeState("ACTIVE", true)).isEmpty());

        var unmatched = new BossPresentationSemanticState(
            1, 10L, true, 1, ATTACK.toString(), "ACTIVE", 0.5D,
            "different_cue", "line_charge", List.of("sidestep"), true
        );
        assertTrue(resolver.resolve(BOSS, "base", unmatched).isEmpty());
    }

    @Test
    void validatorRequiresFullAuthoredCoverageForBossAttackSemantics() {
        ContentRegistry registry = registry();
        BossPresentationProfile valid = completeProfile();
        var validator = new BossPresentationProfileValidator();
        assertFalse(validator.validate(registry, List.of(valid)).hasErrors());

        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> missing = new java.util.LinkedHashMap<>(valid.bindings());
        missing.remove(key("charge", "line_charge", "ACTIVE"));
        BossPresentationProfile incomplete = new BossPresentationProfile(valid.id(), valid.bossProfile(), valid.variant(), valid.modelKey(), missing);
        var report = validator.validate(registry, List.of(incomplete));
        assertEquals(1, report.byCode(BossPresentationProfileValidator.Code.MISSING_PRESENTATION_BINDING).size());
    }

    @Test
    void codecRejectsDuplicateSelectorsAndParsesStableLogicalKeys() {
        String oneBinding = """
            {"kind":"boss_presentation_profile","id":"riftfrontier:boss_present","boss_profile":"riftfrontier:resolver_boss","variant":"base","model_key":"riftfrontier:models/resolver_boss","bindings":[
              {"presentation_cue":"charge","delivery":"line_charge","phase":"ACTIVE","animation_key":"riftfrontier:animations/charge_active","vfx_key":"riftfrontier:vfx/charge_active","sound_key":"riftfrontier:sounds/charge_active"}
            ]}
            """;
        BossPresentationProfile decoded = new BossPresentationProfileCodec().decode(oneBinding);
        assertEquals(BOSS, decoded.bossProfile());
        assertTrue(decoded.bindings().containsKey(key("charge", "line_charge", "ACTIVE")));

        String duplicate = """
            {"kind":"boss_presentation_profile","id":"riftfrontier:boss_present","boss_profile":"riftfrontier:resolver_boss","variant":"base","model_key":"riftfrontier:models/resolver_boss","bindings":[
              {"presentation_cue":"charge","delivery":"line_charge","phase":"ACTIVE","animation_key":"riftfrontier:animations/charge_active","vfx_key":"riftfrontier:vfx/charge_active","sound_key":"riftfrontier:sounds/charge_active"},
              {"presentation_cue":"charge","delivery":"line_charge","phase":"ACTIVE","animation_key":"riftfrontier:animations/charge_active","vfx_key":"riftfrontier:vfx/charge_active","sound_key":"riftfrontier:sounds/charge_active"}
            ]}
            """;
        assertThrows(IllegalArgumentException.class, () -> new BossPresentationProfileCodec().decode(duplicate));
    }

    @Test
    void duplicateBossVariantContextIsRejected() {
        BossPresentationProfile first = completeProfile();
        BossPresentationProfile second = new BossPresentationProfile(
            ContentId.parse("riftfrontier:boss_present_2"), BOSS, "base",
            ContentId.parse("riftfrontier:models/resolver_boss_alt"), first.bindings()
        );
        assertThrows(IllegalArgumentException.class, () -> new BossPresentationResolver(List.of(first, second)));
        assertEquals(1, new BossPresentationProfileValidator().validate(registry(), List.of(first, second))
            .byCode(BossPresentationProfileValidator.Code.DUPLICATE_PRESENTATION_CONTEXT).size());
    }

    @Test
    void assetManifestRequiresEveryProfileKeyWithTheCorrectProductionRole() {
        BossPresentationProfile profile = completeProfile();
        BossPresentationAssetManifest valid = completeManifest(profile);
        assertFalse(valid.validateProfiles(List.of(profile)).hasErrors());

        List<BossPresentationAssetManifest.Asset> missingSound = new ArrayList<>(valid.assets().values());
        missingSound.removeIf(asset -> asset.logicalKey().equals(ContentId.parse("riftfrontier:sounds/charge_active")));
        var missingReport = new BossPresentationAssetManifest(missingSound).validateProfiles(List.of(profile));
        assertEquals(1, missingReport.byCode(BossPresentationAssetManifest.Code.MISSING_LOGICAL_KEY).size());

        List<BossPresentationAssetManifest.Asset> wrongKind = new ArrayList<>(valid.assets().values());
        wrongKind.removeIf(asset -> asset.logicalKey().equals(ContentId.parse("riftfrontier:animations/charge_active")));
        wrongKind.add(new BossPresentationAssetManifest.Asset(
            ContentId.parse("riftfrontier:animations/charge_active"), BossPresentationAssetManifest.Kind.VFX,
            ContentId.parse("riftfrontier:actual/charge_active.animation.json"), "authored-test", "test-only"
        ));
        var wrongKindReport = new BossPresentationAssetManifest(wrongKind).validateProfiles(List.of(profile));
        assertEquals(1, wrongKindReport.byCode(BossPresentationAssetManifest.Code.WRONG_ASSET_KIND).size());
    }

    @Test
    void assetManifestPhysicalProbeFailsClosedWithoutInventingPlaceholderResources() {
        BossPresentationAssetManifest manifest = completeManifest(completeProfile());
        var report = manifest.validateResources((kind, resourceId) -> !resourceId.path().contains("charge_active.sound"));
        assertEquals(1, report.byCode(BossPresentationAssetManifest.Code.MISSING_RESOURCE).size());
        assertEquals(ContentId.parse("riftfrontier:sounds/charge_active"),
            report.byCode(BossPresentationAssetManifest.Code.MISSING_RESOURCE).getFirst().logicalKey());
    }

    private static ContentRegistry registry() {
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(ATTACK, "line_charge", 4, 3, 5, Set.of("sidestep"), "charge"));
        registry.register(new CoreDefinition.BossProfile(BOSS, 2, Set.of(ATTACK), "bounded_arena"));
        return registry;
    }

    private static BossPresentationProfile completeProfile() {
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new java.util.LinkedHashMap<>();
        for (String phase : List.of("TELEGRAPH", "ACTIVE", "RECOVERY")) {
            String suffix = phase.toLowerCase(java.util.Locale.ROOT);
            bindings.put(key("charge", "line_charge", phase), new BossPresentationProfile.AssetBinding(
                ContentId.parse("riftfrontier:animations/charge_" + suffix),
                ContentId.parse("riftfrontier:vfx/charge_" + suffix),
                ContentId.parse("riftfrontier:sounds/charge_" + suffix)
            ));
        }
        return new BossPresentationProfile(
            ContentId.parse("riftfrontier:boss_present"), BOSS, "base",
            ContentId.parse("riftfrontier:models/resolver_boss"), bindings
        );
    }

    private static BossPresentationAssetManifest completeManifest(BossPresentationProfile profile) {
        List<BossPresentationAssetManifest.Asset> assets = new ArrayList<>();
        assets.add(new BossPresentationAssetManifest.Asset(
            profile.modelKey(), BossPresentationAssetManifest.Kind.MODEL,
            ContentId.parse("riftfrontier:actual/resolver_boss.geo.json"), "authored-test", "test-only"
        ));
        for (BossPresentationProfile.AssetBinding binding : profile.bindings().values()) {
            assets.add(new BossPresentationAssetManifest.Asset(
                binding.animationKey(), BossPresentationAssetManifest.Kind.ANIMATION,
                ContentId.parse("riftfrontier:actual/" + binding.animationKey().path().replace("animations/", "") + ".animation.json"),
                "authored-test", "test-only"
            ));
            assets.add(new BossPresentationAssetManifest.Asset(
                binding.vfxKey(), BossPresentationAssetManifest.Kind.VFX,
                ContentId.parse("riftfrontier:actual/" + binding.vfxKey().path().replace("vfx/", "") + ".vfx.json"),
                "authored-test", "test-only"
            ));
            assets.add(new BossPresentationAssetManifest.Asset(
                binding.soundKey(), BossPresentationAssetManifest.Kind.SOUND,
                ContentId.parse("riftfrontier:actual/" + binding.soundKey().path().replace("sounds/", "") + ".sound.json"),
                "authored-test", "test-only"
            ));
        }
        return new BossPresentationAssetManifest(assets);
    }

    private static BossPresentationProfile.BindingKey key(String cue, String delivery, String phase) {
        return new BossPresentationProfile.BindingKey(cue, delivery, kr.moonseungjun.riftfrontier.combat.AttackTimeline.Phase.valueOf(phase));
    }

    private static BossPresentationSemanticState activeState(String phase, boolean hitWindowOpen) {
        return new BossPresentationSemanticState(
            1, 10L, true, 1, ATTACK.toString(), phase, 0.5D,
            "charge", "line_charge", new ArrayList<>(List.of("sidestep")), hitWindowOpen
        );
    }
}
