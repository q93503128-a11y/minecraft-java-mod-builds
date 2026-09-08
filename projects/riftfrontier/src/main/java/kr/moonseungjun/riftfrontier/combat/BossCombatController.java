package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-thread-owned boss combat lifecycle over one validated BossProfile.
 *
 * <p>This controller owns phase transitions, attack selection, interruption and completion accounting.
 * It intentionally delegates every attack's telegraph/active/recovery cadence to AttackStateMachine.</p>
 */
public final class BossCombatController {
    private final ContentId bossId;
    private final CoreDefinition.BossProfile profile;
    private final CombatRuntimeCatalog catalog;
    private final BossAttackSelectionPolicy selectionPolicy;
    private final List<ContentId> candidateAttacks;
    private final AttackStateMachine attackState = new AttackStateMachine();

    private int phase = 1;
    private long completedAttackCount;
    private Optional<ContentId> lastAttack = Optional.empty();

    public BossCombatController(
        CombatRuntimeCatalog catalog,
        ContentId bossId,
        BossAttackSelectionPolicy selectionPolicy
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.bossId = Objects.requireNonNull(bossId, "bossId");
        this.selectionPolicy = Objects.requireNonNull(selectionPolicy, "selectionPolicy");
        this.profile = catalog.requireBossProfile(bossId);
        this.candidateAttacks = profile.attackPatterns().stream().sorted().toList();
        if (candidateAttacks.isEmpty()) {
            throw new IllegalStateException("Boss profile has no attack patterns: " + bossId);
        }
        candidateAttacks.forEach(catalog::requireAttackPattern);
    }

    public int phase() {
        return phase;
    }

    public int phaseCount() {
        return profile.phaseCount();
    }

    public boolean attackExecuting() {
        return attackState.isExecuting();
    }

    public long completedAttackCount() {
        return completedAttackCount;
    }

    public Optional<ContentId> lastAttack() {
        return lastAttack;
    }

    public AttackExecution.Snapshot beginNextAttack(long gameTick) {
        if (attackState.isExecuting()) {
            throw new IllegalStateException("Cannot select a boss attack while another attack is executing");
        }

        BossAttackSelectionPolicy.SelectionContext context = new BossAttackSelectionPolicy.SelectionContext(
            bossId,
            phase,
            profile.phaseCount(),
            candidateAttacks,
            completedAttackCount,
            lastAttack
        );
        ContentId selected = Objects.requireNonNull(selectionPolicy.select(context), "selection policy returned null");
        if (!candidateAttacks.contains(selected)) {
            throw new IllegalStateException("Selection policy returned attack outside boss profile: " + selected);
        }
        return attackState.begin(catalog.requireAttackPattern(selected), gameTick);
    }

    public Step advance(long gameTick) {
        AttackStateMachine.Step attackStep = attackState.advance(gameTick);
        if (attackStep.finished()) {
            ContentId completed = attackStep.snapshot().orElseThrow().patternId();
            lastAttack = Optional.of(completed);
            completedAttackCount = Math.addExact(completedAttackCount, 1L);
        }
        return new Step(phase, completedAttackCount, lastAttack, attackStep);
    }

    /**
     * Explicit server-owned phase transition. Any executing attack is interrupted first so a phase
     * transition can never leave an old-phase hit window alive.
     */
    public PhaseTransition transitionToPhase(int newPhase) {
        if (newPhase < 1 || newPhase > profile.phaseCount()) {
            throw new IllegalArgumentException("newPhase must be within 1.." + profile.phaseCount());
        }
        if (newPhase == phase) {
            throw new IllegalArgumentException("newPhase must differ from current phase");
        }

        int previousPhase = phase;
        Optional<ContentId> interrupted = attackState.cancel().map(AttackExecution::patternId);
        phase = newPhase;
        return new PhaseTransition(previousPhase, newPhase, interrupted);
    }

    /** Explicit stun/death/despawn interruption boundary without changing boss phase. */
    public Optional<ContentId> cancelAttack() {
        return attackState.cancel().map(AttackExecution::patternId);
    }

    public record Step(
        int phase,
        long completedAttackCount,
        Optional<ContentId> lastAttack,
        AttackStateMachine.Step attackStep
    ) {
        public Step {
            if (phase <= 0 || completedAttackCount < 0) {
                throw new IllegalArgumentException("invalid boss combat step state");
            }
            lastAttack = Objects.requireNonNull(lastAttack, "lastAttack");
            Objects.requireNonNull(attackStep, "attackStep");
        }
    }

    public record PhaseTransition(int previousPhase, int newPhase, Optional<ContentId> interruptedAttack) {
        public PhaseTransition {
            if (previousPhase <= 0 || newPhase <= 0 || previousPhase == newPhase) {
                throw new IllegalArgumentException("phase transition must change between positive phases");
            }
            interruptedAttack = Objects.requireNonNull(interruptedAttack, "interruptedAttack");
        }
    }
}
