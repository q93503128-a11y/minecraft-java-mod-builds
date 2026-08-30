package io.github.q93503128.turnbound.progression;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Server-authoritative v0.4 player economy/collection state. */
public final class PlayerProfile {
    public static final int PARTY_PRESET_COUNT = 3;
    public enum Currency { GOLD, SUMMON_CRYSTAL, STAR_ESSENCE, AWAKENING_CORE }
    public record Acquisition(String characterId, int nativeStars, boolean newlyOwned, int starEssenceGranted) {}
    public record SummonHistory(String characterId, int nativeStars, boolean newlyOwned, int starEssenceGranted, int pityAfter) {}

    public record Snapshot(
            long gold, long summonCrystal, long starEssence, long awakeningCore,
            Set<String> ownedCharacters, int fiveStarPity,
            boolean starterArchiveUnlocked, boolean starterArchiveUsed,
            List<SummonHistory> summonHistory, List<List<String>> partyPresets) {
        public Snapshot(long gold, long summonCrystal, long starEssence, long awakeningCore,
                        Set<String> ownedCharacters, int fiveStarPity,
                        boolean starterArchiveUnlocked, boolean starterArchiveUsed) {
            this(gold, summonCrystal, starEssence, awakeningCore, ownedCharacters, fiveStarPity,
                    starterArchiveUnlocked, starterArchiveUsed, List.of(), emptyPresets());
        }
        public Snapshot(long gold, long summonCrystal, long starEssence, long awakeningCore,
                        Set<String> ownedCharacters, int fiveStarPity,
                        boolean starterArchiveUnlocked, boolean starterArchiveUsed,
                        List<SummonHistory> summonHistory) {
            this(gold, summonCrystal, starEssence, awakeningCore, ownedCharacters, fiveStarPity,
                    starterArchiveUnlocked, starterArchiveUsed, summonHistory, emptyPresets());
        }
        public Snapshot {
            if (gold < 0 || summonCrystal < 0 || starEssence < 0 || awakeningCore < 0) throw new IllegalArgumentException("Negative TURNBOUND currency");
            if (fiveStarPity < 0 || fiveStarPity >= GachaCatalog.HARD_PITY) throw new IllegalArgumentException("Invalid five-star pity " + fiveStarPity);
            ownedCharacters = Set.copyOf(ownedCharacters);
            for (String id : ownedCharacters) if (!GachaCatalog.isSummonable(id)) throw new IllegalArgumentException("Unknown owned character " + id);
            if (starterArchiveUsed && !starterArchiveUnlocked) throw new IllegalArgumentException("Starter Archive cannot be used before unlock");
            summonHistory = List.copyOf(summonHistory == null ? List.of() : summonHistory);
            if (summonHistory.size() > GachaCatalog.HISTORY_LIMIT) {
                summonHistory = summonHistory.subList(summonHistory.size() - GachaCatalog.HISTORY_LIMIT, summonHistory.size());
            }
            partyPresets = normalizePresets(partyPresets, ownedCharacters);
        }
    }

    private long gold, summonCrystal, starEssence, awakeningCore;
    private final Set<String> ownedCharacters = new LinkedHashSet<>();
    private final List<SummonHistory> summonHistory = new ArrayList<>();
    private final List<List<String>> partyPresets = new ArrayList<>(emptyPresets());
    private int fiveStarPity;
    private boolean starterArchiveUnlocked, starterArchiveUsed;

    private PlayerProfile() {}
    public static PlayerProfile newGame() { PlayerProfile profile = new PlayerProfile(); profile.gold = 5_000; return profile; }

    public static PlayerProfile restore(Snapshot snapshot) {
        PlayerProfile profile = new PlayerProfile();
        profile.gold=snapshot.gold(); profile.summonCrystal=snapshot.summonCrystal(); profile.starEssence=snapshot.starEssence(); profile.awakeningCore=snapshot.awakeningCore();
        profile.ownedCharacters.addAll(snapshot.ownedCharacters()); profile.fiveStarPity=snapshot.fiveStarPity();
        profile.starterArchiveUnlocked=snapshot.starterArchiveUnlocked(); profile.starterArchiveUsed=snapshot.starterArchiveUsed();
        profile.summonHistory.addAll(snapshot.summonHistory());
        profile.partyPresets.clear(); profile.partyPresets.addAll(snapshot.partyPresets());
        return profile;
    }

    public Snapshot snapshot() { return new Snapshot(gold,summonCrystal,starEssence,awakeningCore,ownedCharacters,fiveStarPity,starterArchiveUnlocked,starterArchiveUsed,summonHistory,partyPresets); }

