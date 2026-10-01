package dev.moonseungjun.openworldrpg.combat.runtime;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

public final class MageArcaneWeaveRuntimeState {
    public static final long BASE_SEQUENCE_WINDOW_TICKS = 160L;
    public static final long WEAVE_READY_WINDOW_TICKS = 160L;

    private final Set<String> sequenceIds = new LinkedHashSet<>();
    private final Map<String, EmpoweredCast> empoweredCasts = new HashMap<>();
    private long sequenceExpiresAt = Long.MIN_VALUE;
    private long weaveReadyExpiresAt = Long.MIN_VALUE;
    private long resonantMindReadyAt = Long.MIN_VALUE;

    public CastResult recordAcceptedActive(
            String spellId,
            boolean weaveConsumer,
            long nowTick,
            long sequenceExpiryBonusTicks,
            double weaveEffectMagnitudeMultiplier
    ) {
        if (spellId == null || spellId.isBlank()
                || nowTick < 0L
                || sequenceExpiryBonusTicks < 0L
                || !Double.isFinite(weaveEffectMagnitudeMultiplier)
                || weaveEffectMagnitudeMultiplier < 1.0) {
            throw new IllegalArgumentException("Invalid Arcane Weave cast input.");
        }

        expire(nowTick);

        boolean consumedReady = weaveConsumer && weaveReadyExpiresAt > nowTick;
        if (consumedReady) {
            weaveReadyExpiresAt = Long.MIN_VALUE;
            empoweredCasts.put(
                    spellId,
                    new EmpoweredCast(
                            weaveEffectMagnitudeMultiplier,
                            nowTick + WEAVE_READY_WINDOW_TICKS
                    )
            );
        }

        boolean distinctAdded = sequenceIds.add(spellId);
        sequenceExpiresAt = nowTick
                + BASE_SEQUENCE_WINDOW_TICKS
                + sequenceExpiryBonusTicks;

        boolean completed = sequenceIds.size() >= 3;
        if (completed) {
            sequenceIds.clear();
            sequenceExpiresAt = Long.MIN_VALUE;
            weaveReadyExpiresAt = nowTick + WEAVE_READY_WINDOW_TICKS;
        }

        return new CastResult(
                distinctAdded,
                completed,
                consumedReady,
                sequenceIds.size(),
                weaveReadyExpiresAt > nowTick,
                consumedReady ? weaveEffectMagnitudeMultiplier : 1.0
        );
    }

    public int sigilCount(long nowTick) {
        expire(nowTick);
        return sequenceIds.size();
    }

    public boolean weaveReady(long nowTick) {
        expire(nowTick);
        return weaveReadyExpiresAt > nowTick;
    }

    public boolean atTwoSigils(long nowTick) {
        expire(nowTick);
        return sequenceIds.size() == 2;
    }

    public long sequenceRemainingTicks(long nowTick) {
        expire(nowTick);
        return sequenceIds.isEmpty()
                ? 0L
                : Math.max(0L, sequenceExpiresAt - nowTick);
    }

    public DirectHpDamageResult recordDirectHpDamage(
            long nowTick,
            long shortenTicks
    ) {
        if (nowTick < 0L || shortenTicks < 0L) {
            throw new IllegalArgumentException(
                    "Arcane Memory damage timing must be non-negative."
            );
        }
        expire(nowTick);
        if (sequenceIds.size() != 2 || shortenTicks == 0L) {
            return new DirectHpDamageResult(
                    false,
                    0L,
                    sequenceRemainingTicks(nowTick),
                    sequenceIds.size()
            );
        }

        long remainingBefore = Math.max(
                0L,
                sequenceExpiresAt - nowTick
        );
        long shortened = Math.min(
                shortenTicks,
                remainingBefore
        );
        sequenceExpiresAt -= shortened;
        expire(nowTick);
        return new DirectHpDamageResult(
                true,
                shortened,
                sequenceRemainingTicks(nowTick),
                sequenceIds.size()
        );
    }

    public OptionalDouble consumeWeaveReadyForUltimate(
            long nowTick,
            double weaveEffectMagnitudeMultiplier
    ) {
        if (nowTick < 0L
                || !Double.isFinite(weaveEffectMagnitudeMultiplier)
                || weaveEffectMagnitudeMultiplier < 1.0) {
            throw new IllegalArgumentException("Invalid ultimate Weave consumption input.");
        }
        expire(nowTick);
        if (weaveReadyExpiresAt <= nowTick) {
            return OptionalDouble.empty();
        }
        weaveReadyExpiresAt = Long.MIN_VALUE;
        return OptionalDouble.of(weaveEffectMagnitudeMultiplier);
    }

    public OptionalDouble consumeEmpoweredCast(String spellId, long nowTick) {
        Objects.requireNonNull(spellId, "spellId");
        expire(nowTick);
        EmpoweredCast cast = empoweredCasts.remove(spellId);
        return cast == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(cast.magnitudeMultiplier());
    }

    public boolean tryClaimResonantMind(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick cannot be negative.");
        }
        if (nowTick < resonantMindReadyAt) {
            return false;
        }
        resonantMindReadyAt =
                nowTick + MageRootPassiveEffects.RESONANT_MIND_ICD_TICKS;
        return true;
    }

    private void expire(long nowTick) {
        if (!sequenceIds.isEmpty() && sequenceExpiresAt <= nowTick) {
            sequenceIds.clear();
            sequenceExpiresAt = Long.MIN_VALUE;
        }
        if (weaveReadyExpiresAt <= nowTick) {
            weaveReadyExpiresAt = Long.MIN_VALUE;
        }
        empoweredCasts.entrySet().removeIf(
                entry -> entry.getValue().expiresAtTick() <= nowTick
        );
    }

    public record DirectHpDamageResult(
            boolean arcaneMemoryActive,
            long shortenedTicks,
            long remainingTicks,
            int sigilCountAfter
    ) {
        public DirectHpDamageResult {
            if (shortenedTicks < 0L
                    || remainingTicks < 0L
                    || sigilCountAfter < 0
                    || sigilCountAfter > 2
                    || (!arcaneMemoryActive && shortenedTicks != 0L)) {
                throw new IllegalArgumentException(
                        "Invalid Arcane Memory damage result."
                );
            }
        }
    }

    private record EmpoweredCast(double magnitudeMultiplier, long expiresAtTick) {
        private EmpoweredCast {
            if (!Double.isFinite(magnitudeMultiplier)
                    || magnitudeMultiplier < 1.0
                    || expiresAtTick < 0L) {
                throw new IllegalArgumentException("Invalid empowered Mage cast snapshot.");
            }
        }
    }

    public record CastResult(
            boolean distinctSigilAdded,
            boolean weaveCompleted,
            boolean weaveConsumed,
            int sigilCountAfter,
            boolean weaveReadyAfter,
            double weaveEffectMagnitudeMultiplier
    ) {
        public CastResult {
            if (sigilCountAfter < 0 || sigilCountAfter > 2
                    || !Double.isFinite(weaveEffectMagnitudeMultiplier)
                    || weaveEffectMagnitudeMultiplier < 1.0
                    || (!weaveConsumed && weaveEffectMagnitudeMultiplier != 1.0)) {
                throw new IllegalArgumentException("Invalid Arcane Weave cast result.");
            }
        }
    }
}
