package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatSessionState;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerCombatSessionStateTest {
    @Test
    void combatSessionSnapshotSurvivesCodecRoundTrip() {
        var original = new PlayerCombatSessionState(
                PlayerCombatSessionState.CURRENT_SCHEMA_VERSION,
                true,
                42.5,
                61.0,
                100L,
                80L,
                90L,
                85L,
                112L,
                Map.of("openworld_rpg:arc_bolt", 140L),
                Optional.of(new PlayerCombatSessionState.PoiseSnapshot(
                        80.0, 40.0, 100L, 90L, 105L
                )),
                Optional.of(new PlayerCombatSessionState.ShockSnapshot(
                        100.0, 35.0, 100L, 95L, 160L
                )),
                Optional.of(
                        new PlayerCombatSessionState.NegativeStatusesSnapshot(
                                100L,
                                Map.of(
                                        "openworld_rpg:poison",
                                        new PlayerCombatSessionState
                                                .ActiveNegativeStatusSnapshot(
                                                        Set.of("minor_dispellable"),
                                                        180L
                                                )
                                ),
                                200L
                        )
                ),
                Optional.of(
                        new PlayerCombatSessionState.UltimateSnapshot(
                                74.0,
                                760L,
                                3.0,
                                100L
                        )
                )
        );

        var encoded = PlayerCombatSessionState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerCombatSessionState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
        assertTrue(decoded.captured());
    }

    @Test
    void legacySchemaOneResourceSnapshotStillDecodesWithoutNewFields() {
        var legacy = new PlayerCombatSessionState(
                1,
                true,
                20.0,
                30.0,
                50L,
                40L,
                45L,
                42L,
                60L,
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        var encoded = PlayerCombatSessionState.CODEC
                .encodeStart(JsonOps.INSTANCE, legacy)
                .getOrThrow();
        var decoded = PlayerCombatSessionState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(1, decoded.schemaVersion());
        assertTrue(decoded.poise().isEmpty());
        assertTrue(decoded.shock().isEmpty());
        assertTrue(decoded.negativeStatuses().isEmpty());
        assertTrue(decoded.ultimate().isEmpty());
    }
}
