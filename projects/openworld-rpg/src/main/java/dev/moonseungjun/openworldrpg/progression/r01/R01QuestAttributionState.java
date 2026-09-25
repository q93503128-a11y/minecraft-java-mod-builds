package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded class-ownership evidence for R01 objective progress.
 *
 * <p>Only the five Dust action categories are stored here. This prevents last-second class swapping
 * from stealing all quest Class XP without creating a generic unbounded action log.</p>
 */
public record R01QuestAttributionState(
        int schemaVersion,
        Map<String, RootClass> dustActionOwners
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01QuestAttributionState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("quest_attribution_schema_version")
                            .forGetter(R01QuestAttributionState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, RootClass.CODEC)
                            .fieldOf("dust_action_owners")
                            .forGetter(R01QuestAttributionState::dustActionOwners)
            ).apply(instance, R01QuestAttributionState::new));

    public R01QuestAttributionState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported quest-attribution schema version: " + schemaVersion
            );
        }
        dustActionOwners = Map.copyOf(
                Objects.requireNonNull(dustActionOwners, "dustActionOwners")
        );
        for (String actionId : dustActionOwners.keySet()) {
            boolean known = false;
            for (R01PlayerState.QuarryRoadAction action
                    : R01PlayerState.QuarryRoadAction.values()) {
                if (action.id().equals(actionId)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                throw new IllegalArgumentException(
                        "Unknown Dust action attribution id: " + actionId
                );
            }
        }
    }

    public static R01QuestAttributionState initial() {
        return new R01QuestAttributionState(
                CURRENT_SCHEMA_VERSION,
                Map.of()
        );
    }

    public R01QuestAttributionState recordDustAction(
            R01PlayerState.QuarryRoadAction action,
            RootClass owner
    ) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(owner, "owner");
        if (dustActionOwners.containsKey(action.id())) {
            return this;
        }

        Map<String, RootClass> next = new HashMap<>(dustActionOwners);
        next.put(action.id(), owner);
        return new R01QuestAttributionState(
                schemaVersion,
                Map.copyOf(next)
        );
    }

    /**
     * Returns the strict-majority class only. Ties/no-majority fall back to the class active at
     * completion in the owning service.
     */
    public Optional<RootClass> dustMajorityClass() {
        int total = dustActionOwners.size();
        if (total == 0) {
            return Optional.empty();
        }

        EnumMap<RootClass, Integer> counts = new EnumMap<>(RootClass.class);
        for (RootClass owner : dustActionOwners.values()) {
            counts.merge(owner, 1, Integer::sum);
        }

        for (Map.Entry<RootClass, Integer> entry : counts.entrySet()) {
            if (entry.getValue() * 2 > total) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
}
