package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * Server-owned Minecraft adapter that keeps boss lifecycle and real damage on the same authored attack timeline.
 *
 * <p>The {@link BossCombatController} remains authoritative for phase/selection/completion while
 * {@link MinecraftAttackAdapter} owns Minecraft hit-volume resolution. This adapter starts, advances and cancels
 * both halves together and fails closed if their sampled phases ever diverge.</p>
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
            return new TickResult(controller.phase(), damageStep, false);
        }

        AttackExecution.Snapshot bossSnapshot = bossStep.attackStep().snapshot().orElseThrow();
        if (bossSnapshot.presentationPhase() != damageStep.phase()
            || bossStep.attackStep().hitWindowOpen() != damageStep.hitWindowOpen()
            || bossStep.attackStep().finished() != damageStep.finished()) {
            failClosed("boss lifecycle and Minecraft damage timeline diverged");
        }

        return new TickResult(controller.phase(), damageStep, bossStep.attackStep().finished());
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

    public record TickResult(int bossPhase, MinecraftAttackAdapter.TickResult attack, boolean attackFinished) {
        public TickResult {
            if (bossPhase <= 0) {
                throw new IllegalArgumentException("bossPhase must be positive");
            }
            Objects.requireNonNull(attack, "attack");
            if (attackFinished != attack.finished()) {
                throw new IllegalArgumentException("attackFinished must match Minecraft attack completion");
            }
        }
    }
}
