package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Server-owned Minecraft adapter that keeps boss lifecycle, real damage and presentation sampling
 * on the same authored attack timeline.
 *
 * <p>The {@link BossCombatController} remains authoritative for phase/selection/completion while
 * {@link MinecraftAttackAdapter} owns Minecraft hit-volume resolution. Presentation consumers receive
 * an immutable {@link PresentationFrame} derived from the exact same {@link AttackExecution.Snapshot}
 * used to validate the damage phase. No animation/VFX cadence is authored in this adapter.</p>
 */
public final class MinecraftBossCombatAdapter {
    private final CombatRuntimeCatalog catalog;
    private final BossCombatController controller;
    private final MinecraftAttackAdapter damageAdapter;

    public MinecraftBossCombatAdapter(
        CombatRuntimeCatalog catalog,
        ContentId bossId,
        BossAttackSelectionPolicy selectionPolicy,
        MinecraftAttackAdapter.HitVolume hitVolume,
        float damage
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.controller = new BossCombatController(
            catalog,
            Objects.requireNonNull(bossId, "bossId"),
            Objects.requireNonNull(selectionPolicy, "selectionPolicy")
        );
        this.damageAdapter = new MinecraftAttackAdapter(Objects.requireNonNull(hitVolume, "hitVolume"), damage);
    }

    public int phase() {
        return controller.phase();
    }

    public boolean attackExecuting() {
        return controller.attackExecuting();
    }

    public long completedAttackCount() {
        return controller.completedAttackCount();
    }

    public AttackExecution.Snapshot beginNextAttack(long gameTick) {
        AttackExecution.Snapshot selected = controller.beginNextAttack(gameTick);
        try {
            AttackExecution.Snapshot damage = damageAdapter.begin(catalog.requireAttackPattern(selected.patternId()), gameTick);
            requireSameSnapshot(selected, damage);
            return selected;
        } catch (RuntimeException failure) {
            controller.cancelAttack();
            damageAdapter.cancel();
            throw failure;
        }
    }

    /** Advances boss lifecycle and real Minecraft damage from the same server game tick. */
    public TickResult tick(ServerLevel level, LivingEntity boss, long gameTick) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(boss, "boss");

        BossCombatController.Step bossStep = controller.advance(gameTick);
        MinecraftAttackAdapter.TickResult damageStep = damageAdapter.tick(level, boss, gameTick);

        if (bossStep.attackStep().snapshot().isEmpty()) {
            if (damageAdapter.isExecuting() || damageStep.hitWindowOpen() || damageStep.damagedCount() != 0) {
                failClosed("damage adapter remained active while boss controller was idle");
            }
            return new TickResult(controller.phase(), damageStep, false, Optional.empty());
        }

        AttackExecution.Snapshot bossSnapshot = bossStep.attackStep().snapshot().orElseThrow();
        if (bossSnapshot.presentationPhase() != damageStep.phase()
            || bossStep.attackStep().hitWindowOpen() != damageStep.hitWindowOpen()
            || bossStep.attackStep().finished() != damageStep.finished()) {
            failClosed("boss lifecycle and Minecraft damage timeline diverged");
        }

        PresentationFrame presentation = PresentationFrame.from(controller.phase(), bossSnapshot);
        if (presentation.hitWindowOpen() != damageStep.hitWindowOpen()) {
            failClosed("boss presentation and Minecraft damage hit window diverged");
        }

