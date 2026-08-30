package io.github.q93503128.turnbound.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Server-side v0.4 Standard/Starter Archive resolver. */
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
        return new BatchResult(List.of(pullStandard(profile, false, null)), GachaCatalog.SINGLE_COST);
    }

    public BatchResult summonStandardTen(PlayerProfile profile) {
        spend(profile, GachaCatalog.TEN_COST);
        List<PullResult> pulls = new ArrayList<>(10);
        boolean hasFourPlus = false;
        for (int slot = 0; slot < 10; slot++) {
            boolean guaranteeFourPlus = slot == 9 && !hasFourPlus;
            PullResult result = pullStandard(profile, guaranteeFourPlus, null);
            pulls.add(result);
            hasFourPlus |= result.nativeStars() >= 4;
        }
        return new BatchResult(pulls, GachaCatalog.TEN_COST);
    }

    public BatchResult summonStarterTen(PlayerProfile profile) {
        if (!profile.starterArchiveAvailable()) throw new IllegalStateException("Starter Archive is not available");
        String reservedGuarantee = rollStarterGuarantee(profile);
        spend(profile, GachaCatalog.TEN_COST);

        List<PullResult> pulls = new ArrayList<>(10);
        for (int slot = 0; slot < 9; slot++) pulls.add(pullStandard(profile, false, reservedGuarantee));
        pulls.add(pullSpecific(profile, reservedGuarantee));
        profile.consumeStarterArchive();
        return new BatchResult(pulls, GachaCatalog.TEN_COST);
    }

    public static double effectiveFiveStarRate(int pityBeforePull) {
        if (pityBeforePull < 0 || pityBeforePull >= GachaCatalog.HARD_PITY) throw new IllegalArgumentException("Invalid pity " + pityBeforePull);
        int pullOrdinal = pityBeforePull + 1;
        if (pullOrdinal >= GachaCatalog.HARD_PITY) return 1.0;
        if (pullOrdinal < GachaCatalog.SOFT_PITY_START) return GachaCatalog.BASE_FIVE_STAR_RATE;
        double boosted = GachaCatalog.BASE_FIVE_STAR_RATE + (pullOrdinal - GachaCatalog.SOFT_PITY_START + 1) * GachaCatalog.SOFT_PITY_STEP;
        return Math.min(1.0, boosted);
    }

    private PullResult pullStandard(PlayerProfile profile, boolean guaranteeFourPlus, String excludedCharacterId) {
        int stars = guaranteeFourPlus ? rollFourPlus(profile.fiveStarPity()) : rollRarity(profile.fiveStarPity());
        return pullSpecific(profile, pickFromPool(GachaCatalog.standardPool(stars), excludedCharacterId));
    }

    private PullResult pullSpecific(PlayerProfile profile, String characterId) {
        int stars = GachaCatalog.nativeStars(characterId);
        PlayerProfile.Acquisition acquisition = profile.acquireCharacter(characterId);
        profile.recordSummonRarity(stars);
        profile.recordSummonHistory(acquisition);
        return new PullResult(characterId, stars, acquisition.newlyOwned(), acquisition.starEssenceGranted(), profile.fiveStarPity());
    }

    private int rollRarity(int pityBeforePull) {
        double fiveRate = effectiveFiveStarRate(pityBeforePull);
        if (fiveRate >= 1.0) return 5;
        double roll = random.nextDouble();
        if (roll < fiveRate) return 5;
        double nonFive = (roll - fiveRate) / (1.0 - fiveRate);
        double total = 1.0 - GachaCatalog.BASE_FIVE_STAR_RATE;
        if (nonFive < GachaCatalog.FOUR_STAR_RATE / total) return 4;
        if (nonFive < (GachaCatalog.FOUR_STAR_RATE + GachaCatalog.THREE_STAR_RATE) / total) return 3;
        if (nonFive < (GachaCatalog.FOUR_STAR_RATE + GachaCatalog.THREE_STAR_RATE + GachaCatalog.TWO_STAR_RATE) / total) return 2;
        return 1;
    }

    private int rollFourPlus(int pityBeforePull) {
        double fiveRate = effectiveFiveStarRate(pityBeforePull);
        if (fiveRate >= 1.0) return 5;
        return random.nextDouble() < fiveRate / (fiveRate + GachaCatalog.FOUR_STAR_RATE) ? 5 : 4;
    }

    private String rollStarterGuarantee(PlayerProfile profile) {
        List<String> unowned = GachaCatalog.starterGuaranteePool().stream().filter(id -> !profile.owns(id)).toList();
        if (unowned.isEmpty()) throw new IllegalStateException("Starter Archive has no unowned ★4+ guarantee candidate");
        double total = unowned.stream().mapToDouble(GachaCatalog::standardCharacterWeight).sum();
        double roll = random.nextDouble() * total;
        double cursor = 0.0;
        for (String id : unowned) {
            cursor += GachaCatalog.standardCharacterWeight(id);
            if (roll < cursor) return id;
        }
        return unowned.getLast();
    }

    private String pickFromPool(List<String> pool, String excludedCharacterId) {
        if (excludedCharacterId == null || !pool.contains(excludedCharacterId)) return pool.get(random.nextInt(pool.size()));
        List<String> eligible = pool.stream().filter(id -> !id.equals(excludedCharacterId)).toList();
        if (eligible.isEmpty()) throw new IllegalStateException("Starter guarantee reservation exhausted a rarity pool");
        return eligible.get(random.nextInt(eligible.size()));
    }

    private static void spend(PlayerProfile profile, int amount) {
        if (!profile.spend(PlayerProfile.Currency.SUMMON_CRYSTAL, amount)) throw new IllegalStateException("Not enough Summon Crystal");
    }
}
