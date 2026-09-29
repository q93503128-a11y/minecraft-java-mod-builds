package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GachaServiceTest {
    @Test
    void canonicalSoftAndHardPityRatesAreStable() {
        assertEquals(0.03, GachaService.effectiveFiveStarRate(63), 0.000001);
        assertEquals(0.06, GachaService.effectiveFiveStarRate(64), 0.000001);
        assertEquals(0.09, GachaService.effectiveFiveStarRate(65), 0.000001);
        assertEquals(1.0, GachaService.effectiveFiveStarRate(79), 0.000001);
    }

    @Test
    void hardPityForcesFiveStarAndResetsCounter() {
        PlayerProfile profile = PlayerProfile.restore(new PlayerProfile.Snapshot(
                5_000, 300, 0, 0, Set.of(), 79, false, false));
        GachaService.PullResult result = new GachaService(new Random(7)).summonStandardSingle(profile).pulls().getFirst();
        assertEquals(5, result.nativeStars());
        assertEquals(0, profile.fiveStarPity());
        assertEquals(0, profile.currency(PlayerProfile.Currency.SUMMON_CRYSTAL));
    }

    @Test
    void tenPullAlwaysContainsFourStarOrHigher() {
        PlayerProfile profile = PlayerProfile.newGame();
        profile.grant(PlayerProfile.Currency.SUMMON_CRYSTAL, 3_000);
        GachaService.BatchResult result = new GachaService(new Random(99)).summonStandardTen(profile);
        assertEquals(10, result.pulls().size());
        assertTrue(result.pulls().stream().anyMatch(p -> p.nativeStars() >= 4));
        assertEquals(0, profile.currency(PlayerProfile.Currency.SUMMON_CRYSTAL));
    }

    @Test
    void starterArchiveUsesOnceAndSlotTenIsUnownedFourPlus() {
        Set<String> owned = new HashSet<>(Set.of("P01", "P03", "P04", "F03", "P08"));
        PlayerProfile profile = PlayerProfile.restore(new PlayerProfile.Snapshot(
                17_000, 3_000, 60, 0, owned, 0, true, false));
        GachaService.BatchResult result = new GachaService(new Random(1234)).summonStarterTen(profile);

        assertEquals(10, result.pulls().size());
        GachaService.PullResult guaranteed = result.pulls().getLast();
        assertTrue(GachaCatalog.starterGuaranteePool().contains(guaranteed.characterId()));
        assertTrue(guaranteed.nativeStars() >= 4);
        assertTrue(guaranteed.newlyOwned());
        assertEquals(0, profile.currency(PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertFalse(profile.starterArchiveAvailable());
        assertThrows(IllegalStateException.class, () -> new GachaService(new Random(1)).summonStarterTen(profile));
    }

    @Test
    void starterArchiveDoesNotSpendIfNoUnownedGuaranteeCandidateExists() {
        Set<String> owned = new HashSet<>(Set.of("P01", "P03", "P04", "F03", "P08", "P02", "P05", "P06", "P07"));
        PlayerProfile profile = PlayerProfile.restore(new PlayerProfile.Snapshot(
                17_000, 3_000, 60, 0, owned, 0, true, false));

        assertThrows(IllegalStateException.class, () -> new GachaService(new Random(5)).summonStarterTen(profile));
        assertEquals(3_000, profile.currency(PlayerProfile.Currency.SUMMON_CRYSTAL));
        assertTrue(profile.starterArchiveAvailable());
    }

    @Test
    void duplicateCharacterImmediatelyBecomesStarEssence() {
        PlayerProfile profile = PlayerProfile.newGame();
        assertTrue(profile.acquireCharacter("P08").newlyOwned());
        PlayerProfile.Acquisition duplicate = profile.acquireCharacter("P08");
        assertFalse(duplicate.newlyOwned());
        assertEquals(40, duplicate.starEssenceGranted());
        assertEquals(40, profile.currency(PlayerProfile.Currency.STAR_ESSENCE));
    }

    @Test
    void productionSummonPoolContainsCanonicalOneToFiveStarRoster() {
        Set<String> seen = new HashSet<>();
        for (int stars : new int[]{1,2,3,4,5}) {
            for (String id : GachaCatalog.standardPool(stars)) {
                assertEquals(stars, GachaCatalog.nativeStars(id));
                assertTrue(GachaCatalog.isSummonable(id));
                seen.add(id);
            }
        }
        assertEquals(Set.of(
                "P01","P02","P03","P04","P05","P06","P07","P08",
                "F01","F02","F03","F04"), seen);
    }

    @Test
    void pity79ForcesTheNextPullToFiveStar() {
        PlayerProfile restored = PlayerProfile.restore(new PlayerProfile.Snapshot(
                5_000, 300, 0, 0, Set.of("P01","F03"), 79, false, false));
        assertEquals(79, restored.fiveStarPity());
        var pull = new GachaService(new Random(4)).summonStandardSingle(restored).pulls().getFirst();
        assertEquals(5, pull.nativeStars());
        assertEquals(0, restored.fiveStarPity());
    }

    @Test
    void canonicalRarityTableAndDuplicateEssenceMatchV04() {
        assertEquals(0.03, GachaCatalog.baseRarityRate(5), 0.000001);
        assertEquals(0.12, GachaCatalog.baseRarityRate(4), 0.000001);
        assertEquals(0.35, GachaCatalog.baseRarityRate(3), 0.000001);
        assertEquals(0.30, GachaCatalog.baseRarityRate(2), 0.000001);
        assertEquals(0.20, GachaCatalog.baseRarityRate(1), 0.000001);
        assertEquals(5, GachaCatalog.duplicateEssence(1));
        assertEquals(15, GachaCatalog.duplicateEssence(2));
        assertEquals(40, GachaCatalog.duplicateEssence(3));
        assertEquals(100, GachaCatalog.duplicateEssence(4));
        assertEquals(250, GachaCatalog.duplicateEssence(5));
        assertEquals(Set.of("P02","P05","P06"), Set.copyOf(GachaCatalog.standardPool(5)));
        assertEquals(Set.of("P01","P03","P04","P07"), Set.copyOf(GachaCatalog.standardPool(4)));
        assertEquals(Set.of("F03","F04"), Set.copyOf(GachaCatalog.standardPool(2)));
        assertEquals(Set.of("F01","F02"), Set.copyOf(GachaCatalog.standardPool(1)));
    }

    @Test
    void insufficientCrystalDoesNotSpendAnything() {
        PlayerProfile profile = PlayerProfile.newGame();
        GachaService service = new GachaService(new Random(1));
        assertThrows(IllegalStateException.class, () -> service.summonStandardSingle(profile));
        assertEquals(0, profile.currency(PlayerProfile.Currency.SUMMON_CRYSTAL));
    }

    @Test
    void profileSnapshotRoundTripsEconomyCollectionAndPity() {
        PlayerProfile profile = PlayerProfile.restore(new PlayerProfile.Snapshot(
                12_345, 900, 150, 1, Set.of("P01", "F03"), 64, true, false));
        assertEquals(profile.snapshot(), PlayerProfile.restore(profile.snapshot()).snapshot());
    }
}
