package kr.moonseungjun.turnboundre.progression;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M4SavedDataCodecTest {
    @Test
    void savedDataRoundTripsPlayerRootsByStableUuid() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        var tuning = registry.progressions().get(PlayerProgressStore.DEFAULT_PROGRESSION_ID);
        UUID firstId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID secondId = UUID.fromString("10000000-0000-0000-0000-000000000002");
        TurnboundProgressSavedData data = new TurnboundProgressSavedData();

        PlayerProgress first = data.getOrCreate(firstId, tuning);
        assertTrue(data.isDirty());
        assertEquals(PlayerProgress.fresh(tuning), first);

        PlayerProgress advanced = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA, 900, 300,
                Map.of("turnbound_re:zombie", 22),
                Map.of("turnbound_re:zombie", new CharacterProgress("turnbound_re:zombie", 2, 3, 30)),
                List.of("turnbound_re:zombie"), tuning.partyCapacity());
        data.put(firstId, advanced);
        data.put(secondId, PlayerProgress.fresh(tuning));

        JsonElement encoded = TurnboundProgressSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        TurnboundProgressSavedData decoded = TurnboundProgressSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(advanced, decoded.get(firstId).orElseThrow());
        assertEquals(PlayerProgress.fresh(tuning), decoded.get(secondId).orElseThrow());
        assertEquals(2, decoded.snapshot().size());
    }

    @Test
    void persistedKeysMustBeRealUuids() {
        assertThrows(IllegalArgumentException.class,
                () -> new TurnboundProgressSavedData(Map.of("not-a-uuid",
                        new PlayerProgress(1, 0, 0, Map.of(), Map.of(), List.of(), 12))));
    }
}
