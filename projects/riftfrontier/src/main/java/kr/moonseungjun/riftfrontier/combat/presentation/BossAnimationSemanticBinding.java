package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.ValidatedBossCombatSemantics;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Objects;
import java.util.Set;

/**
 * Capability proving that a visually reviewed source-animation binding covers the exact logical attack-animation
 * keys required by already validated server-authoritative boss semantics.
 *
 * <p>This gate does not authorize damage timing, source clip meaning, materials, VFX, sound or encounter attachment.
 * It prevents a reviewed-but-unrelated source binding from entering the Region 01 renderer simply because both sides
 * are individually valid.</p>
 */
public final class BossAnimationSemanticBinding {
    private final ValidatedBossCombatSemantics combatSemantics;
    private final BossAnimationSourceBinding sourceBinding;
    private final Set<ContentId> requiredLogicalAnimationKeys;

    private BossAnimationSemanticBinding(
        ValidatedBossCombatSemantics combatSemantics,
        BossAnimationSourceBinding sourceBinding,
        Set<ContentId> requiredLogicalAnimationKeys
    ) {
        this.combatSemantics = Objects.requireNonNull(combatSemantics, "combatSemantics");
        this.sourceBinding = Objects.requireNonNull(sourceBinding, "sourceBinding");
        this.requiredLogicalAnimationKeys = Set.copyOf(requiredLogicalAnimationKeys);
    }

    public static BossAnimationSemanticBinding validate(
        ValidatedBossCombatSemantics combatSemantics,
        BossAnimationSourceBinding reviewedSourceBinding
    ) {
        Objects.requireNonNull(combatSemantics, "combatSemantics");
        Objects.requireNonNull(reviewedSourceBinding, "reviewedSourceBinding");

        BossAnimationSourceBinding reviewed = reviewedSourceBinding.requireReviewedPhaseWindows();
        Set<ContentId> required = combatSemantics.requiredLogicalAnimationKeys();
        if (required.isEmpty()) {
            throw new IllegalArgumentException("validated boss combat semantics require no logical attack animation keys");
        }

        BossAnimationSourceBinding restricted = reviewed.subset(required).requireReviewedPhaseWindows();
        if (!restricted.sourceClipByLogicalKey().keySet().equals(required)
            || !restricted.sourceWindowByLogicalKey().keySet().equals(required)) {
            throw new IllegalStateException("semantic boss animation binding did not retain exact required logical-key coverage");
        }
        return new BossAnimationSemanticBinding(combatSemantics, restricted, required);
    }

    public ContentId bossProfile() {
        return combatSemantics.bossProfile();
    }

    public ValidatedBossCombatSemantics combatSemantics() {
        return combatSemantics;
    }

    /** Exact reviewed source binding narrowed to server-required attack animation keys. */
    public BossAnimationSourceBinding sourceBinding() {
        return sourceBinding;
    }

    public Set<ContentId> requiredLogicalAnimationKeys() {
        return requiredLogicalAnimationKeys;
    }
}
