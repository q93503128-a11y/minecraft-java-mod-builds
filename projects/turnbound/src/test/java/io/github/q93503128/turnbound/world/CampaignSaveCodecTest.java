package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CampaignSaveCodecTest {
    @Test
    void campaignSaveRoundTripsEveryAuthoritativeField() {
        CampaignProgressStore.Snapshot snapshot = new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(17_000, 2_100, 410, 2,
                        Set.of("P01", "P03", "P04", "F03", "P08", "P05"), 37, true, true),
                Map.of(
                        "P01", new CharacterProgression.State(8, 33),
                        "P05", new CharacterProgression.State(1, 0),
                        "P08", new CharacterProgression.State(3, 77)),
                Set.of("southgate_enc_m01", "southgate_b01_graul"));

        assertEquals(snapshot, CampaignSaveCodec.decode(CampaignSaveCodec.encode(snapshot)));
    }

    @Test
    void unknownSchemaIsRejectedInsteadOfSilentlyResettingProgress() {
        String json = CampaignSaveCodec.encode(new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(5_000, 0, 0, 0, Set.of("P01"), 0, false, false),
                Map.of("P01", new CharacterProgression.State(1, 0)), Set.of()));
        assertThrows(IllegalStateException.class,
                () -> CampaignSaveCodec.decode(json.replace("\"schemaVersion\": 1", "\"schemaVersion\": 999")));
    }
}
