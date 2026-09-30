package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerClassMilestoneService {
    private PlayerClassMilestoneService() {
    }

    public static PlayerClassMilestoneState state(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                PlayerClassMilestoneAttachments.CLASS_MILESTONES,
                PlayerClassMilestoneState.initial()
        );
    }

    public static MilestoneResult completeRank20Technique(
            ServerPlayer player,
            ClassSpecialization specialization
    ) {
        return completeMilestone(
                player,
                specialization,
                20,
                MilestoneKind.RANK_20_TECHNIQUE,
                null
        );
    }

    public static MilestoneResult completeRank32Doctrine(
            ServerPlayer player,
            ClassSpecialization specialization,
            ClassDoctrine initialDoctrine
    ) {
        Objects.requireNonNull(initialDoctrine, "initialDoctrine");
        if (initialDoctrine.specialization() != specialization) {
            return MilestoneResult.rejected(
                    MilestoneStatus.WRONG_DOCTRINE,
                    MilestoneKind.RANK_32_DOCTRINE,
                    specialization,
                    currentRank(player, specialization)
            );
        }
        return completeMilestone(
                player,
                specialization,
                32,
                MilestoneKind.RANK_32_DOCTRINE,
                initialDoctrine
        );
    }

    public static MilestoneResult completeRank44Ascendant(
            ServerPlayer player,
            ClassSpecialization specialization
    ) {
        return completeMilestone(
                player,
                specialization,
                44,
                MilestoneKind.RANK_44_ASCENDANT,
                null
        );
    }

    public static DoctrineResult selectDoctrine(
            ServerPlayer player,
            ClassDoctrine doctrine,
            boolean outOfCombat
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(doctrine, "doctrine");
        ClassSpecialization specialization =
                doctrine.specialization();

        if (!outOfCombat) {
            return DoctrineResult.rejected(
                    DoctrineStatus.COMBAT_LOCKED,
                    doctrine
            );
        }
        if (!isActiveBranch(player, specialization)) {
            return DoctrineResult.rejected(
                    DoctrineStatus.WRONG_ACTIVE_BRANCH,
                    doctrine
            );
        }

        BranchMilestoneProgress progress =
                state(player).progress(specialization);
        if (!progress.rank32DoctrineComplete()) {
            return DoctrineResult.rejected(
                    DoctrineStatus.RANK_32_MILESTONE_REQUIRED,
                    doctrine
            );
        }

        BranchMilestoneProgress next =
                progress.selectDoctrine(doctrine);
        if (next.equals(progress)) {
            return new DoctrineResult(
                    DoctrineStatus.ALREADY_ACTIVE,
                    doctrine
            );
        }
        replace(
                player,
                state(player).update(specialization, next)
        );
        return new DoctrineResult(
                DoctrineStatus.SWITCHED,
                doctrine
        );
    }

    public static boolean rank20TechniqueComplete(
            ServerPlayer player,
            ClassSpecialization specialization
    ) {
        return state(player).progress(specialization)
                .rank20TechniqueComplete();
    }

    public static boolean rank32DoctrineComplete(
            ServerPlayer player,
            ClassSpecialization specialization
    ) {
        return state(player).progress(specialization)
                .rank32DoctrineComplete();
    }

    public static boolean rank44AscendantComplete(
            ServerPlayer player,
            ClassSpecialization specialization
    ) {
        return state(player).progress(specialization)
                .rank44AscendantComplete();
    }

    private static MilestoneResult completeMilestone(
            ServerPlayer player,
            ClassSpecialization specialization,
            int requiredRank,
            MilestoneKind kind,
            ClassDoctrine initialDoctrine
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(specialization, "specialization");
        Objects.requireNonNull(kind, "kind");

        int rank = currentRank(player, specialization);
        if (rank < requiredRank) {
            return MilestoneResult.rejected(
                    MilestoneStatus.RANK_REQUIRED,
                    kind,
                    specialization,
                    rank
            );
        }
        if (!isActiveBranch(player, specialization)) {
            return MilestoneResult.rejected(
                    MilestoneStatus.WRONG_ACTIVE_BRANCH,
                    kind,
                    specialization,
                    rank
            );
        }

        PlayerClassMilestoneState state = state(player);
        BranchMilestoneProgress progress =
                state.progress(specialization);
        BranchMilestoneProgress next;
        try {
            next = switch (kind) {
                case RANK_20_TECHNIQUE ->
                        progress.completeRank20Technique();
                case RANK_32_DOCTRINE ->
                        progress.completeRank32Doctrine(
                                Objects.requireNonNull(
                                        initialDoctrine,
                                        "initialDoctrine"
                                )
                        );
                case RANK_44_ASCENDANT ->
                        progress.completeRank44Ascendant();
            };
        } catch (IllegalStateException exception) {
            return MilestoneResult.rejected(
                    MilestoneStatus.PREVIOUS_MILESTONE_REQUIRED,
                    kind,
                    specialization,
                    rank
            );
        }

        if (next.equals(progress)) {
            return new MilestoneResult(
                    MilestoneStatus.ALREADY_COMPLETE,
                    kind,
                    specialization,
                    rank
            );
        }

        replace(
                player,
                state.update(specialization, next)
        );
        return new MilestoneResult(
                MilestoneStatus.COMPLETED,
                kind,
                specialization,
                rank
        );
    }

    private static int currentRank(
            ServerPlayer player,
            ClassSpecialization specialization
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(specialization, "specialization");
        return PlayerProgressionService.state(player)
                .classProgress(specialization.rootClass())
                .rank();
    }

    private static boolean isActiveBranch(
            ServerPlayer player,
            ClassSpecialization specialization
    ) {
        var progression = PlayerProgressionService.state(player);
        if (!progression.activeClass().equals(
                Optional.of(specialization.rootClass())
        )) {
            return false;
        }
        return PlayerClassAdvancementService.state(player)
                .rootState(specialization.rootClass())
                .active()
                .equals(Optional.of(specialization));
    }

    private static void replace(
            ServerPlayer player,
            PlayerClassMilestoneState next
    ) {
        if (!state(player).equals(next)) {
            player.setAttached(
                    PlayerClassMilestoneAttachments.CLASS_MILESTONES,
                    next
            );
        }
    }

    public enum MilestoneKind {
        RANK_20_TECHNIQUE,
        RANK_32_DOCTRINE,
        RANK_44_ASCENDANT
    }

    public enum MilestoneStatus {
        COMPLETED,
        ALREADY_COMPLETE,
        RANK_REQUIRED,
        WRONG_ACTIVE_BRANCH,
        PREVIOUS_MILESTONE_REQUIRED,
        WRONG_DOCTRINE
    }

    public record MilestoneResult(
            MilestoneStatus status,
            MilestoneKind kind,
            ClassSpecialization specialization,
            int classRank
    ) {
        public MilestoneResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(
                    specialization,
                    "specialization"
            );
            if (classRank < 1
                    || classRank
                            > ProjectProgressionRules.MAX_CLASS_RANK) {
                throw new IllegalArgumentException(
                        "Milestone result Class Rank is invalid."
                );
            }
        }

        public static MilestoneResult rejected(
                MilestoneStatus status,
                MilestoneKind kind,
                ClassSpecialization specialization,
                int classRank
        ) {
            if (status == MilestoneStatus.COMPLETED) {
                throw new IllegalArgumentException(
                        "Use completed result directly."
                );
            }
            return new MilestoneResult(
                    status,
                    kind,
                    specialization,
                    classRank
            );
        }

        public boolean completedNow() {
            return status == MilestoneStatus.COMPLETED;
        }
    }

    public enum DoctrineStatus {
        SWITCHED,
        ALREADY_ACTIVE,
        COMBAT_LOCKED,
        WRONG_ACTIVE_BRANCH,
        RANK_32_MILESTONE_REQUIRED
    }

    public record DoctrineResult(
            DoctrineStatus status,
            ClassDoctrine doctrine
    ) {
        public DoctrineResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(doctrine, "doctrine");
        }

        public static DoctrineResult rejected(
                DoctrineStatus status,
                ClassDoctrine doctrine
        ) {
            if (status == DoctrineStatus.SWITCHED
                    || status == DoctrineStatus.ALREADY_ACTIVE) {
                throw new IllegalArgumentException(
                        "Rejected doctrine result cannot be a success state."
                );
            }
            return new DoctrineResult(status, doctrine);
        }

        public boolean switched() {
            return status == DoctrineStatus.SWITCHED;
        }
    }
}
