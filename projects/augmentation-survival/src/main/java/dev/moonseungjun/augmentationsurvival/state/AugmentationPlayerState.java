package dev.moonseungjun.augmentationsurvival.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent server-owned player state. */
public record AugmentationPlayerState(
        int schemaVersion,
        Map<String, Integer> augmentLevels,
        List<String> activeSlots,
        Map<String, Long> cooldownReadyTicks,
        List<String> stolenAuthorityBossIds,
        List<String> bossFirstKillRewardIds,
        Map<String, Integer> persistentInts
) {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public static final Codec<AugmentationPlayerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(AugmentationPlayerState::schemaVersion),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("augment_levels", Map.of()).forGetter(AugmentationPlayerState::augmentLevels),
            Codec.STRING.listOf().optionalFieldOf("active_slots", List.of()).forGetter(AugmentationPlayerState::activeSlots),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldown_ready_ticks", Map.of()).forGetter(AugmentationPlayerState::cooldownReadyTicks),
            Codec.STRING.listOf().optionalFieldOf("stolen_authority_boss_ids", List.of()).forGetter(AugmentationPlayerState::stolenAuthorityBossIds),
            Codec.STRING.listOf().optionalFieldOf("boss_first_kill_reward_ids", List.of()).forGetter(AugmentationPlayerState::bossFirstKillRewardIds),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("persistent_ints", Map.of()).forGetter(AugmentationPlayerState::persistentInts)
    ).apply(instance, AugmentationPlayerState::new));

    public AugmentationPlayerState {
        if (schemaVersion <= 0) throw new IllegalArgumentException("schemaVersion must be positive");
        if (activeSlots.size() > 10) throw new IllegalArgumentException("activeSlots cannot exceed 10 entries");
        for (Map.Entry<String, Integer> entry : augmentLevels.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) throw new IllegalArgumentException("augmentLevels cannot contain a blank id");
            if (entry.getValue() == null || entry.getValue() <= 0) throw new IllegalArgumentException("augmentLevels must contain positive levels");
        }
        for (Map.Entry<String, Integer> entry : persistentInts.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) throw new IllegalArgumentException("persistentInts cannot contain a blank id");
            if (entry.getValue() == null || entry.getValue() <= 0) throw new IllegalArgumentException("persistentInts must contain positive values");
        }
        augmentLevels = Map.copyOf(augmentLevels);
        activeSlots = List.copyOf(activeSlots);
        cooldownReadyTicks = Map.copyOf(cooldownReadyTicks);
        stolenAuthorityBossIds = List.copyOf(stolenAuthorityBossIds);
        bossFirstKillRewardIds = List.copyOf(bossFirstKillRewardIds);
        persistentInts = Map.copyOf(persistentInts);
    }

    public static AugmentationPlayerState initial() {
        return new AugmentationPlayerState(CURRENT_SCHEMA_VERSION, Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of());
    }

    public int level(String augmentId) { return augmentLevels.getOrDefault(augmentId, 0); }

    public int persistentInt(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("persistent int key must not be blank");
        return persistentInts.getOrDefault(key, 0);
    }

    public AugmentationPlayerState withPersistentInt(String key, int value) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("persistent int key must not be blank");
        int normalized = Math.max(0, value);
        if (persistentInt(key) == normalized) return this;
        Map<String, Integer> values = new HashMap<>(persistentInts);
        if (normalized == 0) values.remove(key); else values.put(key, normalized);
        return new AugmentationPlayerState(CURRENT_SCHEMA_VERSION, augmentLevels, activeSlots, cooldownReadyTicks,
                stolenAuthorityBossIds, bossFirstKillRewardIds, values);
    }

    public AugmentationPlayerState withAugmentLevel(String augmentId, int level) {
        if (augmentId == null || augmentId.isBlank()) throw new IllegalArgumentException("augmentId must not be blank");
        if (level <= 0) throw new IllegalArgumentException("augment level must be positive");
        if (level(augmentId) == level) return this;
        Map<String, Integer> levels = new HashMap<>(augmentLevels);
        levels.put(augmentId, level);
        return new AugmentationPlayerState(schemaVersion, levels, activeSlots, cooldownReadyTicks,
                stolenAuthorityBossIds, bossFirstKillRewardIds, persistentInts);
    }

    public AugmentationPlayerState withActiveSlots(List<String> slots) {
        if (slots == null) throw new IllegalArgumentException("slots must not be null");
        if (slots.size() > 10) throw new IllegalArgumentException("active slots cannot exceed 10 entries");
        List<String> normalized = new ArrayList<>(10);
        for (int i = 0; i < slots.size(); i++) normalized.add(slots.get(i) == null ? "" : slots.get(i));
        while (normalized.size() < 10) normalized.add("");
        if (activeSlots.equals(normalized)) return this;
        return new AugmentationPlayerState(schemaVersion, augmentLevels, normalized, cooldownReadyTicks,
                stolenAuthorityBossIds, bossFirstKillRewardIds, persistentInts);
    }

    public long cooldownReadyTick(String augmentId) {
        if (augmentId == null || augmentId.isBlank()) throw new IllegalArgumentException("augmentId must not be blank");
        return cooldownReadyTicks.getOrDefault(augmentId, 0L);
    }

    public AugmentationPlayerState withCooldownReadyTick(String augmentId, long readyTick) {
        if (augmentId == null || augmentId.isBlank()) throw new IllegalArgumentException("augmentId must not be blank");
        if (readyTick < 0L) throw new IllegalArgumentException("readyTick cannot be negative");
        if (cooldownReadyTick(augmentId) == readyTick) return this;
        Map<String, Long> cooldowns = new HashMap<>(cooldownReadyTicks);
        if (readyTick == 0L) cooldowns.remove(augmentId); else cooldowns.put(augmentId, readyTick);
        return new AugmentationPlayerState(schemaVersion, augmentLevels, activeSlots, cooldowns,
                stolenAuthorityBossIds, bossFirstKillRewardIds, persistentInts);
    }

    public boolean hasStolenAuthority(String bossId) { return bossId != null && stolenAuthorityBossIds.contains(bossId); }

    public AugmentationPlayerState withStolenAuthority(String bossId) {
        if (bossId == null || bossId.isBlank()) throw new IllegalArgumentException("bossId must not be blank");
        if (hasStolenAuthority(bossId)) return this;
        ArrayList<String> stolen = new ArrayList<>(stolenAuthorityBossIds);
        stolen.add(bossId);
        return new AugmentationPlayerState(schemaVersion, augmentLevels, activeSlots, cooldownReadyTicks,
                stolen, bossFirstKillRewardIds, persistentInts);
    }

    public boolean hasBossFirstKillReward(String bossId) { return bossFirstKillRewardIds.contains(bossId); }

    public AugmentationPlayerState withBossFirstKillReward(String bossId) {
        if (bossId == null || bossId.isBlank()) throw new IllegalArgumentException("bossId must not be blank");
        if (hasBossFirstKillReward(bossId)) return this;
        ArrayList<String> claimed = new ArrayList<>(bossFirstKillRewardIds);
        claimed.add(bossId);
        return new AugmentationPlayerState(schemaVersion, augmentLevels, activeSlots, cooldownReadyTicks,
                stolenAuthorityBossIds, claimed, persistentInts);
    }
}
