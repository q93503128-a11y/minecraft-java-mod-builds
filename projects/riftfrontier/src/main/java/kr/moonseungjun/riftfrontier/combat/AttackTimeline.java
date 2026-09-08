package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.Objects;

/**
 * Runtime view of one data-authored attack cadence.
 *
 * <p>The content definition remains the only source of timing truth. Animation, hit-volume and
 * feedback adapters sample this timeline instead of copying telegraph/active/recovery constants.</p>
 */
public final class AttackTimeline {
    private final CoreDefinition.AttackPattern pattern;
    private final int activeStart;
    private final int recoveryStart;
    private final int completeAt;

    public AttackTimeline(CoreDefinition.AttackPattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        this.activeStart = pattern.telegraphTicks();
        this.recoveryStart = Math.addExact(activeStart, pattern.activeTicks());
        this.completeAt = Math.addExact(recoveryStart, pattern.recoveryTicks());
    }

    public CoreDefinition.AttackPattern pattern() {
        return pattern;
    }

    public int totalTicks() {
        return completeAt;
    }

    public Sample sample(long elapsedTicks) {
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks must be >= 0");
        }

        if (elapsedTicks < activeStart) {
            return sample(Phase.TELEGRAPH, elapsedTicks, 0, activeStart);
        }
        if (elapsedTicks < recoveryStart) {
            return sample(Phase.ACTIVE, elapsedTicks, activeStart, recoveryStart);
        }
        if (elapsedTicks < completeAt) {
            return sample(Phase.RECOVERY, elapsedTicks, recoveryStart, completeAt);
        }
        return new Sample(Phase.COMPLETE, elapsedTicks, 0, 0, false, 1.0);
    }

    private static Sample sample(Phase phase, long elapsedTicks, int phaseStart, int phaseEnd) {
        long phaseTick = elapsedTicks - phaseStart;
        long phaseDuration = phaseEnd - phaseStart;
        long remaining = phaseDuration - phaseTick;
        double progress = phaseDuration == 0 ? 1.0 : Math.min(1.0, Math.max(0.0, (double) phaseTick / phaseDuration));
        return new Sample(phase, elapsedTicks, phaseTick, remaining, phase == Phase.ACTIVE, progress);
    }

    public enum Phase {
        TELEGRAPH,
        ACTIVE,
        RECOVERY,
        COMPLETE
    }

    /** Immutable state consumed by presentation and server-authoritative hit-volume adapters. */
    public record Sample(
        Phase phase,
        long elapsedTicks,
        long phaseTick,
        long phaseTicksRemaining,
        boolean hitWindowOpen,
        double phaseProgress
    ) {
        public Sample {
            Objects.requireNonNull(phase, "phase");
            if (elapsedTicks < 0 || phaseTick < 0 || phaseTicksRemaining < 0) {
                throw new IllegalArgumentException("attack sample ticks must be >= 0");
            }
            if (!Double.isFinite(phaseProgress) || phaseProgress < 0.0 || phaseProgress > 1.0) {
                throw new IllegalArgumentException("phaseProgress must be finite and between 0 and 1");
            }
            if (hitWindowOpen != (phase == Phase.ACTIVE)) {
                throw new IllegalArgumentException("hitWindowOpen must match ACTIVE phase");
            }
        }
    }
}
