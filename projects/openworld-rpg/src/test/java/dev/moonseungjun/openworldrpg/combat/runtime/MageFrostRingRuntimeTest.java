package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MageFrostRingRuntimeTest {
    @Test
    void authoritativeRingUsesHorizontalRadiusAndVerticalEnvelope() {
        Vec3 origin = new Vec3(0.0, 64.0, 0.0);

        assertTrue(MageFrostRingRuntime.contains(
                origin,
                new AABB(4.0, 64.0, -0.2, 4.4, 65.8, 0.2)
        ));
        assertFalse(MageFrostRingRuntime.contains(
                origin,
                new AABB(4.3, 64.0, -0.2, 4.7, 65.8, 0.2)
        ));
        assertFalse(MageFrostRingRuntime.contains(
                origin,
                new AABB(-0.2, 66.6, -0.2, 0.2, 68.4, 0.2)
        ));
    }
}
