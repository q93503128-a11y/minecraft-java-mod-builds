package dev.moonseungjun.fishinggame.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.fishinggame.fishing.CollectionRewards;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishSpecies;

public record PlayerFishingProfile(
        int coins,
        int rodTier,
        List<CatchEntry> catches,
        List<FishRecord> records,
        int rebirths
) {
    public static final int BAG_CAPACITY = 40;

    public static final Codec<PlayerFishingProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("coins", 0).forGetter(PlayerFishingProfile::coins),
            Codec.INT.optionalFieldOf("rod_tier", 0).forGetter(PlayerFishingProfile::rodTier),
            CatchEntry.CODEC.listOf().optionalFieldOf("catches", List.of()).forGetter(PlayerFishingProfile::catches),
            FishRecord.CODEC.listOf().optionalFieldOf("records", List.of()).forGetter(PlayerFishingProfile::records),
            Codec.INT.optionalFieldOf("rebirths", 0).forGetter(PlayerFishingProfile::rebirths)
    ).apply(instance, PlayerFishingProfile::new));

    public PlayerFishingProfile(int coins, int rodTier, List<CatchEntry> catches, List<FishRecord> records) {
        this(coins, rodTier, catches, records, 0);
    }

    public PlayerFishingProfile {
        coins = Math.max(0, coins);
        rodTier = Math.max(0, Math.min(2, rodTier));
        catches = catches == null ? List.of() : List.copyOf(catches);
        records = normalizeRecords(records == null ? List.of() : records, catches);
        rebirths = Math.max(0, Math.min(FishingPrestige.MAX_REBIRTHS, rebirths));
    }

    public static PlayerFishingProfile empty() {
        return new PlayerFishingProfile(0, 0, List.of(), List.of(), 0);
    }

    public boolean bagFull() {
        return catches.size() >= BAG_CAPACITY;
    }

    public int baseBagValue() {
        return catches.stream().mapToInt(CatchEntry::value).sum();
    }

    public int bagValue() {
        return FishingPrestige.boostedSaleValue(baseBagValue(), rebirths);
    }

    public double saleMultiplier() {
        return FishingPrestige.saleMultiplier(rebirths);
    }

    public Optional<FishRecord> recordFor(String speciesId) {
        return records.stream().filter(record -> record.speciesId().equals(speciesId)).findFirst();
    }

    public PlayerFishingProfile addCatch(CatchEntry entry) {
        if (bagFull()) return this;

        ArrayList<CatchEntry> nextCatches = new ArrayList<>(catches);
        nextCatches.add(entry);

        ArrayList<FishRecord> nextRecords = new ArrayList<>(records);
        int recordIndex = -1;
        for (int i = 0; i < nextRecords.size(); i++) {
            if (nextRecords.get(i).speciesId().equals(entry.speciesId())) {
                recordIndex = i;
                break;
            }
        }
        if (recordIndex >= 0) {
            nextRecords.set(recordIndex, nextRecords.get(recordIndex).add(entry));
        } else {
            nextRecords.add(FishRecord.fromCatch(entry));
        }

        FishSpecies species = FishCatalog.byId(entry.speciesId());
        CollectionRewards.Reward reward = CollectionRewards.rewardForCatch(records, nextRecords, species);
        return new PlayerFishingProfile(
                safeAddCoins(coins, reward.totalCoins()),
                rodTier,
                nextCatches,
                nextRecords,
                rebirths
        );
    }

    public PlayerFishingProfile sellAll() {
        if (catches.isEmpty()) return this;
        return new PlayerFishingProfile(safeAddCoins(coins, bagValue()), rodTier, List.of(), records, rebirths);
    }

    public PlayerFishingProfile withRodTierAndCoins(int nextTier, int nextCoins) {
        return new PlayerFishingProfile(nextCoins, nextTier, catches, records, rebirths);
    }

    public PlayerFishingProfile rebirth() {
        if (rebirths >= FishingPrestige.MAX_REBIRTHS) return this;
        return new PlayerFishingProfile(0, 0, List.of(), records, rebirths + 1);
    }

    private static int safeAddCoins(int current, int amount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) current + Math.max(0, amount)));
    }

    private static List<FishRecord> normalizeRecords(List<FishRecord> savedRecords, List<CatchEntry> catches) {
        Map<String, FishRecord> normalized = new LinkedHashMap<>();
        for (FishRecord record : savedRecords) {
            FishRecord existing = normalized.get(record.speciesId());
            if (existing == null) {
                normalized.put(record.speciesId(), record);
            } else {
                normalized.put(record.speciesId(), new FishRecord(
                        record.speciesId(),
                        existing.caughtCount() + record.caughtCount(),
                        Math.max(existing.bestWeightGrams(), record.bestWeightGrams()),
                        Math.max(existing.bestLengthMm(), record.bestLengthMm())
                ));
            }
        }

        if (normalized.isEmpty()) {
            for (CatchEntry entry : catches) {
                FishRecord existing = normalized.get(entry.speciesId());
                normalized.put(entry.speciesId(), existing == null ? FishRecord.fromCatch(entry) : existing.add(entry));
            }
        } else {
            for (CatchEntry entry : catches) {
                normalized.putIfAbsent(entry.speciesId(), FishRecord.fromCatch(entry));
            }
        }
        return List.copyOf(normalized.values());
    }
}
