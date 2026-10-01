package dev.moonseungjun.openworldrpg.recovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

public record R01NourishmentState(
        int schemaVersion,
        Optional<R01NourishmentMeal> meal,
        long expiresAtActiveTick
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01NourishmentState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("nourishment_schema_version")
                            .forGetter(R01NourishmentState::schemaVersion),
                    R01NourishmentMeal.CODEC.optionalFieldOf("meal")
                            .forGetter(R01NourishmentState::meal),
                    Codec.LONG.optionalFieldOf("expires_at_active_tick", 0L)
                            .forGetter(R01NourishmentState::expiresAtActiveTick)
            ).apply(instance, R01NourishmentState::new));

    public R01NourishmentState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported nourishment schema.");
        }
        meal = Objects.requireNonNull(meal, "meal");
        if (expiresAtActiveTick < 0L) {
            throw new IllegalArgumentException(
                    "Nourishment expiry must be non-negative."
            );
        }
        if (meal.isEmpty() && expiresAtActiveTick != 0L) {
            throw new IllegalArgumentException(
                    "Empty nourishment cannot retain an expiry."
            );
        }
        if (meal.isPresent() && expiresAtActiveTick <= 0L) {
            throw new IllegalArgumentException(
                    "Active nourishment requires a positive expiry."
            );
        }
    }

    public static R01NourishmentState initial() {
        return new R01NourishmentState(
                CURRENT_SCHEMA_VERSION,
                Optional.empty(),
                0L
        );
    }

    public Optional<R01NourishmentMeal> activeMeal(long activeTick) {
        if (activeTick < 0L) {
            throw new IllegalArgumentException("activeTick must be non-negative.");
        }
        return meal.filter(ignored -> activeTick < expiresAtActiveTick);
    }

    public R01NourishmentState apply(
            R01NourishmentMeal nextMeal,
            long activeTick,
            long durationTicks
    ) {
        Objects.requireNonNull(nextMeal, "nextMeal");
        if (activeTick < 0L || durationTicks <= 0L) {
            throw new IllegalArgumentException(
                    "Nourishment timing must be positive."
            );
        }
        return new R01NourishmentState(
                schemaVersion,
                Optional.of(nextMeal),
                Math.addExact(activeTick, durationTicks)
        );
    }

    public R01NourishmentState clearExpired(long activeTick) {
        if (activeMeal(activeTick).isPresent() || meal.isEmpty()) {
            return this;
        }
        return initial();
    }
}