        return new TickResult(
            controller.phase(),
            damageStep,
            bossStep.attackStep().finished(),
            Optional.of(presentation)
        );
    }

    /**
     * Changes authoritative boss phase only after closing any old attack in both lifecycle and Minecraft damage state.
     */
    public BossCombatController.PhaseTransition transitionToPhase(int newPhase) {
        BossCombatController.PhaseTransition transition = controller.transitionToPhase(newPhase);
        boolean damageCancelled = damageAdapter.cancel();
        if (transition.interruptedAttack().isPresent() != damageCancelled) {
            failClosed("phase transition cancellation diverged between boss and damage state");
        }
        return transition;
    }

    /** Explicit stun/death/despawn interruption without a phase change. */
    public boolean cancelAttack() {
        boolean lifecycleCancelled = controller.cancelAttack().isPresent();
        boolean damageCancelled = damageAdapter.cancel();
        if (lifecycleCancelled != damageCancelled) {
            failClosed("explicit cancellation diverged between boss and damage state");
        }
        return lifecycleCancelled;
    }

    private void requireSameSnapshot(AttackExecution.Snapshot lifecycle, AttackExecution.Snapshot damage) {
        if (!lifecycle.patternId().equals(damage.patternId())
            || lifecycle.presentationPhase() != damage.presentationPhase()
            || lifecycle.mayApplyHit() != damage.mayApplyHit()) {
            failClosed("boss and Minecraft damage adapters did not start from the same attack snapshot");
        }
    }

    private void failClosed(String message) {
        controller.cancelAttack();
        damageAdapter.cancel();
        throw new IllegalStateException(message);
    }

    /**
     * Immutable client/presentation-facing view derived from the authoritative attack snapshot.
     *
     * <p>Animation, VFX, audio and telegraph adapters may map these semantic values to final assets,
     * but must not invent independent timing constants. `phaseProgress` comes directly from the
     * authored {@link AttackTimeline} sample.</p>
     */
    public record PresentationFrame(
        int bossPhase,
        ContentId patternId,
        AttackTimeline.Phase attackPhase,
        double phaseProgress,
        String presentationCue,
        String delivery,
        Set<String> counterplay,
        boolean hitWindowOpen
    ) {
        public PresentationFrame {
            if (bossPhase <= 0) {
                throw new IllegalArgumentException("bossPhase must be positive");
            }
            Objects.requireNonNull(patternId, "patternId");
            Objects.requireNonNull(attackPhase, "attackPhase");
            if (!Double.isFinite(phaseProgress) || phaseProgress < 0.0D || phaseProgress > 1.0D) {
                throw new IllegalArgumentException("phaseProgress must be finite and between 0 and 1");
            }
            presentationCue = Objects.requireNonNull(presentationCue, "presentationCue");
            delivery = Objects.requireNonNull(delivery, "delivery");
            counterplay = Set.copyOf(Objects.requireNonNull(counterplay, "counterplay"));
            if (hitWindowOpen != (attackPhase == AttackTimeline.Phase.ACTIVE)) {
                throw new IllegalArgumentException("presentation hitWindowOpen must exactly match ACTIVE phase");
            }
        }

        public static PresentationFrame from(int bossPhase, AttackExecution.Snapshot snapshot) {
            Objects.requireNonNull(snapshot, "snapshot");
            return new PresentationFrame(
                bossPhase,
                snapshot.patternId(),
                snapshot.presentationPhase(),
                snapshot.timeline().phaseProgress(),
                snapshot.presentationCue(),
                snapshot.delivery(),
                snapshot.counterplay(),
                snapshot.mayApplyHit()
            );
        }
    }

    public record TickResult(
        int bossPhase,
        MinecraftAttackAdapter.TickResult attack,
        boolean attackFinished,
        Optional<PresentationFrame> presentation
    ) {
        public TickResult {
            if (bossPhase <= 0) {
                throw new IllegalArgumentException("bossPhase must be positive");
            }
            Objects.requireNonNull(attack, "attack");
            presentation = Objects.requireNonNull(presentation, "presentation");
            if (attackFinished != attack.finished()) {
                throw new IllegalArgumentException("attackFinished must match Minecraft attack completion");
            }
            if (presentation.isEmpty()) {
                if (attack.hitWindowOpen() || attack.damagedCount() != 0 || attack.candidateCount() != 0) {
                    throw new IllegalArgumentException("idle presentation cannot accompany active damage state");
                }
            } else {
                PresentationFrame frame = presentation.orElseThrow();
                if (frame.bossPhase() != bossPhase
                    || frame.attackPhase() != attack.phase()
                    || frame.hitWindowOpen() != attack.hitWindowOpen()) {
                    throw new IllegalArgumentException("presentation must match authoritative boss/damage state");
                }
            }
        }
    }
}
