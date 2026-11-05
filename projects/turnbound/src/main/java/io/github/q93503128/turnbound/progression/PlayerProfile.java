package io.github.q93503128.turnbound.progression;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

/** Server-authoritative v0.4 player economy/collection state. */
public final class PlayerProfile {
    public static final int PARTY_PRESET_COUNT = 3;
    public enum Currency { GOLD, SUMMON_CRYSTAL, STAR_ESSENCE, AWAKENING_CORE }
    public record Acquisition(String characterId, int nativeStars, boolean newlyOwned, int starEssenceGranted) {}
    public record SummonHistory(
            String characterId, int nativeStars, boolean newlyOwned, int starEssenceGranted, int pityAfter,
            int bonusLevelGranted, int bonusLevelAfter) {
        public SummonHistory(String characterId, int nativeStars, boolean newlyOwned, int starEssenceGranted, int pityAfter) {
            this(characterId, nativeStars, newlyOwned, starEssenceGranted, pityAfter, 0, 0);
        }
    }

    public record Snapshot(
            long gold, long summonCrystal, long starEssence, long awakeningCore,
            Set<String> ownedCharacters, int fiveStarPity,
            boolean starterArchiveUnlocked, boolean starterArchiveUsed,
            List<SummonHistory> summonHistory, List<List<String>> partyPresets, Map<String, Integer> pendingDuplicates) {
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
        public Snapshot(long gold, long summonCrystal, long starEssence, long awakeningCore,
                        Set<String> ownedCharacters, int fiveStarPity,
                        boolean starterArchiveUnlocked, boolean starterArchiveUsed,
                        List<SummonHistory> summonHistory, List<List<String>> partyPresets) {
            this(gold, summonCrystal, starEssence, awakeningCore, ownedCharacters, fiveStarPity,
                    starterArchiveUnlocked, starterArchiveUsed, summonHistory, partyPresets, Map.of());
        }
        public Snapshot {
            if (gold < 0 || summonCrystal < 0 || starEssence < 0 || awakeningCore < 0) throw new IllegalArgumentException("Negative TURNBOUND currency");
            if (fiveStarPity < 0) throw new IllegalArgumentException("Invalid five-star pity " + fiveStarPity);
            // Preserve the canonical 0..79 pity range. Only malformed/out-of-range values are defensively clamped.
            if (fiveStarPity >= GachaCatalog.HARD_PITY) fiveStarPity = GachaCatalog.HARD_PITY - 1;
            ownedCharacters = Set.copyOf(ownedCharacters);
            for (String id : ownedCharacters) if (!GachaCatalog.isKnownCharacter(id)) throw new IllegalArgumentException("Unknown owned character " + id);
            if (starterArchiveUsed && !starterArchiveUnlocked) throw new IllegalArgumentException("Starter Archive cannot be used before unlock");
            summonHistory = List.copyOf(summonHistory == null ? List.of() : summonHistory);
            if (summonHistory.size() > GachaCatalog.HISTORY_LIMIT) {
                summonHistory = summonHistory.subList(summonHistory.size() - GachaCatalog.HISTORY_LIMIT, summonHistory.size());
            }
            partyPresets = normalizePresets(partyPresets, ownedCharacters);
            pendingDuplicates = Map.copyOf(pendingDuplicates == null ? Map.of() : pendingDuplicates);
            for (var entry : pendingDuplicates.entrySet()) {
                if (!ownedCharacters.contains(entry.getKey()) || entry.getValue() == null || entry.getValue() <= 0)
                    throw new IllegalArgumentException("Invalid pending duplicate choice");
            }
        }
    }