    public long currency(Currency currency) { return switch (currency) { case GOLD->gold; case SUMMON_CRYSTAL->summonCrystal; case STAR_ESSENCE->starEssence; case AWAKENING_CORE->awakeningCore; }; }
    public void grant(Currency currency,long amount) {
        if(amount<0) throw new IllegalArgumentException("Negative currency grant");
        switch(currency){case GOLD->gold=Math.addExact(gold,amount);case SUMMON_CRYSTAL->summonCrystal=Math.addExact(summonCrystal,amount);case STAR_ESSENCE->starEssence=Math.addExact(starEssence,amount);case AWAKENING_CORE->awakeningCore=Math.addExact(awakeningCore,amount);}
    }
    public boolean spend(Currency currency,long amount) {
        if(amount<0) throw new IllegalArgumentException("Negative currency spend"); if(currency(currency)<amount) return false;
        switch(currency){case GOLD->gold-=amount;case SUMMON_CRYSTAL->summonCrystal-=amount;case STAR_ESSENCE->starEssence-=amount;case AWAKENING_CORE->awakeningCore-=amount;} return true;
    }

    public boolean owns(String characterId){return ownedCharacters.contains(characterId);} public Set<String> ownedCharacters(){return Set.copyOf(ownedCharacters);}
    public Acquisition acquireCharacter(String characterId) {
        int stars=GachaCatalog.nativeStars(characterId); if(ownedCharacters.add(characterId)) return new Acquisition(characterId,stars,true,0);
        int essence=GachaCatalog.duplicateEssence(stars); grant(Currency.STAR_ESSENCE,essence); return new Acquisition(characterId,stars,false,essence);
    }
    public int fiveStarPity(){return fiveStarPity;} public List<SummonHistory> summonHistory(){return List.copyOf(summonHistory);}
    void recordSummonRarity(int nativeStars){if(nativeStars==5) fiveStarPity=0; else fiveStarPity=Math.min(GachaCatalog.HARD_PITY-1,fiveStarPity+1);}
    void recordSummonHistory(Acquisition acquisition) {
        summonHistory.add(new SummonHistory(acquisition.characterId(), acquisition.nativeStars(), acquisition.newlyOwned(), acquisition.starEssenceGranted(), fiveStarPity));
        while (summonHistory.size() > GachaCatalog.HISTORY_LIMIT) summonHistory.removeFirst();
    }

    public List<List<String>> partyPresets() { return List.copyOf(partyPresets); }
    public List<String> partyPreset(int index) {
        if (index < 0 || index >= PARTY_PRESET_COUNT) throw new IllegalArgumentException("Party preset index must be 0..2");
        return partyPresets.get(index);
    }
    public void savePartyPreset(int index, List<String> party) {
        if (index < 0 || index >= PARTY_PRESET_COUNT) throw new IllegalArgumentException("Party preset index must be 0..2");
        partyPresets.set(index, validatePreset(party, ownedCharacters));
    }

    public void unlockStarterArchive(){starterArchiveUnlocked=true;} public boolean starterArchiveAvailable(){return starterArchiveUnlocked&&!starterArchiveUsed;}
    void consumeStarterArchive(){if(!starterArchiveAvailable()) throw new IllegalStateException("Starter Archive is not available"); starterArchiveUsed=true;}

    private static List<List<String>> emptyPresets() { return List.of(List.of(), List.of(), List.of()); }
    private static List<List<String>> normalizePresets(List<List<String>> raw, Set<String> owned) {
        List<List<String>> out = new ArrayList<>();
        if (raw != null) for (List<String> preset : raw) { if (out.size() >= PARTY_PRESET_COUNT) break; out.add(validatePreset(preset, owned)); }
        while (out.size() < PARTY_PRESET_COUNT) out.add(List.of());
        return List.copyOf(out);
    }
    private static List<String> validatePreset(List<String> party, Set<String> owned) {
        if (party == null || party.isEmpty()) return List.of();
        if (party.size() > 4) throw new IllegalArgumentException("Party preset exceeds four characters");
        LinkedHashSet<String> unique = new LinkedHashSet<>(party);
        if (unique.size() != party.size()) throw new IllegalArgumentException("Party preset contains duplicate characters");
        for (String id : unique) if (!owned.contains(id)) throw new IllegalArgumentException("Party preset contains unowned character " + id);
        return List.copyOf(unique);
    }
}
