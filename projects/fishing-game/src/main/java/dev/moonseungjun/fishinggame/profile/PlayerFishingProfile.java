package dev.moonseungjun.fishinggame.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PlayerFishingProfile(int coins, int rodTier, List<CatchEntry> catches, List<FishRecord> records) {
    public static final int BAG_CAPACITY = 40;

    public static final Codec<PlayerFishingProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("coins", 0).forGetter(PlayerFishingProfile::coins),
            Codec.INT.optionalFieldOf("rod_tier", 0).forGetter(PlayerFishingProfile::rodTier),
            CatchEntry.CODEC.listOf().optionalFieldOf("catches", List.of()).forGetter(PlayerFishingProfile::catches),
            FishRecord.CODEC.listOf().optionalFieldOf("records", List.of()).forGetter(PlayerFishingProfile::records)
    ).apply(instance, PlayerFishingProfile::new));

    public PlayerFishingProfile {
        coins = Math.max(0, coins);
        rodTier = Math.max(0, Math.min(2, rodTier));
        catches = catches == null ? List.of() : List.copyOf(catches);
        records = normalizeRecords(records == null ? List.of() : records, catches);
    }

    public static PlayerFishingProfile empty() {
        return new PlayerFishingProfile(0, 0, List.of(), List.of());
    }

    public boolean bagFull() {
        return catches.size() >= BAG_CAPACITY;
    }

    public int bagValue() {
        return catches.stream().mapToInt(CatchEntry::value).sum();
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

        return new PlayerFishingProfile(coins, rodTier, nextCatches, nextRecords);
    }

    public PlayerFishingProfile sellAll() {
        if (catches.isEmpty()) return this;
        return new PlayerFishingProfile(coins + bagValue(), rodTier, List.of(), records);
    }

    public PlayerFishingProfile withRodTierAndCoins(int nextTier, int nextCoins) {
        return new PlayerFishingProfile(nextCoins, nextTier, catches, records);
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
