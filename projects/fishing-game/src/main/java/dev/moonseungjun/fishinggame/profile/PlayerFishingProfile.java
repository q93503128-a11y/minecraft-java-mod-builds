package dev.moonseungjun.fishinggame.profile;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PlayerFishingProfile(int coins, int rodTier, List<CatchEntry> catches) {
    public static final int BAG_CAPACITY = 40;

    public static final Codec<PlayerFishingProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("coins", 0).forGetter(PlayerFishingProfile::coins),
            Codec.INT.optionalFieldOf("rod_tier", 0).forGetter(PlayerFishingProfile::rodTier),
            CatchEntry.CODEC.listOf().optionalFieldOf("catches", List.of()).forGetter(PlayerFishingProfile::catches)
    ).apply(instance, PlayerFishingProfile::new));

    public PlayerFishingProfile {
        coins = Math.max(0, coins);
        rodTier = Math.max(0, Math.min(2, rodTier));
        catches = catches == null ? List.of() : List.copyOf(catches);
    }

    public static PlayerFishingProfile empty() {
        return new PlayerFishingProfile(0, 0, List.of());
    }

    public boolean bagFull() {
        return catches.size() >= BAG_CAPACITY;
    }

    public int bagValue() {
        return catches.stream().mapToInt(CatchEntry::value).sum();
    }

    public PlayerFishingProfile addCatch(CatchEntry entry) {
        if (bagFull()) return this;
        ArrayList<CatchEntry> next = new ArrayList<>(catches);
        next.add(entry);
        return new PlayerFishingProfile(coins, rodTier, next);
    }

    public PlayerFishingProfile sellAll() {
        if (catches.isEmpty()) return this;
        return new PlayerFishingProfile(coins + bagValue(), rodTier, List.of());
    }

    public PlayerFishingProfile withRodTierAndCoins(int nextTier, int nextCoins) {
        return new PlayerFishingProfile(nextCoins, nextTier, catches);
    }
}
