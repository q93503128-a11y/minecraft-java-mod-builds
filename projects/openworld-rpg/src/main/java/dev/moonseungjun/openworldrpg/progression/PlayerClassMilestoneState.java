package dev.moonseungjun.openworldrpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record PlayerClassMilestoneState(
        int schemaVersion,
        Map<String, BranchMilestoneProgress> branchProgress
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Map<String, BranchMilestoneProgress>>
            PROGRESS_CODEC = Codec.unboundedMap(
                    Codec.STRING,
                    BranchMilestoneProgress.CODEC
            );

    public static final Codec<PlayerClassMilestoneState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .optionalFieldOf(
                                    "class_milestone_schema_version",
                                    CURRENT_SCHEMA_VERSION
                            )
                            .forGetter(PlayerClassMilestoneState::schemaVersion),
                    PROGRESS_CODEC
                            .optionalFieldOf("branch_progress", Map.of())
                            .forGetter(PlayerClassMilestoneState::branchProgress)
            ).apply(instance, PlayerClassMilestoneState::new));

    public PlayerClassMilestoneState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported class-milestone schema version: "
                            + schemaVersion
            );
        }
        branchProgress = Map.copyOf(
                Objects.requireNonNull(
                        branchProgress,
                        "branchProgress"
                )
        );
        for (var entry : branchProgress.entrySet()) {
            ClassSpecialization specialization =
                    ClassSpecialization.byId(entry.getKey())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Unknown specialization milestone key: "
                                                    + entry.getKey()
                                    ));
            BranchMilestoneProgress progress =
                    Objects.requireNonNull(
                            entry.getValue(),
                            "branch milestone progress"
                    );
            progress.activeDoctrine().ifPresent(doctrine -> {
                if (doctrine.specialization() != specialization) {
                    throw new IllegalArgumentException(
                            "Active doctrine belongs to a different specialization."
                    );
                }
            });
        }
    }

    public static PlayerClassMilestoneState initial() {
        return new PlayerClassMilestoneState(
                CURRENT_SCHEMA_VERSION,
                Map.of()
        );
    }

    public BranchMilestoneProgress progress(
            ClassSpecialization specialization
    ) {
        Objects.requireNonNull(specialization, "specialization");
        return branchProgress.getOrDefault(
                specialization.id(),
                BranchMilestoneProgress.initial()
        );
    }

    public PlayerClassMilestoneState update(
            ClassSpecialization specialization,
            BranchMilestoneProgress next
    ) {
        Objects.requireNonNull(specialization, "specialization");
        Objects.requireNonNull(next, "next");
        next.activeDoctrine().ifPresent(doctrine -> {
            if (doctrine.specialization() != specialization) {
                throw new IllegalArgumentException(
                        "Doctrine does not belong to specialization "
                                + specialization.id()
                );
            }
        });
        Map<String, BranchMilestoneProgress> copy =
                new HashMap<>(branchProgress);
        if (next.equals(BranchMilestoneProgress.initial())) {
            copy.remove(specialization.id());
        } else {
            copy.put(specialization.id(), next);
        }
        return new PlayerClassMilestoneState(
                schemaVersion,
                Map.copyOf(copy)
        );
    }
}
