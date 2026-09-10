package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
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
    private static final Map<LivingEntity, Set<ValidatedRuntime>> VALIDATED_RUNTIMES_BY_OWNER = new IdentityHashMap<>();

    private final CombatRuntimeCatalog catalog;
    private final BossCombatController controller;
    private final MinecraftAttackAdapter damageAdapter;

    /**
     * Low-level construction boundary retained for API-free/runtime contract tests and non-production adapters.
     *
     * <p>Production boss runtime must use {@link #validated(CombatRuntimeCatalog, ValidatedBossCombatSemantics,
     * MinecraftAttackAdapter.HitVolume, float)} so boss identity, phase attack selection and outgoing presentation
     * state all originate from one validated semantic capability.</p>
     */
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

    public static ValidatedRuntime validated(
        CombatRuntimeCatalog catalog,
        ValidatedBossCombatSemantics semantics,
        MinecraftAttackAdapter.HitVolume hitVolume,
        float damage
    ) {
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(semantics, "semantics");
        return new ValidatedRuntime(
            semantics,
            new MinecraftBossCombatAdapter(
                catalog,
                semantics.bossProfile(),
                BossAttackSelectionPolicy.authoredPhases(semantics),
                Objects.requireNonNull(hitVolume, "hitVolume"),
                damage
            ),
            PublishedContentGenerationGuard.fromCatalog(catalog)
        );
    }

    /** Event-driven lifetime boundary for validated Minecraft boss capabilities. */
    public static void entityLeaveLevel(EntityLeaveLevelEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity owner)) {
            return;
        }

        Set<ValidatedRuntime> runtimes;
        synchronized (VALIDATED_RUNTIMES_BY_OWNER) {
            runtimes = VALIDATED_RUNTIMES_BY_OWNER.remove(owner);
        }
        if (runtimes == null || runtimes.isEmpty()) {
            return;
        }
        for (ValidatedRuntime runtime : Set.copyOf(runtimes)) {
            runtime.invalidateMinecraftOwner(owner);
        }
    }

    /** Claims the single authoritative validated boss capability for one live entity instance. */
    private static void bindValidatedRuntimeOwner(LivingEntity owner, ValidatedRuntime runtime) {
        synchronized (VALIDATED_RUNTIMES_BY_OWNER) {
            Set<ValidatedRuntime> runtimes = VALIDATED_RUNTIMES_BY_OWNER
                .computeIfAbsent(owner, ignored -> Collections.newSetFromMap(new IdentityHashMap<>()));

            Iterator<ValidatedRuntime> iterator = runtimes.iterator();
            while (iterator.hasNext()) {
                ValidatedRuntime existing = iterator.next();
                if (existing != runtime && existing.retireIfGenerationStale()) {
                    iterator.remove();
                }
            }

            if (!runtimes.isEmpty() && !runtimes.contains(runtime)) {
                throw new IllegalStateException("Boss already owns a different validated combat runtime");
            }
            runtimes.add(runtime);
        }
    }

    /** Removes exactly one retired capability claim without disturbing a newer runtime for the same live actor. */
    private static void releaseValidatedRuntimeOwner(LivingEntity owner, ValidatedRuntime runtime) {
        synchronized (VALIDATED_RUNTIMES_BY_OWNER) {
            Set<ValidatedRuntime> runtimes = VALIDATED_RUNTIMES_BY_OWNER.get(owner);
            if (runtimes == null) return;
            runtimes.remove(runtime);
            if (runtimes.isEmpty()) {
                VALIDATED_RUNTIMES_BY_OWNER.remove(owner);
            }
        }
    }

    public int phase() { return controller.phase(); }
    public boolean attackExecuting() { return controller.attackExecuting(); }
    public long completedAttackCount() { return controller.completedAttackCount(); }

    /**
     * Low-level timeline-only begin retained for API-free contract tests. Minecraft-facing callers
     * should prefer {@link #beginNextAttack(ServerLevel, LivingEntity, long)} so ownership is fixed
     * before the first authoritative tick can occur.
     */
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

    /** Begins a boss attack and immediately binds the Minecraft damage execution to one eligible actor. */
    public AttackExecution.Snapshot beginNextAttack(ServerLevel level, LivingEntity boss, long gameTick) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(boss, "boss");
        if (!MinecraftCombatAuthority.isEligibleServerActor(level, boss)) {
            cancelAttack();
            throw new IllegalStateException("Boss is not eligible to begin authoritative Minecraft combat");
        }

        AttackExecution.Snapshot selected = controller.beginNextAttack(gameTick);
        try {
            AttackExecution.Snapshot damage = damageAdapter.begin(
                level,
                boss,
                catalog.requireAttackPattern(selected.patternId()),
                gameTick
            );
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
        if (!MinecraftCombatAuthority.isEligibleServerActor(level, boss)) {
            cancelAttack();
            return new TickResult(controller.phase(), MinecraftAttackAdapter.TickResult.idle(), false, Optional.empty());
        }

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

        return new TickResult(controller.phase(), damageStep, bossStep.attackStep().finished(), Optional.of(presentation));
    }

    public BossCombatController.PhaseTransition transitionToPhase(int newPhase) {
        BossCombatController.PhaseTransition transition = controller.transitionToPhase(newPhase);
        boolean damageCancelled = damageAdapter.cancel();
        if (transition.interruptedAttack().isPresent() != damageCancelled) {
            failClosed("phase transition cancellation diverged between boss and damage state");
        }
        return transition;
    }

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

    public static final class ValidatedRuntime {
        private final ValidatedBossCombatSemantics semantics;
        private final MinecraftBossCombatAdapter delegate;
        private final Optional<PublishedContentGenerationGuard> generationGuard;
        private LivingEntity minecraftOwner;
        private ResourceKey<Level> minecraftOwnerDimension;
        private boolean minecraftOwnerInvalidated;
        private boolean minecraftOwnerGenerationRetired;

        private ValidatedRuntime(
            ValidatedBossCombatSemantics semantics,
            MinecraftBossCombatAdapter delegate,
            Optional<PublishedContentGenerationGuard> generationGuard
        ) {
            this.semantics = Objects.requireNonNull(semantics, "semantics");
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.generationGuard = Objects.requireNonNull(generationGuard, "generationGuard");
        }

        public ValidatedBossCombatSemantics semanticCapability() {
            requireRuntimeCurrent();
            return semantics;
        }

        public ContentId bossProfile() {
            requireRuntimeCurrent();
            return semantics.bossProfile();
        }

        public int phase() {
            requireRuntimeCurrent();
            return delegate.phase();
        }

        public boolean attackExecuting() {
            requireRuntimeCurrent();
            return delegate.attackExecuting();
        }

        public long completedAttackCount() {
            requireRuntimeCurrent();
            return delegate.completedAttackCount();
        }

        /** Detached/runtime-test mutation path. It is permanently unavailable after Minecraft ownership is bound. */
        public AttackExecution.Snapshot beginNextAttack(long gameTick) {
            requireRuntimeCurrent();
            requireDetachedMutation("begin attack");
            return validateSelectedAttack(delegate.beginNextAttack(gameTick));
        }

        /**
         * Minecraft-facing begin. The first accepted actor permanently owns this validated capability
         * for its current level lifetime; a later actor cannot adopt it after an attack completes.
         */
        public AttackExecution.Snapshot beginNextAttack(ServerLevel level, LivingEntity boss, long gameTick) {
            requireRuntimeCurrent();
            requireMinecraftOwner(level, boss);
            return validateSelectedAttack(delegate.beginNextAttack(level, boss, gameTick));
        }

        private AttackExecution.Snapshot validateSelectedAttack(AttackExecution.Snapshot snapshot) {
            if (!semantics.candidateAttacks(delegate.phase()).contains(snapshot.patternId())) {
                delegate.cancelAttack();
                throw new IllegalStateException("selected attack escaped validated semantic phase pool: " + snapshot.patternId());
            }
            return snapshot;
        }

        public ValidatedTickResult tick(ServerLevel level, LivingEntity boss, long gameTick) {
            requireRuntimeCurrent();
            requireMinecraftOwner(level, boss);
            TickResult combat = delegate.tick(level, boss, gameTick);
            return ValidatedTickResult.bind(semantics, combat, boss.getId(), boss.getUUID(), gameTick);
        }

        /** Detached/runtime-test phase mutation path; forbidden after Minecraft ownership is established. */
        public BossCombatController.PhaseTransition transitionToPhase(int newPhase) {
            requireRuntimeCurrent();
            requireDetachedMutation("transition phase");
            return validatePhaseTransition(delegate.transitionToPhase(newPhase));
        }

        /** Server-authoritative phase mutation for a Minecraft-bound validated boss capability. */
        public BossCombatController.PhaseTransition transitionToPhase(
            ServerLevel level,
            LivingEntity boss,
            int newPhase
        ) {
            requireRuntimeCurrent();
            requireMinecraftOwner(level, boss);
            return validatePhaseTransition(delegate.transitionToPhase(newPhase));
        }

        private BossCombatController.PhaseTransition validatePhaseTransition(
            BossCombatController.PhaseTransition transition
        ) {
            semantics.candidateAttacks(transition.newPhase());
            return transition;
        }

        /** Cancellation stays available even after publication/owner invalidation so cleanup can never be blocked. */
        public boolean cancelAttack() { return delegate.cancelAttack(); }

        private void requireRuntimeCurrent() {
            if (minecraftOwnerInvalidated) {
                delegate.cancelAttack();
                throw new IllegalStateException("Validated boss runtime owner has left its authoritative level");
            }
            if (minecraftOwnerGenerationRetired) {
                delegate.cancelAttack();
                throw new IllegalStateException("Validated boss runtime was retired after its published content generation became stale");
            }
            requireCurrentGeneration();
        }

        private void requireCurrentGeneration() {
            if (generationGuard.isEmpty()) return;
            try {
                generationGuard.orElseThrow().requireCurrent();
            } catch (IllegalStateException stale) {
                retireStaleGenerationOwnerClaim();
                throw stale;
            }
        }

        /**
         * Called while another runtime is attempting to claim the same live actor. A stale generation
         * must relinquish only its process-local singleton slot; the retained capability itself remains
         * permanently retired and cannot later rebind.
         */
        private boolean retireIfGenerationStale() {
            if (minecraftOwnerGenerationRetired || generationGuard.isEmpty()) return minecraftOwnerGenerationRetired;
            if (generationGuard.orElseThrow().isCurrent()) return false;
            minecraftOwnerGenerationRetired = true;
            delegate.cancelAttack();
            return true;
        }

        private void retireStaleGenerationOwnerClaim() {
            if (!retireIfGenerationStale()) return;
            LivingEntity owner = minecraftOwner;
            if (owner != null) {
                releaseValidatedRuntimeOwner(owner, this);
            }
        }

        private void requireDetachedMutation(String action) {
            if (minecraftOwner != null) {
                delegate.cancelAttack();
                throw new IllegalStateException(
                    "Minecraft-bound validated boss runtime cannot " + action + " without authoritative owner context"
                );
            }
        }

        private void requireMinecraftOwner(ServerLevel level, LivingEntity boss) {
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(boss, "boss");
            if (!MinecraftCombatAuthority.isEligibleServerActor(level, boss)) {
                delegate.cancelAttack();
                throw new IllegalStateException("Boss is not eligible to own a validated Minecraft combat runtime");
            }

            if (minecraftOwner == null) {
                bindValidatedRuntimeOwner(boss, this);
                minecraftOwner = boss;
                minecraftOwnerDimension = level.dimension();
                return;
            }

            if (minecraftOwner != boss
                || minecraftOwnerDimension == null
                || !minecraftOwnerDimension.equals(level.dimension())
                || boss.level() != level) {
                delegate.cancelAttack();
                throw new IllegalStateException("Validated boss runtime belongs to a different server actor or dimension");
            }
        }

        private void invalidateMinecraftOwner(LivingEntity owner) {
            if (minecraftOwner != owner || minecraftOwnerInvalidated) {
                return;
            }
            minecraftOwnerInvalidated = true;
            delegate.cancelAttack();
        }
    }

    public static final class ValidatedTickResult {
        private final ValidatedBossCombatSemantics semantics;
        private final TickResult combat;
        private final BossPresentationSemanticState presentationState;
        private final Optional<ValidatedBossCombatSemantics.ValidatedPresentation> presentationBinding;

        private ValidatedTickResult(
            ValidatedBossCombatSemantics semantics,
            TickResult combat,
            BossPresentationSemanticState presentationState,
            Optional<ValidatedBossCombatSemantics.ValidatedPresentation> presentationBinding
        ) {
            this.semantics = Objects.requireNonNull(semantics, "semantics");
            this.combat = Objects.requireNonNull(combat, "combat");
            this.presentationState = Objects.requireNonNull(presentationState, "presentationState");
            this.presentationBinding = Objects.requireNonNull(presentationBinding, "presentationBinding");
            if (presentationState.active() != presentationBinding.isPresent()) {
                throw new IllegalArgumentException("active semantic state must carry exactly one validated presentation binding");
            }
        }

        private static ValidatedTickResult bind(
            ValidatedBossCombatSemantics semantics,
            TickResult combat,
            int entityId,
            java.util.UUID entityUuid,
            long gameTick
        ) {
            Objects.requireNonNull(semantics, "semantics");
            Objects.requireNonNull(combat, "combat");
            BossPresentationSemanticState state;
            Optional<ValidatedBossCombatSemantics.ValidatedPresentation> binding;
            if (combat.presentation().isPresent()) {
                state = BossPresentationSemanticState.fromFrame(entityId, entityUuid, gameTick, combat.presentation().orElseThrow());
                binding = Optional.of(semantics.validatePresentationState(state));
            } else {
                state = BossPresentationSemanticState.clear(entityId, entityUuid, gameTick);
                binding = Optional.empty();
            }
            return new ValidatedTickResult(semantics, combat, state, binding);
        }

        public TickResult combat() { return combat; }
        public BossPresentationSemanticState presentationState() { return presentationState; }
        public Optional<ValidatedBossCombatSemantics.ValidatedPresentation> presentationBinding() { return presentationBinding; }
        public ValidatedBossCombatSemantics semanticCapability() { return semantics; }

        public void requireSemanticCapability(ValidatedBossCombatSemantics expected) {
            if (semantics != Objects.requireNonNull(expected, "expected")) {
                throw new IllegalArgumentException("validated boss tick belongs to a different semantic capability");
            }
        }
    }

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
            if (bossPhase <= 0) throw new IllegalArgumentException("bossPhase must be positive");
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
            if (bossPhase <= 0) throw new IllegalArgumentException("bossPhase must be positive");
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
