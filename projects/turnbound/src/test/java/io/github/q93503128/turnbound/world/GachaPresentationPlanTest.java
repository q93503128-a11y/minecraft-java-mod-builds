package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.GachaService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GachaPresentationPlanTest {
    @Test
    void newCharactersAreRevealedInPullOrder() {
        var result = new GachaService.BatchResult(List.of(
                new GachaService.PullResult("P01",4,false,60,2),
                new GachaService.PullResult("P08",3,true,0,3),
                new GachaService.PullResult("P06",5,true,0,0)), 3000);
        assertEquals(List.of("P08","P06"), GachaPresentationPlan.revealCharacterIds(result));
    }

    @Test
    void duplicateOnlyBatchStillGetsOneHighestRarityThreeDimensionalFocus() {
        var result = new GachaService.BatchResult(List.of(
                new GachaService.PullResult("P08",3,false,15,8),
                new GachaService.PullResult("P01",4,false,60,9),
                new GachaService.PullResult("P06",5,false,250,0),
                new GachaService.PullResult("P02",5,false,250,0)), 3000);
        assertEquals(List.of("P06"), GachaPresentationPlan.revealCharacterIds(result));
    }
}
