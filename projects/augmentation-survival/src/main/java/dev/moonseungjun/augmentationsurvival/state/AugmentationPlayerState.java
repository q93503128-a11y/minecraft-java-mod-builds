package dev.moonseungjun.augmentationsurvival.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
        List<String> stolenAuthorityBossIds
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
                    .forGetter(AugmentationPlayerState::stolenAuthorityBossIds)
    ).apply(instance, AugmentationPlayerState::new));

    public AugmentationPlayerState {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        if (activeSlots.size() > 10) {
            throw new IllegalArgumentException("activeSlots cannot exceed 10 entries");
        }
        augmentLevels = Map.copyOf(augmentLevels);
        activeSlots = List.copyOf(activeSlots);
        cooldownReadyTicks = Map.copyOf(cooldownReadyTicks);
        stolenAuthorityBossIds = List.copyOf(stolenAuthorityBossIds);
    }

    public static AugmentationPlayerState initial() {
        return new AugmentationPlayerState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                List.of(),
                Map.of(),
                List.of()
        );
    }
}
