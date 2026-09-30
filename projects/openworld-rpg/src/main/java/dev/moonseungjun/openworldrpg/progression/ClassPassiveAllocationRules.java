package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.Optional;

public final class ClassPassiveAllocationRules {
    public static final int MAX_INSIGHT_POINTS = 5;
    public static final int MAX_PASSIVE_POINTS = 30;

    private ClassPassiveAllocationRules() {
    }

    public static int availablePoints(int classRank, int completedInsights) {
        if (classRank < 1 || classRank > ProjectProgressionRules.MAX_CLASS_RANK
                || completedInsights < 0 || completedInsights > 8) {
            throw new IllegalArgumentException("Invalid passive-point progression inputs.");
        }
        return Math.min(
                MAX_PASSIVE_POINTS,
                classRank / 2 + Math.min(MAX_INSIGHT_POINTS, completedInsights)
        );
    }

    public static Decision evaluateAllocation(
            ClassPassiveCatalog catalog,
            PlayerPassiveProgressState passiveState,
            PlayerClassAdvancementState advancementState,
            PlayerClassMilestoneState milestoneState,
            RootClass activeRoot,
            int classRank,
            String nodeId
    ) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(passiveState, "passiveState");
        Objects.requireNonNull(advancementState, "advancementState");
        Objects.requireNonNull(milestoneState, "milestoneState");
        Objects.requireNonNull(activeRoot, "activeRoot");

        ClassPassiveNodeSpec node = catalog.require(nodeId);
        if (node.rootClass() != activeRoot) {
            return Decision.WRONG_ACTIVE_ROOT;
        }

        Optional<ClassSpecialization> activeBranch = advancementState.rootState(activeRoot).active();
        if (node.specialization().isPresent() && !node.specialization().equals(activeBranch)) {
            return Decision.BRANCH_INACTIVE;
        }

        int current = passiveState.allocationRank(node.id());
        if (current >= node.maxRank()) {
            return Decision.MAX_RANK_REACHED;
        }

        int rootSpent = spentRoot(catalog, passiveState, activeRoot);
        int activeBranchSpent = activeBranch
                .map(branch -> spentBranch(catalog, passiveState, branch))
                .orElse(0);

        if (node.specialization().isPresent()) {
            ClassSpecialization branch = node.specialization().orElseThrow();
            if (classRank < 10
                    || !advancementState.rootState(activeRoot).isUnlocked(branch)
                    || rootSpent < 5) {
                return Decision.BRANCH_I_LOCKED;
            }
            BranchMilestoneProgress milestones = milestoneState.progress(branch);
            switch (node.tier()) {
                case ROOT -> throw new IllegalStateException("Branch node cannot use ROOT tier.");
                case I -> {
                }
                case II -> {
                    if (classRank < 20 || !milestones.rank20TechniqueComplete() || activeBranchSpent < 4) {
                        return Decision.BRANCH_II_LOCKED;
                    }
                }
                case III -> {
                    if (classRank < 32 || !milestones.rank32DoctrineComplete() || activeBranchSpent < 9) {
                        return Decision.BRANCH_III_LOCKED;
                    }
                }
                case CAPSTONE -> {
                    if (classRank < 44 || !milestones.rank44AscendantComplete() || activeBranchSpent < 14) {
                        return Decision.CAPSTONE_LOCKED;
                    }
                }
            }
        }

        int rootAfter = rootSpent + (node.rootNode() ? 1 : 0);
        int maxBranchAfter = 0;
        for (ClassSpecialization specialization : ClassSpecialization.forRoot(activeRoot)) {
            int spent = spentBranch(catalog, passiveState, specialization);
            if (node.specialization().equals(Optional.of(specialization))) {
                spent++;
            }
            maxBranchAfter = Math.max(maxBranchAfter, spent);
        }

        int available = availablePoints(
                classRank,
                passiveState.completedInsightCount(activeRoot)
        );
        if (rootAfter + maxBranchAfter > available) {
            return Decision.INSUFFICIENT_POINTS;
        }
        return Decision.ALLOWED;
    }

    public static int spentRoot(
            ClassPassiveCatalog catalog,
            PlayerPassiveProgressState state,
            RootClass rootClass
    ) {
        int total = 0;
        for (ClassPassiveNodeSpec node : catalog.rootNodes(rootClass)) {
            total += state.allocationRank(node.id());
        }
        return total;
    }

    public static int spentBranch(
            ClassPassiveCatalog catalog,
            PlayerPassiveProgressState state,
            ClassSpecialization specialization
    ) {
        int total = 0;
        for (ClassPassiveNodeSpec node : catalog.branchNodes(specialization)) {
            total += state.allocationRank(node.id());
        }
        return total;
    }

    public enum Decision {
        ALLOWED,
        WRONG_ACTIVE_ROOT,
        BRANCH_INACTIVE,
        MAX_RANK_REACHED,
        BRANCH_I_LOCKED,
        BRANCH_II_LOCKED,
        BRANCH_III_LOCKED,
        CAPSTONE_LOCKED,
        INSUFFICIENT_POINTS
    }
}
