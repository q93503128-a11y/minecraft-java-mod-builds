package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Minecraft-facing server authority boundary for player-weapon execution.
 *
 * <p>The adapter deliberately owns no balance values. Equipment is resolved on the server into a
 * validated family/module loadout, {@link PlayerWeaponCombatController} remains the only attack
 * clock, and hit-volume candidates are exposed only while that clock is ACTIVE. Damage, range and
 * presentation remain policy supplied by later validated production data.</p>
 */
public final class MinecraftPlayerWeaponCombatAdapter {
    private final CombatRuntimeCatalog catalog;
    private final Optional<PublishedContentGenerationGuard> generationGuard;
    private final LoadoutResolver loadoutResolver;
    private final HitVolume hitVolume;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public MinecraftPlayerWeaponCombatAdapter(
        CombatRuntimeCatalog catalog,
        LoadoutResolver loadoutResolver,
        HitVolume hitVolume
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.generationGuard = PublishedContentGenerationGuard.fromCatalog(catalog);
        this.loadoutResolver = Objects.requireNonNull(loadoutResolver, "loadoutResolver");
        this.hitVolume = Objects.requireNonNull(hitVolume, "hitVolume");
    }

    /** Accepts an input intent only after re-resolving the actor's authoritative server loadout. */
    public AttackExecution.Snapshot beginMove(LivingEntity actor, ContentId moveId, long gameTick) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(moveId, "moveId");
        requireCombatEligible(actor);
        requireCurrentGeneration(actor.getUUID());
        Loadout loadout = requireLoadout(actor);
        Session session = synchronizeSession(actor, loadout);
        AttackExecution.Snapshot snapshot = session.controller.beginMove(moveId, gameTick);
        session.hitTargets.clear();
        return snapshot;
    }

    /**
     * Advances one actor from the same server-owned clock and exposes unique hit-volume candidates
     * only during ACTIVE. A published content-generation change, loadout swap, actor-instance replacement,
     * world/dimension discontinuity, death/removal, or spectator transition immediately cancels and discards
     * the old execution.
     */
    public TickResult tick(ServerLevel level, LivingEntity actor, long gameTick) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(actor, "actor");

        Session session = sessions.get(actor.getUUID());
        if (session == null) return TickResult.idle();
        if (!isCurrentGeneration()) {
            invalidate(actor.getUUID(), session);
            return TickResult.invalidated();
        }
        if (session.actor != actor) {
            invalidate(actor.getUUID(), session);
            return TickResult.invalidated();
        }
        if (!MinecraftCombatAuthority.isEligibleServerActor(level, actor)) {
            invalidate(actor.getUUID(), session);
            return TickResult.invalidated();
        }

        Optional<Loadout> currentLoadout = loadoutResolver.resolve(actor);
        if (!session.dimension.equals(level.dimension())
            || currentLoadout.isEmpty()
            || !session.loadout.equals(currentLoadout.orElseThrow())) {
            invalidate(actor.getUUID(), session);
            return TickResult.invalidated();
        }

        AttackStateMachine.Step step = session.controller.advance(gameTick);
        if (step.snapshot().isEmpty()) return TickResult.idle();
        AttackExecution.Snapshot snapshot = step.snapshot().orElseThrow();
        int candidates = 0;
        if (step.hitWindowOpen()) {
            Iterable<? extends LivingEntity> resolved = Objects.requireNonNull(
                hitVolume.resolve(level, actor, snapshot),
                "hitVolume.resolve result"
            );
            for (LivingEntity target : resolved) {
                if (!MinecraftCombatAuthority.isEligibleTarget(level, actor, target)) continue;
                if (session.hitTargets.add(target.getUUID())) candidates++;
            }
        }
        if (step.finished()) session.hitTargets.clear();
        return new TickResult(snapshot.presentationPhase(), step.hitWindowOpen(), step.finished(), false, candidates);
    }

    /** Module movement semantics can never authorize outside RECOVERY or from stale combat content. */
    public boolean recoveryPivotAuthorized(LivingEntity actor, long gameTick) {
        Objects.requireNonNull(actor, "actor");
        Session session = sessions.get(actor.getUUID());
        if (session == null) return false;
        if (!isCurrentGeneration()) {
            invalidate(actor.getUUID(), session);
            return false;
        }
        if (session.actor != actor || !MinecraftCombatAuthority.isEligibleServerActor(actor)) {
            invalidate(actor.getUUID(), session);
            return false;
        }
        Optional<Loadout> current = loadoutResolver.resolve(actor);
        if (!session.dimension.equals(actor.level().dimension())
            || current.isEmpty()
            || !session.loadout.equals(current.orElseThrow())) {
            invalidate(actor.getUUID(), session);
            return false;
        }
        return session.controller.recoveryPivotAuthorized(gameTick);
    }

    /** Death/logout/despawn/dimension handoff hook. */
    public boolean clearActor(UUID actorId) {
        Objects.requireNonNull(actorId, "actorId");
        Session removed = sessions.remove(actorId);
        if (removed == null) return false;
        removed.controller.cancel();
        removed.hitTargets.clear();
        return true;
    }

    public boolean hasSession(UUID actorId) {
        return sessions.containsKey(Objects.requireNonNull(actorId, "actorId"));
    }

    private Loadout requireLoadout(LivingEntity actor) {
        return loadoutResolver.resolve(actor)
            .orElseThrow(() -> new IllegalStateException("Actor has no validated Riftfrontier weapon loadout: " + actor.getUUID()));
    }

    private void requireCombatEligible(LivingEntity actor) {
        if (MinecraftCombatAuthority.isEligibleServerActor(actor)) return;
        clearActor(actor.getUUID());
        throw new IllegalStateException("Actor is not combat-eligible for Riftfrontier weapon authority: " + actor.getUUID());
    }

    private void requireCurrentGeneration(UUID actorId) {
        if (generationGuard.isEmpty()) return;
        try {
            generationGuard.orElseThrow().requireCurrent();
        } catch (IllegalStateException stale) {
            clearActor(actorId);
            throw stale;
        }
    }

    private boolean isCurrentGeneration() {
        return generationGuard.isEmpty() || generationGuard.orElseThrow().isCurrent();
    }

    private Session synchronizeSession(LivingEntity actor, Loadout loadout) {
        ResourceKey<Level> dimension = actor.level().dimension();
        Session existing = sessions.get(actor.getUUID());
        if (existing != null
            && existing.actor == actor
            && existing.loadout.equals(loadout)
            && existing.dimension.equals(dimension)) {
            return existing;
        }
        if (existing != null) invalidate(actor.getUUID(), existing);
        PlayerWeaponCombatController controller = catalog.playerWeaponController(loadout.familyId(), loadout.moduleId());
        requireCurrentGeneration(actor.getUUID());
        Session replacement = new Session(actor, loadout, dimension, controller);
        sessions.put(actor.getUUID(), replacement);
        return replacement;
    }

    private void invalidate(UUID actorId, Session session) {
        session.controller.cancel();
        session.hitTargets.clear();
        sessions.remove(actorId, session);
    }

    /** Server equipment decoder output; IDs must resolve through the current validated catalog. */
    public record Loadout(ContentId familyId, Optional<ContentId> moduleId) {
        public Loadout {
            Objects.requireNonNull(familyId, "familyId");
            moduleId = Objects.requireNonNull(moduleId, "moduleId");
        }
    }

    @FunctionalInterface
    public interface LoadoutResolver {
        Optional<Loadout> resolve(LivingEntity actor);
    }

    @FunctionalInterface
    public interface HitVolume {
        Iterable<? extends LivingEntity> resolve(ServerLevel level, LivingEntity actor, AttackExecution.Snapshot snapshot);
    }

    public record TickResult(
        AttackTimeline.Phase phase,
        boolean hitWindowOpen,
        boolean finished,
        boolean loadoutInvalidated,
        int uniqueCandidateCount
    ) {
        public TickResult {
            Objects.requireNonNull(phase, "phase");
            if (uniqueCandidateCount < 0) throw new IllegalArgumentException("uniqueCandidateCount must be >= 0");
            if (!hitWindowOpen && uniqueCandidateCount != 0) {
                throw new IllegalArgumentException("closed hit window cannot expose hit candidates");
            }
            if (hitWindowOpen != (phase == AttackTimeline.Phase.ACTIVE)) {
                throw new IllegalArgumentException("hitWindowOpen must exactly match ACTIVE phase");
            }
        }

        public static TickResult idle() {
            return new TickResult(AttackTimeline.Phase.COMPLETE, false, false, false, 0);
        }

        public static TickResult invalidated() {
            return new TickResult(AttackTimeline.Phase.COMPLETE, false, false, true, 0);
        }
    }

    private static final class Session {
        private final LivingEntity actor;
        private final Loadout loadout;
        private final ResourceKey<Level> dimension;
        private final PlayerWeaponCombatController controller;
        private final Set<UUID> hitTargets = new HashSet<>();

        private Session(
            LivingEntity actor,
            Loadout loadout,
            ResourceKey<Level> dimension,
            PlayerWeaponCombatController controller
        ) {
            this.actor = Objects.requireNonNull(actor, "actor");
            this.loadout = Objects.requireNonNull(loadout, "loadout");
            this.dimension = Objects.requireNonNull(dimension, "dimension");
            this.controller = Objects.requireNonNull(controller, "controller");
        }
    }
}
