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
        assertEquals(0.02, GachaService.effectiveFiveStarRate(43), 0.000001);
        assertEquals(0.05, GachaService.effectiveFiveStarRate(44), 0.000001);
        assertEquals(0.08, GachaService.effectiveFiveStarRate(45), 0.000001);
        assertEquals(1.0, GachaService.effectiveFiveStarRate(59), 0.000001);
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
        assertEquals(15, duplicate.starEssenceGranted());
        assertEquals(15, profile.currency(PlayerProfile.Currency.STAR_ESSENCE));
    }

    @Test
    void productionSummonPoolContainsOnlyCanonicalThreeToFiveStarHeroes() {
        Set<String> seen = new HashSet<>();
        for (int stars : new int[]{3,4,5}) {
            for (String id : GachaCatalog.standardPool(stars)) {
                assertTrue(id.startsWith("P"), id);
                assertEquals(stars, GachaCatalog.nativeStars(id));
                assertTrue(GachaCatalog.isSummonable(id));
                seen.add(id);
            }
        }
        assertEquals(Set.of("P01","P02","P03","P04","P05","P06","P07","P08"), seen);
        for (String legacy : Set.of("F01","F02","F03","F04")) {
            assertFalse(GachaCatalog.isSummonable(legacy));
            assertTrue(GachaCatalog.isKnownCharacter(legacy));
        }
    }

    @Test
    void legacyHighPitySaveMigratesToNextPullHardPity() {
        PlayerProfile restored = PlayerProfile.restore(new PlayerProfile.Snapshot(
                5_000, 300, 0, 0, Set.of("P01","F03"), 79, false, false));
        assertEquals(59, restored.fiveStarPity());
        var pull = new GachaService(new Random(4)).summonStandardSingle(restored).pulls().getFirst();
        assertEquals(5, pull.nativeStars());
        assertEquals(0, restored.fiveStarPity());
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
