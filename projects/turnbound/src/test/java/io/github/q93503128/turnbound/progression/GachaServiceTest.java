package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

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
    void duplicateCharacterImmediatelyBecomesStarEssence() {
        PlayerProfile profile = PlayerProfile.newGame();
        assertTrue(profile.acquireCharacter("P08").newlyOwned());
        PlayerProfile.Acquisition duplicate = profile.acquireCharacter("P08");
        assertFalse(duplicate.newlyOwned());
        assertEquals(40, duplicate.starEssenceGranted());
        assertEquals(40, profile.currency(PlayerProfile.Currency.STAR_ESSENCE));
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
