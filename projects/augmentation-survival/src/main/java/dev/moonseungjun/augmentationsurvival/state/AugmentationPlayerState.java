package dev.moonseungjun.augmentationsurvival.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent server-owned player state.
 *
 * <p>The schema intentionally stores stable string IDs so later augment definitions can remain
 * data-driven without save migrations for every roster edit.</p>
 */
public record AugmentationPlayerState(
        int schemaVersion,
        Map<String, Integer> augmentLevels,
        List<String> activeSlots,
        Map<String, Long> cooldownReadyTicks,
        List<String> stolenAuthorityBossIds,
        List<String> bossFirstKillRewardIds
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<AugmentationPlayerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION)
                    .forGetter(AugmentationPlayerState::schemaVersion),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("augment_levels", Map.of())
                    .forGetter(AugmentationPlayerState::augmentLevels),
            Codec.STRING.listOf()
                    .optionalFieldOf("active_slots", List.of())
                    .forGetter(AugmentationPlayerState::activeSlots),
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                    .optionalFieldOf("cooldown_ready_ticks", Map.of())
                    .forGetter(AugmentationPlayerState::cooldownReadyTicks),
            Codec.STRING.listOf()
                    .optionalFieldOf("stolen_authority_boss_ids", List.of())
                    .forGetter(AugmentationPlayerState::stolenAuthorityBossIds),
            Codec.STRING.listOf()
                    .optionalFieldOf("boss_first_kill_reward_ids", List.of())
                    .forGetter(AugmentationPlayerState::bossFirstKillRewardIds)
    ).apply(instance, AugmentationPlayerState::new));

    public AugmentationPlayerState {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        if (activeSlots.size() > 10) {
            throw new IllegalArgumentException("activeSlots cannot exceed 10 entries");
        }
        for (Map.Entry<String, Integer> entry : augmentLevels.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("augmentLevels cannot contain a blank id");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("augmentLevels must contain positive levels");
            }
        }
        augmentLevels = Map.copyOf(augmentLevels);
        activeSlots = List.copyOf(activeSlots);
        cooldownReadyTicks = Map.copyOf(cooldownReadyTicks);
        stolenAuthorityBossIds = List.copyOf(stolenAuthorityBossIds);
        bossFirstKillRewardIds = List.copyOf(bossFirstKillRewardIds);
    }

    public static AugmentationPlayerState initial() {
        return new AugmentationPlayerState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of()
        );
    }

    public int level(String augmentId) {
        return augmentLevels.getOrDefault(augmentId, 0);
    }

    public AugmentationPlayerState withAugmentLevel(String augmentId, int level) {
        if (augmentId == null || augmentId.isBlank()) {
            throw new IllegalArgumentException("augmentId must not be blank");
        }
        if (level <= 0) {
            throw new IllegalArgumentException("augment level must be positive");
        }
        if (level(augmentId) == level) {
            return this;
        }
        Map<String, Integer> levels = new HashMap<>(augmentLevels);
        levels.put(augmentId, level);
        return new AugmentationPlayerState(
                schemaVersion,
                levels,
                activeSlots,
                cooldownReadyTicks,
                stolenAuthorityBossIds,
                bossFirstKillRewardIds
        );
    }

    public long cooldownReadyTick(String augmentId) {
        if (augmentId == null || augmentId.isBlank()) {
            throw new IllegalArgumentException("augmentId must not be blank");
        }
        return cooldownReadyTicks.getOrDefault(augmentId, 0L);
    }

    public AugmentationPlayerState withCooldownReadyTick(String augmentId, long readyTick) {
        if (augmentId == null || augmentId.isBlank()) {
            throw new IllegalArgumentException("augmentId must not be blank");
        }
        if (readyTick < 0L) {
            throw new IllegalArgumentException("readyTick cannot be negative");
        }
        if (cooldownReadyTick(augmentId) == readyTick) {
            return this;
        }
        Map<String, Long> cooldowns = new HashMap<>(cooldownReadyTicks);
        if (readyTick == 0L) {
            cooldowns.remove(augmentId);
        } else {
            cooldowns.put(augmentId, readyTick);
        }
        return new AugmentationPlayerState(
                schemaVersion,
                augmentLevels,
                activeSlots,
                cooldowns,
                stolenAuthorityBossIds,
                bossFirstKillRewardIds
        );
    }

    public boolean hasBossFirstKillReward(String bossId) {
        return bossFirstKillRewardIds.contains(bossId);
    }

    public AugmentationPlayerState withBossFirstKillReward(String bossId) {
        if (bossId == null || bossId.isBlank()) {
            throw new IllegalArgumentException("bossId must not be blank");
        }
        if (hasBossFirstKillReward(bossId)) {
            return this;
        }
        java.util.ArrayList<String> claimed = new java.util.ArrayList<>(bossFirstKillRewardIds);
        claimed.add(bossId);
        return new AugmentationPlayerState(
                schemaVersion,
                augmentLevels,
                activeSlots,
                cooldownReadyTicks,
                stolenAuthorityBossIds,
                claimed
        );
    }
}
