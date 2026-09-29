package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerActionRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerReactionRuntime;
import org.junit.jupiter.api.Test;

class ProjectPlayerReactionRuntimeTest {
    @Test
    void combatTemperMultiplierUsesServerTickGranularity() {
        assertEquals(
                9,
                ProjectPlayerReactionRuntime.resolveDurationTicks(
                        10,
                        0.90
                )
        );
        assertEquals(
                10,
                ProjectPlayerReactionRuntime.resolveDurationTicks(
                        11,
                        0.90
                )
        );
        assertEquals(
                17,
                ProjectPlayerReactionRuntime.resolveDurationTicks(
                        17,
                        1.0
                )
        );
    }

    @Test
    void canonicalSkillCancelFractionsRoundToFirstLegalTick() {
        assertEquals(
                10,
                ProjectPlayerActionRuntime.ActionSpec.cancelOffset(
                        14,
                        0.70
                )
        );
        assertEquals(
                10,
                ProjectPlayerActionRuntime.ActionSpec.cancelOffset(
                        13,
                        0.72
                )
        );
        assertEquals(
                24,
                ProjectPlayerActionRuntime.ActionSpec.cancelOffset(
                        29,
                        0.82
                )
        );
    }
}
