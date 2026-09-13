package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reviewed, staged source-animation mapping for the Region 01 Dragon Evolved boss attacks whose motion-role fit has
 * been explicitly accepted. This is deliberately not a complete production {@link BossAnimationSemanticBinding}:
 * arena pressure remains unresolved and therefore the renderer's exact-coverage gate must continue to fail closed.
 */
public final class Region01BossDragonEvolvedStagedAttackBinding {
    public static final String EVIDENCE_RECEIPT =
        "assets/sources/region_01_boss_dragon_evolved.semantic_role_review.json";

    private static final ContentId STRIKE_TELEGRAPH = key("region_01_committed_strike/telegraph");
    private static final ContentId STRIKE_ACTIVE = key("region_01_committed_strike/active");
    private static final ContentId STRIKE_RECOVERY = key("region_01_committed_strike/recovery");
    private static final ContentId LINE_TELEGRAPH = key("region_01_line_displacement/telegraph");
    private static final ContentId LINE_ACTIVE = key("region_01_line_displacement/active");
    private static final ContentId LINE_RECOVERY = key("region_01_line_displacement/recovery");
    private static final Set<ContentId> UNRESOLVED = Set.of(
        key("region_01_arena_pressure/telegraph"),
        key("region_01_arena_pressure/active"),
        key("region_01_arena_pressure/recovery")
    );

    private static final BossAnimationSourceBinding BINDING = build();

    private Region01BossDragonEvolvedStagedAttackBinding() {}

    public static BossAnimationSourceBinding reviewedResolvedAttacks() {
        return BINDING;
    }

    public static Set<ContentId> unresolvedLogicalAnimationKeys() {
        return UNRESOLVED;
    }

    private static BossAnimationSourceBinding build() {
        Map<ContentId, String> clips = new LinkedHashMap<>();
        Map<ContentId, BossAnimationSourceBinding.ClipWindow> windows = new LinkedHashMap<>();

        bind(clips, windows, STRIKE_TELEGRAPH, "Punch", 0.0D, 0.2D);
        bind(clips, windows, STRIKE_ACTIVE, "Punch", 0.2D, 0.275D);
        bind(clips, windows, STRIKE_RECOVERY, "Punch", 0.275D, 1.0D);

        bind(clips, windows, LINE_TELEGRAPH, "Headbutt", 0.0D, 0.15555555555555556D);
        bind(clips, windows, LINE_ACTIVE, "Headbutt", 0.15555555555555556D, 0.2D);
        bind(clips, windows, LINE_RECOVERY, "Headbutt", 0.2D, 0.4888888888888889D);

        return BossAnimationSourceBinding.reviewed(
            Region01BossDragonEvolvedMotionReview.reviewedSourceMotion(),
            Region01BossDragonEvolvedPhaseWindowReview.reviewedSourceWindows(),
            clips,
            windows
        );
    }

    private static void bind(
        Map<ContentId, String> clips,
        Map<ContentId, BossAnimationSourceBinding.ClipWindow> windows,
        ContentId logicalKey,
        String clip,
        double start,
        double end
    ) {
        clips.put(logicalKey, clip);
        windows.put(logicalKey, new BossAnimationSourceBinding.ClipWindow(start, end));
    }

    private static ContentId key(String suffix) {
        return ContentId.rift("animation/boss/" + suffix);
    }
}
