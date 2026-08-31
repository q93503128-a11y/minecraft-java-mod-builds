package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.progression.QuestProgress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PendingEquipmentSaveTest {
    @Test
    void pendingEquipmentRewardSurvivesSchemaFourSaveRoundTrip() {
        var pending = new EquipmentInventory.Item("eq_00000002", "W05", 0);
        var equipment = new EquipmentInventory.Snapshot(3,
                Map.of("eq_00000001", new EquipmentInventory.Item("eq_00000001", "W01", 0)),
                Map.of(), Map.of(), List.of(pending));
        var snapshot = new CampaignProgressStore.Snapshot(
                new PlayerProfile.Snapshot(5_000,0,0,0,Set.of("P01"),0,false,false),
                Map.of("P01", new CharacterProgression.State(1,0)),
                Map.of("P01", CharacterGrowthRules.initial("P01")), equipment, QuestProgress.Snapshot.empty(),
                Set.of(), Set.of(), Set.of());

        var decoded = CampaignSaveCodec.decode(CampaignSaveCodec.encode(snapshot));
        assertEquals(List.of(pending), decoded.equipment().pendingRewards());
        assertEquals(1, decoded.equipment().items().size());
    }
}
