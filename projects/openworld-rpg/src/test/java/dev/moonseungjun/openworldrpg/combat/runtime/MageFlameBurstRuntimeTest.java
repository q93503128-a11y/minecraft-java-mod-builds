package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MageFlameBurstRuntimeTest {
    @Test
    void fullDurationPulseMathPreservesLockedDirectAndPoiseTotals() {
        assertEquals(
                4,
                MageFlameBurstRuntime.FULL_DURATION_PULSE_COUNT
        );
        assertEquals(
                2.30,
                MageFlameBurstRuntime
                        .effectiveDirectTotalCoefficient(false, 1.0),
                0.0001
        );
        assertEquals(
                2.60,
                MageFlameBurstRuntime
                        .effectiveDirectTotalCoefficient(true, 1.0),
                0.0001
        );
        assertEquals(
                2.63,
                MageFlameBurstRuntime
                        .effectiveDirectTotalCoefficient(true, 1.10),
                0.0001
        );
        assertEquals(
                0.40,
                MageFlameBurstRuntime
                        .effectiveBurningTotalCoefficient(false, 1.0),
                0.0001
        );
        assertEquals(
                0.51,
                MageFlameBurstRuntime
                        .effectiveBurningTotalCoefficient(true, 1.10),
                0.0001
        );
    }

    @Test
    void projectAuthorityRechecksGroundCenterAndRealTargetBounds() {
        Vec3 release = new Vec3(0.0, 64.0, 0.0);
        assertTrue(MageFlameBurstRuntime.validCenter(
                release,
                new Vec3(10.5, 64.0, 0.0)
        ));
        assertFalse(MageFlameBurstRuntime.validCenter(
                release,
                new Vec3(11.1, 64.0, 0.0)
        ));

        Vec3 center = new Vec3(4.0, 64.0, 0.0);
        assertTrue(MageFlameBurstRuntime.contains(
                center,
                new AABB(7.3, 64.0, -0.2, 7.7, 65.8, 0.2)
        ));
        assertFalse(MageFlameBurstRuntime.contains(
                center,
                new AABB(7.6, 64.0, -0.2, 8.0, 65.8, 0.2)
        ));
        assertFalse(MageFlameBurstRuntime.contains(
                center,
                new AABB(3.8, 67.1, -0.2, 4.2, 68.9, 0.2)
        ));
    }
}
