package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentPackLoader;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductionRegion01BossCombatContentTest {
    private static final String RESOURCE = "/data/riftfrontier/riftfrontier/content/region_01.json";

    @Test
    void productionRegionPackContainsTheThreeReferenceLockedBossRoles() throws Exception {
        var stream = ProductionRegion01BossCombatContentTest.class.getResourceAsStream(RESOURCE);
        assertNotNull(stream, "production Region 01 pack must be packaged");

        ContentPackLoader.LoadedPack pack;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            pack = new ContentPackLoader().load(reader, RESOURCE);
        }
        pack.requireValid();

        ContentId bossId = ContentId.parse("riftfrontier:boss/region_01_first_apex");
        ContentId committedId = ContentId.parse("riftfrontier:attack/boss/region_01_committed_strike");
        ContentId lineId = ContentId.parse("riftfrontier:attack/boss/region_01_line_displacement");
        ContentId pressureId = ContentId.parse("riftfrontier:attack/boss/region_01_arena_pressure");

        var boss = (CoreDefinition.BossProfile) pack.registry()
            .find(CoreDefinition.Kind.BOSS_PROFILE, bossId).orElseThrow();
        var committed = (CoreDefinition.AttackPattern) pack.registry()
            .find(CoreDefinition.Kind.ATTACK_PATTERN, committedId).orElseThrow();
        var line = (CoreDefinition.AttackPattern) pack.registry()
            .find(CoreDefinition.Kind.ATTACK_PATTERN, lineId).orElseThrow();
        var pressure = (CoreDefinition.AttackPattern) pack.registry()
            .find(CoreDefinition.Kind.ATTACK_PATTERN, pressureId).orElseThrow();

        assertEquals(2, boss.phaseCount());
        assertEquals(Set.of(committedId, lineId, pressureId), boss.attackPatterns());
        assertTrue(boss.arenaRule().contains("position-control"));
        assertTrue(boss.arenaRule().contains("phase changes"));

        assertEquals("arc_melee", committed.delivery());
        assertTrue(committed.counterplay().contains("read_pre_hit_pose"));
        assertTrue(committed.counterplay().contains("punish_recovery"));

        assertEquals("line_charge", line.delivery());
        assertTrue(line.counterplay().contains("read_travel_lane"));
        assertTrue(line.counterplay().contains("move_laterally"));

        assertEquals("area_denial", pressure.delivery());
        assertTrue(pressure.counterplay().contains("read_danger_area"));
        assertTrue(pressure.counterplay().contains("leave_marked_space"));

        for (CoreDefinition.AttackPattern pattern : Set.of(committed, line, pressure)) {
            assertTrue(pattern.telegraphTicks() > 0, "every boss action must retain a readable pre-hit window");
            assertTrue(pattern.activeTicks() > 0, "every boss action must have an authoritative active window");
            assertTrue(pattern.recoveryTicks() > 0, "every first-boss action must expose a counterattack recovery window");
            assertFalse(pattern.presentationCue().isBlank());
        }

        pack.registry().all().forEach(definition ->
            assertFalse(definition.id().path().contains("fixture"), "production Region 01 pack must not promote fixture IDs"));
    }
}
