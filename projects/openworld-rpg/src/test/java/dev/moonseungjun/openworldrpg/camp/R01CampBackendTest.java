package dev.moonseungjun.openworldrpg.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class R01CampBackendTest {
    @Test
    void recipeKnowledgeRequiresBothPermanentMaterialEncounters() {
        var state = R01CampPlayerState.initial();
        assertFalse(state.recipeKnown());
        state = state.recordMaterialEncounter(R01GatheringRules.HARDWOOD);
        assertFalse(state.recipeKnown());
        state = state.recordMaterialEncounter(R01CampRules.TOUGH_HIDE);
        assertTrue(state.recipeKnown());

        var encoded = R01CampPlayerState.CODEC
                .encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow();
        assertTrue(
                R01CampPlayerState.CODEC
                        .parse(JsonOps.INSTANCE, encoded)
                        .getOrThrow()
                        .recipeKnown()
        );
    }

    @Test
    void kitCraftIsPermanentAndOnePendingTransactionOnly() {
        String playerId = UUID.randomUUID().toString();
        var ready = R01CampPlayerState.initial()
                .recordMaterialEncounter(R01GatheringRules.HARDWOOD)
                .recordMaterialEncounter(R01CampRules.TOUGH_HIDE);

        var first = ready.beginKitCraft(playerId);
        var duplicate = first.state().beginKitCraft(playerId);
        assertTrue(first.created());
        assertFalse(duplicate.created());
        assertEquals(first.pending(), duplicate.pending());

        var unlocked = first.state()
                .completeKitCraft(first.pending().transactionId());
        assertTrue(unlocked.kitUnlocked());
        assertTrue(unlocked.pendingKitCraft().isEmpty());
    }

    @Test
    void exactPlacementBoundariesAreAccepted() {
        String owner = UUID.randomUUID().toString();
        var candidate = new R01CampPlacementAuthority.Candidate(
                new Vec3(100.0, 64.0, 100.0),
                90.0
        );
        var probe = new R01CampPlacementAuthority.PlacementProbe(
                candidate,
                samples(64.0, 64.75, 20),
                false,
                true,
                false,
                24.0,
                24.0,
                1.5,
                true
        );
        var decision = R01CampPlacementAuthority.evaluate(
                owner,
                candidate,
                probe,
                R01CampWorldState.initial()
        );
        assertTrue(decision.allowed());
        assertEquals(20L, decision.stableSupportSamples());
        assertEquals(0.75, decision.supportHeightVariance(), 0.0001);
    }

    @Test
    void placementRejectsTooFewSamplesUnevenGroundAndCloseCamp() {
        String ownerA = UUID.randomUUID().toString();
        String ownerB = UUID.randomUUID().toString();
        var candidate = new R01CampPlacementAuthority.Candidate(
                new Vec3(0.0, 64.0, 0.0), 0.0
        );

        assertEquals(
                R01CampPlacementAuthority.Status.INSUFFICIENT_SUPPORT,
                R01CampPlacementAuthority.evaluate(
                        ownerA,
                        candidate,
                        probe(candidate, samples(64.0, 64.2, 19)),
                        R01CampWorldState.initial()
                ).status()
        );
        assertEquals(
                R01CampPlacementAuthority.Status.TOO_UNEVEN,
                R01CampPlacementAuthority.evaluate(
                        ownerA,
                        candidate,
                        probe(candidate, samples(64.0, 64.751, 20)),
                        R01CampWorldState.initial()
                ).status()
        );

        var first = R01CampWorldState.initial().deploy(
                ownerA,
                candidate
        );
        var close = new R01CampPlacementAuthority.Candidate(
                new Vec3(11.999, 64.0, 0.0), 0.0
        );
        assertEquals(
                R01CampPlacementAuthority.Status.TOO_CLOSE_TO_OTHER_CAMP,
                R01CampPlacementAuthority.evaluate(
                        ownerB,
                        close,
                        probe(close, samples(64.0, 64.2, 25)),
                        first.state()
                ).status()
        );
    }

    @Test
    void redeployReplacesOnlyOwnersCampAndWorldStatePersists() {
        String owner = UUID.randomUUID().toString();
        var first = R01CampWorldState.initial().deploy(
                owner,
                new R01CampPlacementAuthority.Candidate(
                        new Vec3(0.0, 64.0, 0.0), 0.0
                )
        );
        var second = first.state().deploy(
                owner,
                new R01CampPlacementAuthority.Candidate(
                        new Vec3(30.0, 64.0, 0.0), 180.0
                )
        );
        assertTrue(second.previous().isPresent());
        assertEquals(1, second.state().activeCamps().size());
        assertEquals(30.0, second.current().x(), 0.0001);

        var encoded = R01CampWorldState.CODEC
                .encodeStart(JsonOps.INSTANCE, second.state())
                .getOrThrow();
        var decoded = R01CampWorldState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(second.state(), decoded);
    }

    private static R01CampPlacementAuthority.PlacementProbe probe(
            R01CampPlacementAuthority.Candidate candidate,
            List<R01CampPlacementAuthority.SupportSample> support
    ) {
        return new R01CampPlacementAuthority.PlacementProbe(
                candidate,
                support,
                false,
                true,
                false,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                1.5,
                true
        );
    }

    private static List<R01CampPlacementAuthority.SupportSample> samples(
            double low,
            double high,
            int stableCount
    ) {
        ArrayList<R01CampPlacementAuthority.SupportSample> result =
                new ArrayList<>();
        for (int i = 0; i < R01CampRules.SUPPORT_SAMPLE_COUNT; i++) {
            if (i >= stableCount) {
                result.add(R01CampPlacementAuthority.SupportSample.unsupported());
            } else {
                result.add(new R01CampPlacementAuthority.SupportSample(
                        true,
                        i == stableCount - 1 ? high : low
                ));
            }
        }
        return List.copyOf(result);
    }
}
