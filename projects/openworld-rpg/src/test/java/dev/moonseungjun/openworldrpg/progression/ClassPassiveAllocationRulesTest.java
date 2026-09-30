package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import org.junit.jupiter.api.Test;

class ClassPassiveAllocationRulesTest {
    private static final ClassPassiveCatalog CATALOG = ClassPassiveCatalog.bundled();

    @Test
    void pointCurveIsExactlyTwentyFiveRankPlusFirstFiveInsights() {
        assertEquals(0, ClassPassiveAllocationRules.availablePoints(1, 0));
        assertEquals(1, ClassPassiveAllocationRules.availablePoints(2, 0));
        assertEquals(25, ClassPassiveAllocationRules.availablePoints(50, 0));
        assertEquals(30, ClassPassiveAllocationRules.availablePoints(50, 5));
        assertEquals(30, ClassPassiveAllocationRules.availablePoints(50, 8));
    }

    @Test
    void branchOneRequiresFiveRootPointsAndEnoughTotalBudget() {
        var advancement = PlayerClassAdvancementState.initial()
                .unlockSpecialization(ClassSpecialization.HUNTER_RANGER);
        var branchNode = CATALOG.branchNodes(ClassSpecialization.HUNTER_RANGER)
                .stream()
                .filter(node -> node.tier() == ClassPassiveTier.I)
                .findFirst()
                .orElseThrow();

        var empty = PlayerPassiveProgressState.initial();
        assertEquals(
                ClassPassiveAllocationRules.Decision.BRANCH_I_LOCKED,
                ClassPassiveAllocationRules.evaluateAllocation(
                        CATALOG,
                        empty,
                        advancement,
                        PlayerClassMilestoneState.initial(),
                        RootClass.HUNTER,
                        10,
                        branchNode.id()
                )
        );

        var withRoot = allocateRootPoints(empty, RootClass.HUNTER, 5);
        assertEquals(
                ClassPassiveAllocationRules.Decision.ALLOWED,
                ClassPassiveAllocationRules.evaluateAllocation(
                        CATALOG,
                        withRoot,
                        advancement,
                        PlayerClassMilestoneState.initial(),
                        RootClass.HUNTER,
                        12,
                        branchNode.id()
                )
        );
    }

    @Test
    void deeperTiersUseMilestoneStateAndPriorBranchInvestment() {
        var specialization = ClassSpecialization.MAGE_ELEMENTALIST;
        var advancement = PlayerClassAdvancementState.initial().unlockSpecialization(specialization);
        var state = allocateRootPoints(
                PlayerPassiveProgressState.initial(),
                RootClass.MAGE,
                5
        );
        state = allocateBranchPoints(state, specialization, ClassPassiveTier.I, 4);

        var tierTwo = CATALOG.branchNodes(specialization)
                .stream()
                .filter(node -> node.tier() == ClassPassiveTier.II)
                .findFirst()
                .orElseThrow();

        assertEquals(
                ClassPassiveAllocationRules.Decision.BRANCH_II_LOCKED,
                ClassPassiveAllocationRules.evaluateAllocation(
                        CATALOG,
                        state,
                        advancement,
                        PlayerClassMilestoneState.initial(),
                        RootClass.MAGE,
                        20,
                        tierTwo.id()
                )
        );

        var milestones = PlayerClassMilestoneState.initial().update(
                specialization,
                BranchMilestoneProgress.initial().completeRank20Technique()
        );
        assertEquals(
                ClassPassiveAllocationRules.Decision.ALLOWED,
                ClassPassiveAllocationRules.evaluateAllocation(
                        CATALOG,
                        state,
                        advancement,
                        milestones,
                        RootClass.MAGE,
                        20,
                        tierTwo.id()
                )
        );
    }

    private static PlayerPassiveProgressState allocateRootPoints(
            PlayerPassiveProgressState state,
            RootClass rootClass,
            int count
    ) {
        int remaining = count;
        for (ClassPassiveNodeSpec node : CATALOG.rootNodes(rootClass)) {
            for (int rank = 1; rank <= node.maxRank() && remaining > 0; rank++) {
                state = state.withAllocationRank(node.id(), rank);
                remaining--;
            }
            if (remaining == 0) break;
        }
        return state;
    }

    private static PlayerPassiveProgressState allocateBranchPoints(
            PlayerPassiveProgressState state,
            ClassSpecialization specialization,
            ClassPassiveTier tier,
            int count
    ) {
        int remaining = count;
        for (ClassPassiveNodeSpec node : CATALOG.branchNodes(specialization)) {
            if (node.tier() != tier) continue;
            for (int rank = 1; rank <= node.maxRank() && remaining > 0; rank++) {
                state = state.withAllocationRank(node.id(), rank);
                remaining--;
            }
            if (remaining == 0) break;
        }
        return state;
    }
}
