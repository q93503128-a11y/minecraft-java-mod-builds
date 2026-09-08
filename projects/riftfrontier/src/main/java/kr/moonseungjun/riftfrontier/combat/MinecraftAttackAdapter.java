package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Minecraft server adapter for one authoritative attack state machine.
 *
 * <p>Cadence is never copied here. Every damage decision is gated by the sampled
 * {@link AttackStateMachine} state, so telegraph/recovery/cancel/complete can never open a hit
 * window independently of {@link CoreDefinition.AttackPattern}.</p>
 */
public final class MinecraftAttackAdapter {
    private final AttackStateMachine stateMachine = new AttackStateMachine();
    private final HitVolume hitVolume;
    private final float damage;
    private final Set<UUID> damagedThisExecution = new HashSet<>();

    public MinecraftAttackAdapter(HitVolume hitVolume, float damage) {
        this.hitVolume = Objects.requireNonNull(hitVolume, "hitVolume");
        if (!Float.isFinite(damage) || damage <= 0.0F) {
            throw new IllegalArgumentException("damage must be finite and > 0");
        }
        this.damage = damage;
    }

    public AttackExecution.Snapshot begin(CoreDefinition.AttackPattern pattern, long gameTick) {
        AttackExecution.Snapshot snapshot = stateMachine.begin(pattern, gameTick);
        damagedThisExecution.clear();
        return snapshot;
    }

    /**
     * Advances the authoritative timeline and, only during ACTIVE, resolves a bounded hit volume
     * and applies at most one damage event per target for this attack execution.
     */
    public TickResult tick(ServerLevel level, LivingEntity attacker, long gameTick) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(attacker, "attacker");

        AttackStateMachine.Step step = stateMachine.advance(gameTick);
        if (step.snapshot().isEmpty()) {
            return TickResult.idle();
        }

        AttackExecution.Snapshot snapshot = step.snapshot().orElseThrow();
        int candidates = 0;
        int damaged = 0;

        if (step.hitWindowOpen()) {
            Iterable<? extends LivingEntity> resolved = Objects.requireNonNull(
                hitVolume.resolve(level, attacker, snapshot),
                "hitVolume.resolve result"
            );
            for (LivingEntity target : resolved) {
                if (target == null || target == attacker || !target.isAlive()) {
                    continue;
                }
                candidates++;
                if (!damagedThisExecution.add(target.getUUID())) {
                    continue;
                }
                if (target.hurtServer(level, level.damageSources().mobAttack(attacker), damage)) {
                    damaged++;
                }
            }
        }

        if (step.finished()) {
            damagedThisExecution.clear();
        }

        return new TickResult(
            snapshot.presentationPhase(),
            step.hitWindowOpen(),
            step.phaseChanged(),
            step.finished(),
            candidates,
            damaged
        );
    }

    public boolean isExecuting() {
        return stateMachine.isExecuting();
    }

    /** Stun/death/despawn interruption. Cancellation immediately and permanently closes the hit window. */
    public boolean cancel() {
        boolean cancelled = stateMachine.cancel().isPresent();
        damagedThisExecution.clear();
        return cancelled;
    }

    @FunctionalInterface
    public interface HitVolume {
        Iterable<? extends LivingEntity> resolve(ServerLevel level, LivingEntity attacker, AttackExecution.Snapshot snapshot);
    }

    /**
     * Bounded local AABB resolver for technical/runtime attacks. Production attacks can provide a
     * shape-specific resolver while retaining the same authoritative timing gate.
     */
    public record AabbHitVolume(double horizontalRadius, double verticalRadius) implements HitVolume {
        public AabbHitVolume {
            if (!Double.isFinite(horizontalRadius) || horizontalRadius <= 0.0D) {
                throw new IllegalArgumentException("horizontalRadius must be finite and > 0");
            }
            if (!Double.isFinite(verticalRadius) || verticalRadius <= 0.0D) {
                throw new IllegalArgumentException("verticalRadius must be finite and > 0");
            }
        }

        @Override
        public Iterable<? extends LivingEntity> resolve(ServerLevel level, LivingEntity attacker, AttackExecution.Snapshot snapshot) {
            AABB bounds = attacker.getBoundingBox().inflate(horizontalRadius, verticalRadius, horizontalRadius);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                bounds,
                candidate -> candidate != attacker && candidate.isAlive()
            );
            return targets;
        }
    }

    public record TickResult(
        AttackTimeline.Phase phase,
        boolean hitWindowOpen,
        boolean phaseChanged,
        boolean finished,
        int candidateCount,
        int damagedCount
    ) {
        public TickResult {
            Objects.requireNonNull(phase, "phase");
            if (candidateCount < 0 || damagedCount < 0 || damagedCount > candidateCount) {
                throw new IllegalArgumentException("invalid candidate/damaged counts");
            }
            if (!hitWindowOpen && (candidateCount != 0 || damagedCount != 0)) {
                throw new IllegalArgumentException("closed hit window cannot resolve or damage candidates");
            }
            if (hitWindowOpen != (phase == AttackTimeline.Phase.ACTIVE)) {
                throw new IllegalArgumentException("hitWindowOpen must exactly match ACTIVE phase");
            }
        }

        public static TickResult idle() {
            return new TickResult(AttackTimeline.Phase.COMPLETE, false, false, false, 0, 0);
        }
    }
}
