package dev.moonseungjun.openworldrpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

public record BranchMilestoneProgress(
        boolean rank20TechniqueComplete,
        boolean rank32DoctrineComplete,
        boolean rank44AscendantComplete,
        Optional<ClassDoctrine> activeDoctrine
) {
    public static final Codec<BranchMilestoneProgress> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("rank20_technique_complete", false)
                            .forGetter(BranchMilestoneProgress::rank20TechniqueComplete),
                    Codec.BOOL.optionalFieldOf("rank32_doctrine_complete", false)
                            .forGetter(BranchMilestoneProgress::rank32DoctrineComplete),
                    Codec.BOOL.optionalFieldOf("rank44_ascendant_complete", false)
                            .forGetter(BranchMilestoneProgress::rank44AscendantComplete),
                    ClassDoctrine.CODEC.optionalFieldOf("active_doctrine")
                            .forGetter(BranchMilestoneProgress::activeDoctrine)
            ).apply(instance, BranchMilestoneProgress::new));

    public BranchMilestoneProgress {
        activeDoctrine = Objects.requireNonNull(
                activeDoctrine,
                "activeDoctrine"
        );
        if (rank32DoctrineComplete && !rank20TechniqueComplete) {
            throw new IllegalArgumentException(
                    "Rank-32 doctrine milestone requires Rank-20 technique completion."
            );
        }
        if (rank44AscendantComplete && !rank32DoctrineComplete) {
            throw new IllegalArgumentException(
                    "Rank-44 ascendant milestone requires Rank-32 doctrine completion."
            );
        }
        if (activeDoctrine.isPresent() && !rank32DoctrineComplete) {
            throw new IllegalArgumentException(
                    "An active doctrine requires the Rank-32 doctrine milestone."
            );
        }
    }

    public static BranchMilestoneProgress initial() {
        return new BranchMilestoneProgress(
                false,
                false,
                false,
                Optional.empty()
        );
    }

    public BranchMilestoneProgress completeRank20Technique() {
        if (rank20TechniqueComplete) {
            return this;
        }
        return new BranchMilestoneProgress(
                true,
                rank32DoctrineComplete,
                rank44AscendantComplete,
                activeDoctrine
        );
    }

    public BranchMilestoneProgress completeRank32Doctrine(
            ClassDoctrine initialDoctrine
    ) {
        Objects.requireNonNull(initialDoctrine, "initialDoctrine");
        if (!rank20TechniqueComplete) {
            throw new IllegalStateException(
                    "Rank-20 technique milestone must be complete first."
            );
        }
        if (rank32DoctrineComplete) {
            if (activeDoctrine.equals(Optional.of(initialDoctrine))) {
                return this;
            }
            throw new IllegalStateException(
                    "Rank-32 doctrine milestone is already complete."
            );
        }
        return new BranchMilestoneProgress(
                true,
                true,
                rank44AscendantComplete,
                Optional.of(initialDoctrine)
        );
    }

    public BranchMilestoneProgress selectDoctrine(
            ClassDoctrine doctrine
    ) {
        Objects.requireNonNull(doctrine, "doctrine");
        if (!rank32DoctrineComplete) {
            throw new IllegalStateException(
                    "Cannot select a doctrine before the Rank-32 milestone."
            );
        }
        if (activeDoctrine.equals(Optional.of(doctrine))) {
            return this;
        }
        return new BranchMilestoneProgress(
                rank20TechniqueComplete,
                true,
                rank44AscendantComplete,
                Optional.of(doctrine)
        );
    }

    public BranchMilestoneProgress completeRank44Ascendant() {
        if (!rank32DoctrineComplete) {
            throw new IllegalStateException(
                    "Rank-32 doctrine milestone must be complete first."
            );
        }
        if (rank44AscendantComplete) {
            return this;
        }
        return new BranchMilestoneProgress(
                rank20TechniqueComplete,
                true,
                true,
                activeDoctrine
        );
    }
}
