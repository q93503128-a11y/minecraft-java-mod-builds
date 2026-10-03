package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.GachaService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GachaPresentationPlanTest {
    @Test
    void singlePullAlwaysGetsARevealEvenAtLowRarity() {
        var result = new GachaService.BatchResult(List.of(
                new GachaService.PullResult("F01",1,true,0,3)), 300);

        assertEquals(List.of(new GachaPresentationPlan.Reveal("F01",1,true)),
                GachaPresentationPlan.reveals(result));
        assertEquals(List.of(0), GachaPresentationPlan.spotlightIndices(result));
    }

    @Test
    void multiPullReservesFullCeremonyForFourAndFiveStarResults() {
        var result = new GachaService.BatchResult(List.of(
                new GachaService.PullResult("P01",4,false,100,2),
                new GachaService.PullResult("P08",3,true,0,3),
                new GachaService.PullResult("P06",5,true,0,0)), 3000);

        assertEquals(List.of("P01","P06"), GachaPresentationPlan.revealCharacterIds(result));
        assertEquals(List.of(0,2), GachaPresentationPlan.spotlightIndices(result));
    }

    @Test
    void highRarityDuplicateIsNotBuriedByNewLowRarityPull() {
        var result = new GachaService.BatchResult(List.of(
                new GachaService.PullResult("F01",1,true,0,3),
                new GachaService.PullResult("P06",5,false,250,0),
                new GachaService.PullResult("P08",3,false,40,1)), 3000);

        assertEquals(List.of(
                new GachaPresentationPlan.Reveal("P06",5,false)),
                GachaPresentationPlan.reveals(result));
        assertEquals(List.of(1), GachaPresentationPlan.spotlightIndices(result));
    }

    @Test
    void duplicateOnlyBatchSpotlightsEveryFourAndFiveStarPull() {
        var result = new GachaService.BatchResult(List.of(
                new GachaService.PullResult("P08",3,false,40,8),
                new GachaService.PullResult("P01",4,false,100,9),
                new GachaService.PullResult("P06",5,false,250,0),
                new GachaService.PullResult("P02",5,false,250,0)), 3000);
        assertEquals(List.of("P01","P06","P02"), GachaPresentationPlan.revealCharacterIds(result));
    }

    @Test
    void lowRarityOnlyMultiPullStillGetsOneBestPreviewBeforeSummary() {
        var result = new GachaService.BatchResult(List.of(
                new GachaService.PullResult("F01",1,true,0,1),
                new GachaService.PullResult("P08",3,false,40,2),
                new GachaService.PullResult("P03",2,false,20,3)), 3000);

        assertEquals(List.of(new GachaPresentationPlan.Reveal("P08",3,false)),
                GachaPresentationPlan.reveals(result));
        assertEquals(List.of(1), GachaPresentationPlan.spotlightIndices(result));
    }
}
