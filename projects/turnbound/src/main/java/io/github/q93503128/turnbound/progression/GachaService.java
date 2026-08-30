package io.github.q93503128.turnbound.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Server-side v0.4 Standard Archive resolver. */
public final class GachaService {
    public record PullResult(String characterId, int nativeStars, boolean newlyOwned, int starEssenceGranted, int pityAfter) {}
    public record BatchResult(List<PullResult> pulls, int crystalSpent) {
        public BatchResult { pulls = List.copyOf(pulls); }
    }

    private final RandomGenerator random;

    public GachaService(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public BatchResult summonStandardSingle(PlayerProfile profile) {
        spend(profile, GachaCatalog.SINGLE_COST);
        return new BatchResult(List.of(pullStandard(profile, false)), GachaCatalog.SINGLE_COST);
    }

    public BatchResult summonStandardTen(PlayerProfile profile) {
        spend(profile, GachaCatalog.TEN_COST);
        List<PullResult> pulls = new ArrayList<>(10);
        boolean hasFourPlus = false;
        for (int slot = 0; slot < 10; slot++) {
            boolean guaranteeFourPlus = slot == 9 && !hasFourPlus;
            PullResult result = pullStandard(profile, guaranteeFourPlus);
            pulls.add(result);
            hasFourPlus |= result.nativeStars() >= 4;
        }
        return new BatchResult(pulls, GachaCatalog.TEN_COST);
    }

    public static double effectiveFiveStarRate(int pityBeforePull) {
        if (pityBeforePull < 0 || pityBeforePull >= GachaCatalog.HARD_PITY) {
            throw new IllegalArgumentException("Invalid pity " + pityBeforePull);
        }
        int pullOrdinal = pityBeforePull + 1;
        if (pullOrdinal >= GachaCatalog.HARD_PITY) return 1.0;
        if (pullOrdinal < GachaCatalog.SOFT_PITY_START) return GachaCatalog.BASE_FIVE_STAR_RATE;
        double boosted = GachaCatalog.BASE_FIVE_STAR_RATE
                + (pullOrdinal - GachaCatalog.SOFT_PITY_START + 1) * GachaCatalog.SOFT_PITY_STEP;
        return Math.min(1.0, boosted);
    }

    private PullResult pullStandard(PlayerProfile profile, boolean guaranteeFourPlus) {
        int stars = guaranteeFourPlus ? rollFourPlus(profile.fiveStarPity()) : rollRarity(profile.fiveStarPity());
        List<String> pool = GachaCatalog.standardPool(stars);
        String characterId = pool.get(random.nextInt(pool.size()));
        PlayerProfile.Acquisition acquisition = profile.acquireCharacter(characterId);
        profile.recordSummonRarity(stars);
        return new PullResult(characterId, stars, acquisition.newlyOwned(), acquisition.starEssenceGranted(), profile.fiveStarPity());
    }

    private int rollRarity(int pityBeforePull) {
        double fiveRate = effectiveFiveStarRate(pityBeforePull);
        if (fiveRate >= 1.0) return 5;
        double roll = random.nextDouble();
        if (roll < fiveRate) return 5;

        // As soft pity grows, preserve the canonical ★1~★4 relative weights inside the remaining probability mass.
        double nonFive = (roll - fiveRate) / (1.0 - fiveRate);
        double total = 0.97;
        if (nonFive < 0.12 / total) return 4;
        if (nonFive < (0.12 + 0.35) / total) return 3;
        if (nonFive < (0.12 + 0.35 + 0.30) / total) return 2;
        return 1;
    }

    private int rollFourPlus(int pityBeforePull) {
        double fiveRate = effectiveFiveStarRate(pityBeforePull);
        if (fiveRate >= 1.0) return 5;
        double fiveWithinGuarantee = fiveRate / (fiveRate + 0.12);
        return random.nextDouble() < fiveWithinGuarantee ? 5 : 4;
    }

    private static void spend(PlayerProfile profile, int amount) {
        if (!profile.spend(PlayerProfile.Currency.SUMMON_CRYSTAL, amount)) {
            throw new IllegalStateException("Not enough Summon Crystal");
        }
    }
}
