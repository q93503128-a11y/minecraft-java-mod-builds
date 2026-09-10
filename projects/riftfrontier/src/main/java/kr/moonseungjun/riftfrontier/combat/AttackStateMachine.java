package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * Server-thread-owned attack state machine. It owns only execution lifecycle; phase timing stays in
 * {@link CoreDefinition.AttackPattern} through {@link AttackTimeline}.
 *
 * <p>While an execution is active, sampled server game ticks must be monotonic. A rewind is treated
 * as an authority discontinuity: the execution is cancelled before an exception is raised so a
 * caller cannot continue from a phase that was already observed at a later tick.</p>
 */
public final class AttackStateMachine {
    private AttackExecution current;
    private AttackTimeline.Phase lastPhase;
    private long lastObservedGameTick = -1L;

    public AttackExecution.Snapshot begin(CoreDefinition.AttackPattern pattern, long gameTick) {
        Objects.requireNonNull(pattern, "pattern");
        if (gameTick < 0) {
            throw new IllegalArgumentException("gameTick must be >= 0");
        }
        if (current != null) {
            throw new IllegalStateException("Cannot begin a new attack while another attack is executing");
        }
        current = AttackExecution.start(pattern, gameTick);
        AttackExecution.Snapshot snapshot = current.sample(gameTick);
        lastPhase = snapshot.presentationPhase();
        lastObservedGameTick = gameTick;
        return snapshot;
    }

    public Step advance(long gameTick) {
        if (current == null) {
            return Step.idle();
        }
        requireMonotonicTick(gameTick);

        AttackExecution.Snapshot snapshot = current.sample(gameTick);
        AttackTimeline.Phase phase = snapshot.presentationPhase();
        boolean phaseChanged = phase != lastPhase;
        boolean finished = phase == AttackTimeline.Phase.COMPLETE;
        lastPhase = phase;
        lastObservedGameTick = gameTick;

        Step result = new Step(Optional.of(snapshot), phaseChanged, finished);
        if (finished) {
            clearExecutionState();
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
        clearExecutionState();
        return Optional.ofNullable(previous);
    }

    private void requireMonotonicTick(long gameTick) {
        if (gameTick < lastObservedGameTick) {
            long previousTick = lastObservedGameTick;
            clearExecutionState();
            throw new IllegalArgumentException(
                "gameTick cannot move backwards while an attack is executing: " + gameTick + " < " + previousTick
            );
        }
    }

    private void clearExecutionState() {
        current = null;
        lastPhase = null;
        lastObservedGameTick = -1L;
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
