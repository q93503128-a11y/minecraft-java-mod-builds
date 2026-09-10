package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.*;
import kr.moonseungjun.riftfrontier.content.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BossAnimationSemanticBindingTest {
    private static final ContentId BOSS = ContentId.parse("riftfrontier:test_boss");
    private static final ContentId ATTACK_A = ContentId.parse("riftfrontier:attack_a");
    private static final ContentId ATTACK_B = ContentId.parse("riftfrontier:attack_b");

    @Test
    void reviewedBindingIsRestrictedToExactServerRequiredAttackKeys() {
        ValidatedBossCombatSemantics semantics = validatedSemantics();
        Set<ContentId> required = semantics.requiredLogicalAnimationKeys();
        ContentId extra = ContentId.parse("riftfrontier:idle_extra");

        BossAnimationSourceBinding raw = reviewedSourceBinding(required, Optional.of(extra));
        BossAnimationSemanticBinding joined = BossAnimationSemanticBinding.validate(semantics, raw);

        assertEquals(BOSS, joined.bossProfile());
        assertEquals(required, joined.requiredLogicalAnimationKeys());
        assertEquals(required, joined.sourceBinding().sourceClipByLogicalKey().keySet());
        assertFalse(joined.sourceBinding().sourceClipByLogicalKey().containsKey(extra));
    }

    @Test
    void missingServerRequiredLogicalKeyFailsClosed() {
        ValidatedBossCombatSemantics semantics = validatedSemantics();
        Set<ContentId> incomplete = new TreeSet<>(semantics.requiredLogicalAnimationKeys());
        incomplete.remove(incomplete.iterator().next());

        BossAnimationSourceBinding raw = reviewedSourceBinding(incomplete, Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> BossAnimationSemanticBinding.validate(semantics, raw));
    }

    @Test
    void wholeClipReviewWithoutFinePhaseWindowsCannotCrossSemanticGate() {
        ValidatedBossCombatSemantics semantics = validatedSemantics();
        Map<ContentId, String> clips = new LinkedHashMap<>();
        for (ContentId key : semantics.requiredLogicalAnimationKeys()) clips.put(key, "AttackClip");
        BossAnimationMotionReview motion = new BossAnimationMotionReview(Map.of(
            "AttackClip", new BossAnimationMotionReview.ApprovedClip("AttackClip", "motion-evidence", "reviewed motion")
        ));
        BossAnimationSourceBinding unpartitioned = new BossAnimationSourceBinding(motion, clips);

        assertThrows(IllegalStateException.class,
            () -> BossAnimationSemanticBinding.validate(semantics, unpartitioned));
    }

    private static ValidatedBossCombatSemantics validatedSemantics() {
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(ATTACK_A, "melee", 1, 1, 0, Set.of("dodge"), "swipe"));
        registry.register(new CoreDefinition.AttackPattern(ATTACK_B, "melee", 1, 1, 0, Set.of("dodge"), "slam"));
        registry.register(new CoreDefinition.BossProfile(BOSS, 2, Set.of(ATTACK_A, ATTACK_B), "sealed_arena"));

        BossCombatSemanticProfile semantics = new BossCombatSemanticProfile(
            BOSS, Map.of(1, Set.of(ATTACK_A), 2, Set.of(ATTACK_B))
        );
        return ValidatedBossCombatSemantics.validate(
            new CombatRuntimeCatalog(registry), semantics, presentation()
        );
    }

    private static BossPresentationProfile presentation() {
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings = new LinkedHashMap<>();
        addPresentation(bindings, "swipe", "melee", "a");
        addPresentation(bindings, "slam", "melee", "b");
        return new BossPresentationProfile(
            ContentId.parse("riftfrontier:test_boss_presentation"), BOSS, "default",
            ContentId.parse("riftfrontier:test_model"), bindings
        );
    }

    private static void addPresentation(
        Map<BossPresentationProfile.BindingKey, BossPresentationProfile.AssetBinding> bindings,
        String cue,
        String delivery,
        String prefix
    ) {
        for (AttackTimeline.Phase phase : List.of(
            AttackTimeline.Phase.TELEGRAPH, AttackTimeline.Phase.ACTIVE, AttackTimeline.Phase.RECOVERY
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

    private static BossAnimationSourceBinding reviewedSourceBinding(
        Set<ContentId> logicalKeys,
        Optional<ContentId> extra
    ) {
        Map<ContentId, String> clips = new LinkedHashMap<>();
        Map<ContentId, BossAnimationSourceBinding.ClipWindow> windows = new LinkedHashMap<>();
        List<BossAnimationPhaseWindowReview.ApprovedWindow> approvals = new ArrayList<>();
        Map<String, BossAnimationMotionReview.ApprovedClip> motionApprovals = new LinkedHashMap<>();

        int index = 0;
        for (ContentId key : new TreeSet<>(logicalKeys)) {
            String clip = "Clip" + index;
            BossAnimationSourceBinding.ClipWindow window = new BossAnimationSourceBinding.ClipWindow(0.0D, 1.0D);
            clips.put(key, clip);
            windows.put(key, window);
            motionApprovals.put(clip, new BossAnimationMotionReview.ApprovedClip(clip, "motion-" + index, "reviewed motion " + index));
            approvals.add(new BossAnimationPhaseWindowReview.ApprovedWindow(
                clip, BossAnimationPhaseWindowReview.Segment.ACTION, window, "window-" + index, "reviewed window " + index
            ));
            index++;
        }
        if (extra.isPresent()) {
            String clip = "ExtraClip";
            BossAnimationSourceBinding.ClipWindow window = new BossAnimationSourceBinding.ClipWindow(0.0D, 1.0D);
            clips.put(extra.orElseThrow(), clip);
            windows.put(extra.orElseThrow(), window);
            motionApprovals.put(clip, new BossAnimationMotionReview.ApprovedClip(clip, "motion-extra", "reviewed extra motion"));
            approvals.add(new BossAnimationPhaseWindowReview.ApprovedWindow(
                clip, BossAnimationPhaseWindowReview.Segment.ACTION, window, "window-extra", "reviewed extra window"
            ));
        }

        return BossAnimationSourceBinding.reviewed(
            new BossAnimationMotionReview(motionApprovals),
            new BossAnimationPhaseWindowReview(approvals),
            clips,
            windows
        );
    }
}
