package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossProductionPresentation;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductionRegion01BossPresentationTest {
    @Test
    void packagedProfileCoversEveryProductionBossAttackPhaseWithoutClaimingSourceClipBindings() {
        BossPresentationProfile profile = Region01BossProductionPresentation.load();

        assertEquals(ContentId.parse("riftfrontier:boss/region_01_first_apex"), profile.bossProfile());
        assertEquals("default", profile.variant());
        assertEquals(ContentId.parse("riftfrontier:model/boss/region_01_dragon_evolved"), profile.modelKey());
        assertEquals(9, profile.bindings().size());

        Set<String> selectors = profile.bindings().keySet().stream()
            .map(BossPresentationProfile.BindingKey::selector)
            .collect(Collectors.toSet());
        assertEquals(Set.of(
            "region_01_boss_committed_strike|arc_melee|TELEGRAPH",
            "region_01_boss_committed_strike|arc_melee|ACTIVE",
            "region_01_boss_committed_strike|arc_melee|RECOVERY",
            "region_01_boss_line_displacement|line_charge|TELEGRAPH",
            "region_01_boss_line_displacement|line_charge|ACTIVE",
            "region_01_boss_line_displacement|line_charge|RECOVERY",
            "region_01_boss_arena_pressure|area_denial|TELEGRAPH",
            "region_01_boss_arena_pressure|area_denial|ACTIVE",
            "region_01_boss_arena_pressure|area_denial|RECOVERY"
        ), selectors);

        profile.bindings().values().forEach(binding -> {
            assertTrue(binding.animationKey().path().startsWith("animation/boss/region_01_"));
            assertTrue(binding.vfxKey().path().startsWith("vfx/boss/region_01_"));
            assertTrue(binding.soundKey().path().startsWith("sound/boss/region_01_"));
        });
    }
}
