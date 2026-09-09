package kr.moonseungjun.riftfrontier.combat.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Direct visual motion review for every source animation carried by the exact accepted Region 01 Dragon Evolved
 * derivation.
 *
 * <p>This catalog deliberately records only motion that was observed from sampled, skinned frames of the accepted
 * art-neutral glTF. It does not assign gameplay meaning or logical animation keys. Production logical bindings must
 * still be authored separately from server-authoritative presentation semantics and validated through
 * {@link BossAnimationSourceBinding}.</p>
 */
public final class Region01BossDragonEvolvedMotionReview {
    public static final String EVIDENCE_RECEIPT =
        "assets/sources/region_01_boss_dragon_evolved.animation_audit.json#motion_semantics.reviewed_clips";

    private static final BossAnimationMotionReview REVIEW = buildReview();

    private Region01BossDragonEvolvedMotionReview() {
    }

    public static BossAnimationMotionReview reviewedSourceMotion() {
        return REVIEW;
    }

    public static BossAnimationMotionReview.ApprovedClip requireReviewed(String sourceClipName) {
        return REVIEW.requireApproved(sourceClipName);
    }

    private static BossAnimationMotionReview buildReview() {
        Map<String, BossAnimationMotionReview.ApprovedClip> reviewed = new LinkedHashMap<>();
        add(reviewed, "Death",
            "large whole-body orientation and silhouette transition away from the starting airborne pose; the final "
                + "sample remains markedly displaced instead of cycling back to the start");
        add(reviewed, "Fast_Flying",
            "rapid bilateral wing and body oscillation across the clip with a cyclic return close to the starting pose");
        add(reviewed, "Flying_Idle",
            "slower bilateral wing and body hover-like oscillation with a cyclic return close to the starting pose");
        add(reviewed, "Headbutt",
            "pronounced early forward/downward head-and-torso drive followed by recovery toward the starting airborne "
                + "pose; the forelimbs are not the dominant moving silhouette");
        add(reviewed, "HitReact",
            "short whole-body recoil/offset that peaks early-to-mid clip and recovers toward the starting pose");
        add(reviewed, "No",
            "repeating neck/head orientation swing over an otherwise shared airborne body cycle; the motion returns "
                + "near the starting orientation");
        add(reviewed, "Punch",
            "one forelimb raises and extends prominently through the early/middle samples while the torso follows, then "
                + "the limb retracts toward the starting pose");
        add(reviewed, "Yes",
            "repeating neck/head orientation change on a different rotational path from No over the shared airborne "
                + "body cycle, returning near the starting orientation");

        Set<String> expected = Set.copyOf(Region01BossDragonEvolvedAnimationContract.EXPECTED_SOURCE_CLIPS);
        if (!reviewed.keySet().equals(expected)) {
            throw new IllegalStateException(
                "Region 01 Dragon Evolved motion review must exactly cover the accepted source clips: expected="
                    + expected + ", reviewed=" + reviewed.keySet()
            );
        }
        return new BossAnimationMotionReview(reviewed);
    }

    private static void add(
        Map<String, BossAnimationMotionReview.ApprovedClip> reviewed,
        String sourceClipName,
        String observedMotion
    ) {
        var previous = reviewed.put(sourceClipName, new BossAnimationMotionReview.ApprovedClip(
            sourceClipName,
            EVIDENCE_RECEIPT + "/" + sourceClipName,
            observedMotion
        ));
        if (previous != null) {
            throw new IllegalStateException("duplicate Region 01 Dragon Evolved motion review for " + sourceClipName);
        }
    }
}
