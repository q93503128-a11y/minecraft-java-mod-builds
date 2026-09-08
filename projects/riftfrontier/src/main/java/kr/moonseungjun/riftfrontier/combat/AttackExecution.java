package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.Objects;

/**
 * Immutable server-authoritative execution handle for one attack instance.
 *
 * <p>The start tick is runtime state. All cadence, delivery, counterplay and presentation metadata
 * remains owned by the referenced {@link CoreDefinition.AttackPattern}.</p>
 */
public record AttackExecution(AttackTimeline timeline, long startedAtGameTick) {
    public AttackExecution {
        Objects.requireNonNull(timeline, "timeline");
        if (startedAtGameTick < 0) {
            throw new IllegalArgumentException("startedAtGameTick must be >= 0");
        }
    }

    public static AttackExecution start(CoreDefinition.AttackPattern pattern, long gameTick) {
        return new AttackExecution(new AttackTimeline(pattern), gameTick);
    }

    public ContentId patternId() {
        return timeline.pattern().id();
    }

    public Snapshot sample(long gameTick) {
        if (gameTick < startedAtGameTick) {
            throw new IllegalArgumentException("gameTick cannot precede attack start");
        }
        AttackTimeline.Sample sample = timeline.sample(gameTick - startedAtGameTick);
        CoreDefinition.AttackPattern pattern = timeline.pattern();
        return new Snapshot(
            pattern.id(),
            sample,
            pattern.delivery(),
            pattern.presentationCue(),
            pattern.counterplay()
        );
    }

    public long completesAtGameTick() {
        return Math.addExact(startedAtGameTick, timeline.totalTicks());
    }

    public record Snapshot(
        ContentId patternId,
        AttackTimeline.Sample timeline,
        String delivery,
        String presentationCue,
        java.util.Set<String> counterplay
    ) {
        public Snapshot {
            Objects.requireNonNull(patternId, "patternId");
            Objects.requireNonNull(timeline, "timeline");
            delivery = Objects.requireNonNull(delivery, "delivery");
            presentationCue = Objects.requireNonNull(presentationCue, "presentationCue");
            counterplay = java.util.Set.copyOf(Objects.requireNonNull(counterplay, "counterplay"));
        }

        /** Server-side hit-volume code must gate damage through this method. */
        public boolean mayApplyHit() {
            return timeline.hitWindowOpen();
        }

        /** Presentation code uses the same sampled phase rather than an independent timer. */
        public AttackTimeline.Phase presentationPhase() {
            return timeline.phase();
        }
    }
}
