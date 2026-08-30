package io.github.q93503128.turnbound.progression;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Server-authoritative v0.4 player economy/collection state.
 * Persistence adapters should store/restore {@link Snapshot}; gameplay code must mutate this object instead of UI state.
 */
public final class PlayerProfile {
    public enum Currency { GOLD, SUMMON_CRYSTAL, STAR_ESSENCE, AWAKENING_CORE }

    public record Acquisition(String characterId, int nativeStars, boolean newlyOwned, int starEssenceGranted) {}

    public record Snapshot(
            long gold,
            long summonCrystal,
            long starEssence,
            long awakeningCore,
            Set<String> ownedCharacters,
            int fiveStarPity,
            boolean starterArchiveUnlocked,
            boolean starterArchiveUsed) {
        public Snapshot {
            if (gold < 0 || summonCrystal < 0 || starEssence < 0 || awakeningCore < 0) {
                throw new IllegalArgumentException("Negative TURNBOUND currency");
            }
            if (fiveStarPity < 0 || fiveStarPity >= GachaCatalog.HARD_PITY) {
                throw new IllegalArgumentException("Invalid five-star pity " + fiveStarPity);
            }
            ownedCharacters = Set.copyOf(ownedCharacters);
            for (String id : ownedCharacters) {
                if (!GachaCatalog.isSummonable(id)) throw new IllegalArgumentException("Unknown owned character " + id);
            }
            if (starterArchiveUsed && !starterArchiveUnlocked) {
                throw new IllegalArgumentException("Starter Archive cannot be used before unlock");
            }
        }
    }

    private long gold;
    private long summonCrystal;
    private long starEssence;
    private long awakeningCore;
    private final Set<String> ownedCharacters = new LinkedHashSet<>();
    private int fiveStarPity;
    private boolean starterArchiveUnlocked;
    private boolean starterArchiveUsed;

    private PlayerProfile() {}

    public static PlayerProfile newGame() {
        PlayerProfile profile = new PlayerProfile();
        profile.gold = 5_000;
        return profile;
    }

    public static PlayerProfile restore(Snapshot snapshot) {
        PlayerProfile profile = new PlayerProfile();
        profile.gold = snapshot.gold();
        profile.summonCrystal = snapshot.summonCrystal();
        profile.starEssence = snapshot.starEssence();
        profile.awakeningCore = snapshot.awakeningCore();
        profile.ownedCharacters.addAll(snapshot.ownedCharacters());
        profile.fiveStarPity = snapshot.fiveStarPity();
        profile.starterArchiveUnlocked = snapshot.starterArchiveUnlocked();
        profile.starterArchiveUsed = snapshot.starterArchiveUsed();
        return profile;
    }

    public Snapshot snapshot() {
        return new Snapshot(gold, summonCrystal, starEssence, awakeningCore, ownedCharacters,
                fiveStarPity, starterArchiveUnlocked, starterArchiveUsed);
    }

    public long currency(Currency currency) {
        return switch (currency) {
            case GOLD -> gold;
            case SUMMON_CRYSTAL -> summonCrystal;
            case STAR_ESSENCE -> starEssence;
            case AWAKENING_CORE -> awakeningCore;
        };
    }

    public void grant(Currency currency, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative currency grant");
        switch (currency) {
            case GOLD -> gold = Math.addExact(gold, amount);
            case SUMMON_CRYSTAL -> summonCrystal = Math.addExact(summonCrystal, amount);
            case STAR_ESSENCE -> starEssence = Math.addExact(starEssence, amount);
            case AWAKENING_CORE -> awakeningCore = Math.addExact(awakeningCore, amount);
        }
    }

    public boolean spend(Currency currency, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative currency spend");
        long balance = currency(currency);
        if (balance < amount) return false;
        switch (currency) {
            case GOLD -> gold -= amount;
            case SUMMON_CRYSTAL -> summonCrystal -= amount;
            case STAR_ESSENCE -> starEssence -= amount;
            case AWAKENING_CORE -> awakeningCore -= amount;
        }
        return true;
    }

    public boolean owns(String characterId) {
        return ownedCharacters.contains(characterId);
    }

    public Set<String> ownedCharacters() {
        return Set.copyOf(ownedCharacters);
    }

    public Acquisition acquireCharacter(String characterId) {
        int nativeStars = GachaCatalog.nativeStars(characterId);
        if (ownedCharacters.add(characterId)) {
            return new Acquisition(characterId, nativeStars, true, 0);
        }
        int essence = GachaCatalog.duplicateEssence(nativeStars);
        grant(Currency.STAR_ESSENCE, essence);
        return new Acquisition(characterId, nativeStars, false, essence);
    }

    public int fiveStarPity() {
        return fiveStarPity;
    }

    void recordSummonRarity(int nativeStars) {
        if (nativeStars == 5) fiveStarPity = 0;
        else fiveStarPity = Math.min(GachaCatalog.HARD_PITY - 1, fiveStarPity + 1);
    }

    public void unlockStarterArchive() {
        starterArchiveUnlocked = true;
    }

    public boolean starterArchiveAvailable() {
        return starterArchiveUnlocked && !starterArchiveUsed;
    }

    void consumeStarterArchive() {
        if (!starterArchiveAvailable()) throw new IllegalStateException("Starter Archive is not available");
        starterArchiveUsed = true;
    }
}
