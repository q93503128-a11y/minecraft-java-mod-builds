package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossDragonEvolvedStagedAttackBindingTest {
    @Test
    void reviewedStrikeAndLineRolesUseExactReviewedSourceWindows() {
        BossAnimationSourceBinding binding =
            Region01BossDragonEvolvedStagedAttackBinding.reviewedResolvedAttacks();

        assertTrue(binding.hasReviewedPhaseWindows());
        assertEquals(6, binding.sourceClipByLogicalKey().size());
        assertRole(binding, "region_01_committed_strike", "Punch", new double[][] {
            {0.0D, 0.2D}, {0.2D, 0.275D}, {0.275D, 1.0D}
        });
        assertRole(binding, "region_01_line_displacement", "Headbutt", new double[][] {
            {0.0D, 0.15555555555555556D},
            {0.15555555555555556D, 0.2D},
            {0.2D, 0.4888888888888889D}
        });
    }

    @Test
    void arenaPressureRemainsExplicitlyUnresolvedRatherThanReceivingARecycledClip() {
        BossAnimationSourceBinding binding =
            Region01BossDragonEvolvedStagedAttackBinding.reviewedResolvedAttacks();
        Set<ContentId> unresolved = Region01BossDragonEvolvedStagedAttackBinding.unresolvedLogicalAnimationKeys();

        assertEquals(Set.of(
            key("region_01_arena_pressure/telegraph"),
            key("region_01_arena_pressure/active"),
            key("region_01_arena_pressure/recovery")
        ), unresolved);
        unresolved.forEach(key -> {
            assertFalse(binding.sourceClipByLogicalKey().containsKey(key));
            assertFalse(binding.sourceWindowByLogicalKey().containsKey(key));
        });
    }

    private static void assertRole(
        BossAnimationSourceBinding binding,
        String role,
        String expectedClip,
        double[][] expectedWindows
    ) {
        String[] phases = {"telegraph", "active", "recovery"};
        Map<ContentId, String> clips = binding.sourceClipByLogicalKey();
        Map<ContentId, BossAnimationSourceBinding.ClipWindow> windows = binding.sourceWindowByLogicalKey();
        for (int i = 0; i < phases.length; i++) {
            ContentId key = key(role + "/" + phases[i]);
            assertEquals(expectedClip, clips.get(key));
            assertEquals(
                new BossAnimationSourceBinding.ClipWindow(expectedWindows[i][0], expectedWindows[i][1]),
                windows.get(key)
            );
            Region01BossDragonEvolvedPhaseWindowReview.requireReviewed(expectedClip, windows.get(key));
        }
    }

    private static ContentId key(String suffix) {
        return ContentId.rift("animation/boss/" + suffix);
    }
}
