package dev.moonseungjun.openworldrpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record RootClassSpecializationState(
        Set<ClassSpecialization> unlocked,
        Optional<ClassSpecialization> active
) {
    private static final Codec<Set<ClassSpecialization>> SPECIALIZATION_SET_CODEC =
            ClassSpecialization.CODEC.listOf().xmap(
                    list -> Set.copyOf(list),
                    set -> set.stream()
                            .sorted(Comparator.comparing(ClassSpecialization::id))
                            .toList()
            );

    public static final Codec<RootClassSpecializationState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    SPECIALIZATION_SET_CODEC
                            .optionalFieldOf("unlocked", Set.of())
                            .forGetter(RootClassSpecializationState::unlocked),
                    ClassSpecialization.CODEC
                            .optionalFieldOf("active")
                            .forGetter(RootClassSpecializationState::active)
            ).apply(instance, RootClassSpecializationState::new));

    public RootClassSpecializationState {
        unlocked = Set.copyOf(Objects.requireNonNull(unlocked, "unlocked"));
        active = Objects.requireNonNull(active, "active");
        if (unlocked.size() > 2) {
            throw new IllegalArgumentException(
                    "A root class can unlock at most two launch specializations."
            );
        }
        if (active.isPresent() && !unlocked.contains(active.orElseThrow())) {
            throw new IllegalArgumentException(
                    "Active specialization must already be unlocked."
            );
        }
    }

    public static RootClassSpecializationState initial() {
        return new RootClassSpecializationState(Set.of(), Optional.empty());
    }

    public boolean isUnlocked(ClassSpecialization specialization) {
        return unlocked.contains(
                Objects.requireNonNull(specialization, "specialization")
        );
    }

    public RootClassSpecializationState unlock(
            ClassSpecialization specialization
    ) {
        Objects.requireNonNull(specialization, "specialization");
        if (unlocked.contains(specialization)) {
            return this;
        }
        if (unlocked.size() >= 2) {
            throw new IllegalStateException(
                    "Both launch specializations are already unlocked."
            );
        }
        Set<ClassSpecialization> next = new HashSet<>(unlocked);
        next.add(specialization);
        return new RootClassSpecializationState(
                Set.copyOf(next),
                active.isPresent() ? active : Optional.of(specialization)
        );
    }

    public RootClassSpecializationState activate(
            ClassSpecialization specialization
    ) {
        Objects.requireNonNull(specialization, "specialization");
        if (!unlocked.contains(specialization)) {
            throw new IllegalStateException(
                    "Cannot activate a locked specialization."
            );
        }
        if (active.equals(Optional.of(specialization))) {
            return this;
        }
        return new RootClassSpecializationState(
                unlocked,
                Optional.of(specialization)
        );
    }
}
