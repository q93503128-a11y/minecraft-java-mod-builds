package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules;

/** Persistent Rank / current-Rank Class XP for one root class. */
public record RootClassProgress(
        int rank,
        long classXp
) {
    public static final Codec<RootClassProgress> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(1, ProjectProgressionRules.MAX_CLASS_RANK)
                            .fieldOf("rank")
                            .forGetter(RootClassProgress::rank),
                    Codec.LONG.fieldOf("class_xp").forGetter(RootClassProgress::classXp)
            ).apply(instance, RootClassProgress::new)
    );

    public RootClassProgress {
        if (rank < 1 || rank > ProjectProgressionRules.MAX_CLASS_RANK) {
            throw new IllegalArgumentException("Class Rank must be inside [1, 50].");
        }
        if (classXp < 0L) {
            throw new IllegalArgumentException("Class XP cannot be negative.");
        }
        if (rank == ProjectProgressionRules.MAX_CLASS_RANK) {
            if (classXp != 0L) {
                throw new IllegalArgumentException("Rank 50 cannot store overflow Class XP.");
            }
        } else if (classXp >= ProjectProgressionRules.classXpToNext(rank)) {
            throw new IllegalArgumentException(
                    "Current-Rank Class XP must remain below the next-rank requirement."
            );
        }
    }

    public static RootClassProgress initial() {
        return new RootClassProgress(1, 0L);
    }

    public RootClassProgress grantXp(long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Class XP grant cannot be negative.");
        }
        if (amount == 0L || rank == ProjectProgressionRules.MAX_CLASS_RANK) {
            return this;
        }

        int nextRank = rank;
        long nextXp = classXp;
        long remaining = amount;

        while (remaining > 0L && nextRank < ProjectProgressionRules.MAX_CLASS_RANK) {
            long requirement = ProjectProgressionRules.classXpToNext(nextRank);
            long needed = requirement - nextXp;
            if (remaining < needed) {
                nextXp = Math.addExact(nextXp, remaining);
                remaining = 0L;
            } else {
                remaining -= needed;
                nextRank++;
                nextXp = 0L;
            }
        }

        if (nextRank == ProjectProgressionRules.MAX_CLASS_RANK) {
            nextXp = 0L;
        }
        if (nextRank == rank && nextXp == classXp) {
            return this;
        }
        return new RootClassProgress(nextRank, nextXp);
    }
}