    private long gold, summonCrystal, starEssence, awakeningCore;
    private final Set<String> ownedCharacters = new LinkedHashSet<>();
    private final List<SummonHistory> summonHistory = new ArrayList<>();
    private final Map<String, Integer> pendingDuplicates = new LinkedHashMap<>();
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
        profile.pendingDuplicates.putAll(snapshot.pendingDuplicates());
        profile.partyPresets.clear(); profile.partyPresets.addAll(snapshot.partyPresets());
        return profile;
    }

    public Snapshot snapshot() { return new Snapshot(gold,summonCrystal,starEssence,awakeningCore,ownedCharacters,fiveStarPity,starterArchiveUnlocked,starterArchiveUsed,summonHistory,partyPresets,pendingDuplicates); }

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
        int stars=GachaCatalog.nativeStars(characterId);
        if(ownedCharacters.add(characterId)) return new Acquisition(characterId,stars,true,0);
        return new Acquisition(characterId,stars,false,0);
    }
    public Map<String, Integer> pendingDuplicates() { return Map.copyOf(pendingDuplicates); }
    public int pendingDuplicateCount(String characterId) { return pendingDuplicates.getOrDefault(characterId, 0); }
    public void queueDuplicateChoice(String characterId) {
        if (!owns(characterId)) throw new IllegalArgumentException("Duplicate choice requires an owned character");
        pendingDuplicates.merge(characterId, 1, Math::addExact);
    }
    public void consumeDuplicateChoice(String characterId, int essence, int bonusGranted, int bonusAfter) {
        int count = pendingDuplicateCount(characterId);
        if (count <= 0) throw new IllegalStateException("No pending duplicate reward for character");
        if (essence < 0 || bonusGranted < 0 || bonusGranted > 1 || (essence > 0) == (bonusGranted > 0))
            throw new IllegalArgumentException("Choose exactly one duplicate reward");
        if (essence > 0) grant(Currency.STAR_ESSENCE, essence);
        if (count == 1) pendingDuplicates.remove(characterId);
        else pendingDuplicates.put(characterId, count - 1);
        // Reconcile the most recent unresolved receipt while it is still in the bounded history.
        for (int i = summonHistory.size() - 1; i >= 0; i--) {
            SummonHistory row = summonHistory.get(i);
            if (!row.newlyOwned() && row.characterId().equals(characterId)
                    && row.starEssenceGranted() == 0 && row.bonusLevelGranted() == 0) {
                summonHistory.set(i, new SummonHistory(row.characterId(), row.nativeStars(), false,
                        essence, row.pityAfter(), bonusGranted, bonusAfter));
                break;
            }
        }
    }

    public int fiveStarPity(){return fiveStarPity;} public List<SummonHistory> summonHistory(){return List.copyOf(summonHistory);}
    void recordSummonRarity(int nativeStars){if(nativeStars==5) fiveStarPity=0; else fiveStarPity=Math.min(GachaCatalog.HARD_PITY-1,fiveStarPity+1);}
    void recordSummonHistory(Acquisition acquisition) {
        summonHistory.add(new SummonHistory(acquisition.characterId(), acquisition.nativeStars(), acquisition.newlyOwned(), acquisition.starEssenceGranted(), fiveStarPity));
        while (summonHistory.size() > GachaCatalog.HISTORY_LIMIT) summonHistory.removeFirst();
    }

    public void annotateRecentSummonBonuses(List<GachaService.PullResult> pulls) {
        if (pulls == null || pulls.isEmpty()) return;
        int start = summonHistory.size() - pulls.size();
        if (start < 0) throw new IllegalStateException("Summon history is shorter than the resolved batch");
        for (int i = 0; i < pulls.size(); i++) {
            GachaService.PullResult pull = pulls.get(i);
            SummonHistory old = summonHistory.get(start + i);
            if (!old.characterId().equals(pull.characterId())) {
                throw new IllegalStateException("Summon history/result order mismatch");
            }
            summonHistory.set(start + i, new SummonHistory(
                    old.characterId(), old.nativeStars(), old.newlyOwned(), pull.starEssenceGranted(), old.pityAfter(),
                    pull.bonusLevelGranted(), pull.bonusLevelAfter()));
        }
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
