package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic policy boundary for server-owned boss attack selection.
 *
 * <p>Policies choose only from the validated candidate set supplied by {@link BossCombatController}.
 * Timing, hit windows and presentation cadence remain owned by AttackPattern/AttackStateMachine.</p>
 */
@FunctionalInterface
public interface BossAttackSelectionPolicy {
    ContentId select(SelectionContext context);

    /** Stable baseline policy over the controller-supplied full boss pool. */
    static BossAttackSelectionPolicy deterministicRoundRobin() {
        return context -> selectRoundRobin(context, context.candidateAttacks());
    }

    /**
     * Phase-scoped policy backed only by a {@link ValidatedBossCombatSemantics} capability.
     * No unvalidated JSON/profile can affect server attack selection.
     */
    static BossAttackSelectionPolicy authoredPhases(ValidatedBossCombatSemantics semantics) {
        Objects.requireNonNull(semantics, "semantics");
        return context -> {
            if (!context.bossId().equals(semantics.bossProfile())) {
                throw new IllegalStateException("semantic profile targets a different boss: " + semantics.bossProfile());
            }
            List<ContentId> phaseCandidates = semantics.candidateAttacks(context.phase());
            for (ContentId candidate : phaseCandidates) {
                if (!context.candidateAttacks().contains(candidate)) {
                    throw new IllegalStateException("validated semantic attack is outside controller candidate set: " + candidate);
                }
            }
            return selectRoundRobin(context, phaseCandidates);
        };
    }

    private static ContentId selectRoundRobin(SelectionContext context, List<ContentId> candidates) {
        if (candidates.isEmpty()) throw new IllegalStateException("Boss attack selection requires at least one candidate");
        int index = (int) Math.floorMod(context.completedAttackCount(), (long) candidates.size());
        return candidates.get(index);
    }

    record SelectionContext(
        ContentId bossId,
        int phase,
        int phaseCount,
        List<ContentId> candidateAttacks,
        long completedAttackCount,
        Optional<ContentId> lastAttack
    ) {
        public SelectionContext {
            Objects.requireNonNull(bossId, "bossId");
            candidateAttacks = List.copyOf(Objects.requireNonNull(candidateAttacks, "candidateAttacks"));
            lastAttack = Objects.requireNonNull(lastAttack, "lastAttack");
            if (phaseCount <= 0 || phase < 1 || phase > phaseCount) {
                throw new IllegalArgumentException("phase must be within 1..phaseCount");
            }
            if (completedAttackCount < 0) throw new IllegalArgumentException("completedAttackCount must be >= 0");
        }
    }
}
