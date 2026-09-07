package kr.moonseungjun.turnboundre.progression;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class M4RewardAndSaveCodecTest {
    private static final String ZOMBIE = "turnbound_re:zombie";

    @Test
    void rewardRollsAreDeterministicAndShardWeightSelectsAtMostOneCharacter() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        RewardService service = new RewardService(registry);
        String table = "turnbound_re:debug_overworld_patrol";

        RewardService.RewardGrant first = service.roll(table, 817263L);
        RewardService.RewardGrant second = service.roll(table, 817263L);
        assertEquals(first, second);
        assertTrue(first.coin() >= 60 && first.coin() <= 90);
        assertTrue(first.essence() >= 20 && first.essence() <= 30);
        assertTrue(first.shards().size() <= 1);

        for (long seed = 0; seed < 200; seed++) {
            RewardService.RewardGrant grant = service.roll(table, seed);
            assertTrue(grant.shards().size() <= 1, "seed=" + seed + " grant=" + grant);
        }
    }

    @Test
    void applyingRewardCreatesNewStateWithoutMutatingOriginal() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        RewardService service = new RewardService(registry);
        var tuning = registry.progressions().get("turnbound_re:default_progression");
        PlayerProgress original = PlayerProgress.fresh(tuning);

        RewardService.Applied applied = service.rollAndApply(original, "turnbound_re:debug_overworld_patrol", 42L);

        assertNotSame(original, applied.state());
        assertEquals(0, original.coin());
        assertEquals(0, original.essence());
        assertEquals(applied.grant().coin(), applied.state().coin());
        assertEquals(applied.grant().essence(), applied.state().essence());
        assertEquals(applied.grant().shards(), applied.state().shards());
    }

    @Test
    void playerProgressCodecRoundTripsEveryCanonicalM4Field() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        var tuning = registry.progressions().get("turnbound_re:default_progression");
        PlayerProgress state = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA,
                1234L,
                567L,
                Map.of(ZOMBIE, 17),
                Map.of(ZOMBIE, new CharacterProgress(ZOMBIE, 2, 3, 30)),
                List.of(ZOMBIE),
                tuning.partyCapacity());

        JsonElement encoded = PlayerProgress.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        PlayerProgress decoded = PlayerProgress.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(state, decoded);
        assertThrows(IllegalArgumentException.class, () -> new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA + 1, 0, 0, Map.of(), Map.of(), List.of(), tuning.partyCapacity()));
    }
}
