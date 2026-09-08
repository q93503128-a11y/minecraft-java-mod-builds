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

    /**
     * Stable baseline policy: cycle through sorted boss attack IDs by completed execution count.
     * Phase is deliberately present in the context so later authored phase/weight/cooldown policies
     * can replace this policy without changing the controller lifecycle contract.
     */
    static BossAttackSelectionPolicy deterministicRoundRobin() {
        return context -> {
            List<ContentId> candidates = context.candidateAttacks();
            if (candidates.isEmpty()) {
                throw new IllegalStateException("Boss attack selection requires at least one candidate");
            }
            int index = (int) Math.floorMod(context.completedAttackCount(), (long) candidates.size());
            return candidates.get(index);
        };
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
            if (completedAttackCount < 0) {
                throw new IllegalArgumentException("completedAttackCount must be >= 0");
            }
        }
    }
}
