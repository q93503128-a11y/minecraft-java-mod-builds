package io.github.q93503128.turnbound.world;

/** Pure lifecycle predicates for shared physical world vs per-player session ownership. */
final class SharedWorldSessionRules {
    private SharedWorldSessionRules() {}

    static boolean mayCrossSharedSeam(boolean physicalOpen, boolean playerEligible) {
        return physicalOpen && playerEligible;
    }
}
