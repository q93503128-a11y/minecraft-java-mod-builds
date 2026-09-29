package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatSessionState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlayerCombatSessionStateTest {
    @Test
    void combatSessionSnapshotSurvivesCodecRoundTrip() {
        var original = new PlayerCombatSessionState(
                PlayerCombatSessionState.CURRENT_SCHEMA_VERSION,
                42.5,
                61.0,
                100L,
                80L,
                90L,
                85L,
                112L,
                Map.of("openworld_rpg:arc_bolt", 140L)
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
}
