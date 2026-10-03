package dev.moonseungjun.openworldrpg.travel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class R01FastTravelRulesTest {
    @Test
    void productionNodeAllowsPrimaryPlusFourFallbackArrivals() {
        new R01FastTravelNodeRegistry.ProductionNode(
                "openworld_rpg:travel/test",
                Level.OVERWORLD,
                new Vec3(0.0, 64.0, 0.0),
                List.of(
                        new Vec3(10.0, 64.0, 10.0),
                        new Vec3(11.0, 64.0, 10.0),
                        new Vec3(9.0, 64.0, 10.0),
                        new Vec3(10.0, 64.0, 11.0),
                        new Vec3(10.0, 64.0, 9.0)
                ),
                R01FastTravelNodeRegistry.PersonalUnlock
                        .ALDERFORD_GATE_SHRINE
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new R01FastTravelNodeRegistry.ProductionNode(
                        "openworld_rpg:travel/too_many",
                        Level.OVERWORLD,
                        new Vec3(0.0, 64.0, 0.0),
                        List.of(
                                Vec3.ZERO,
                                Vec3.ZERO,
                                Vec3.ZERO,
                                Vec3.ZERO,
                                Vec3.ZERO,
                                Vec3.ZERO
                        ),
                        R01FastTravelNodeRegistry.PersonalUnlock
                                .ALDERFORD_GATE_SHRINE
                )
        );
    }

    @Test
    void personalActivationFollowsExistingR01CheckpointState() {
        R01PlayerState initial = R01PlayerState.initial();
        assertFalse(
                R01FastTravelService.personallyActivated(
                        initial,
                        R01FastTravelNodeRegistry.PersonalUnlock
                                .ALDERFORD_GATE_SHRINE
                )
        );
        assertFalse(
                R01FastTravelService.personallyActivated(
                        initial,
                        R01FastTravelNodeRegistry.PersonalUnlock
                                .QUARRY_WAYSTONE
                )
        );

        R01PlayerState alderford =
                initial.markFirstShrineActivated(1L);
        assertTrue(
                R01FastTravelService.personallyActivated(
                        alderford,
                        R01FastTravelNodeRegistry.PersonalUnlock
                                .ALDERFORD_GATE_SHRINE
                )
        );

        R01PlayerState quarry = alderford
                .markQuarryWaystoneDiscovered(2L)
                .markQuarryWaystoneActivated(3L);
        assertTrue(
                R01FastTravelService.personallyActivated(
                        quarry,
                        R01FastTravelNodeRegistry.PersonalUnlock
                                .QUARRY_WAYSTONE
                )
        );
    }

    @Test
    void originRadiusIsExactSixBlockSphere() {
        Vec3 anchor = new Vec3(0.0, 64.0, 0.0);
        assertTrue(
                R01FastTravelService.withinOriginRadius(
                        new Vec3(6.0, 64.0, 0.0),
                        anchor
                )
        );
        assertFalse(
                R01FastTravelService.withinOriginRadius(
                        new Vec3(6.001, 64.0, 0.0),
                        anchor
                )
        );
    }
}
