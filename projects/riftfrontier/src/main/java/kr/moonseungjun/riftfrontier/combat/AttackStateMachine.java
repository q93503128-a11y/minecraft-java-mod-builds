package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * Server-thread-owned attack state machine. It owns only execution lifecycle; phase timing stays in
 * {@link CoreDefinition.AttackPattern} through {@link AttackTimeline}.
 */
public final class AttackStateMachine {
    private AttackExecution current;
    private AttackTimeline.Phase lastPhase;

    public AttackExecution.Snapshot begin(CoreDefinition.AttackPattern pattern, long gameTick) {
        Objects.requireNonNull(pattern, "pattern");
        if (current != null) {
            throw new IllegalStateException("Cannot begin a new attack while another attack is executing");
        }
        current = AttackExecution.start(pattern, gameTick);
        AttackExecution.Snapshot snapshot = current.sample(gameTick);
        lastPhase = snapshot.presentationPhase();
        return snapshot;
    }

    public Step advance(long gameTick) {
        if (current == null) {
            return Step.idle();
        }

        AttackExecution.Snapshot snapshot = current.sample(gameTick);
        AttackTimeline.Phase phase = snapshot.presentationPhase();
        boolean phaseChanged = phase != lastPhase;
        boolean finished = phase == AttackTimeline.Phase.COMPLETE;
        lastPhase = phase;

        Step result = new Step(Optional.of(snapshot), phaseChanged, finished);
        if (finished) {
            current = null;
            lastPhase = null;
        }
        return result;
    }

    public boolean isExecuting() {
        return current != null;
    }

    public Optional<AttackExecution> currentExecution() {
        return Optional.ofNullable(current);
    }

    /** Explicit interruption boundary for stun/death/despawn policy. */
    public Optional<AttackExecution> cancel() {
        AttackExecution previous = current;
        current = null;
        lastPhase = null;
        return Optional.ofNullable(previous);
    }

    public record Step(Optional<AttackExecution.Snapshot> snapshot, boolean phaseChanged, boolean finished) {
        public Step {
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            if (snapshot.isEmpty() && (phaseChanged || finished)) {
                throw new IllegalArgumentException("idle step cannot change or finish a phase");
            }
            if (finished && snapshot.orElseThrow().presentationPhase() != AttackTimeline.Phase.COMPLETE) {
                throw new IllegalArgumentException("finished step must expose COMPLETE phase");
            }
        }

        public static Step idle() {
            return new Step(Optional.empty(), false, false);
        }

        public boolean hitWindowOpen() {
            return snapshot.map(AttackExecution.Snapshot::mayApplyHit).orElse(false);
        }
    }
}
