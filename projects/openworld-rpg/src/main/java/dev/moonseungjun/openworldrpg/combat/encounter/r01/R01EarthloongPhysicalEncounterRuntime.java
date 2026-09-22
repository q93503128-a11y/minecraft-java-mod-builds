package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerPoisePressureRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class R01EarthloongPhysicalEncounterRuntime {
    private static final String EARTHLOONG_ID = "threateningly_mobs:the_earthloong";
    private static final R01EarthloongEncounterData DATA = R01EarthloongEncounterDataLoader.loadBundled();
    private static final Map<UUID, ActorState> STATES = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private R01EarthloongPhysicalEncounterRuntime() {}

    public static synchronized void initialize(RuntimeProfile profile, Logger logger) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(logger, "logger");
        if (initialized) return;
        if (profile == RuntimeProfile.CORE) {
            initialized = true;
            logger.info("Openworld RPG R01 Earthloong physical runtime inactive for core profile.");
            return;
        }

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living && isEarthloong(living)) {
                STATES.computeIfAbsent(
                        living.getUUID(),
                        ignored -> new ActorState(
                                living, "r01-earthloong:" + level.dimension() + ":" + living.getUUID()));
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            ActorState removed = STATES.remove(entity.getUUID());
            if (removed != null) removed.releaseActorControl();
        });
        ServerTickEvents.START_LEVEL_TICK.register(
                R01EarthloongPhysicalEncounterRuntime::suppressDonorCombatTargets
        );
        ServerTickEvents.END_LEVEL_TICK.register(R01EarthloongPhysicalEncounterRuntime::tickLevel);
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> clearPlayer(handler.getPlayer().getUUID()));

        initialized = true;
        logger.info(
                "Openworld RPG R01 Earthloong physical encounter runtime armed: "
                        + "Claw Sweep + Quarry Rush project scheduling/contact active; "
                        + "Tail Scythe presentation and Phase-1 space-control actions remain gated.");
    }

    public static boolean recordProjectDamageThreat(
            LivingEntity earthloong,
            ServerPlayer attacker,
            double appliedCanonicalDamage,
            long gameTick) {
        if (!Double.isFinite(appliedCanonicalDamage) || appliedCanonicalDamage <= 0.0 || gameTick < 0L) {
            return false;
        }
        ActorState state = STATES.get(earthloong.getUUID());
        if (state == null || state.actor != earthloong) return false;

        var profile = ExternalActorBindingRuntime.combatProfile(earthloong).orElse(null);
        if (profile == null || !EARTHLOONG_ID.equals(profile.entityId())) return false;

        state.threat.engageInitial(attacker.getUUID(), gameTick);
        state.threat.addThreat(
                attacker.getUUID(),
                100.0 * appliedCanonicalDamage / profile.maxHealth(),
                gameTick);
        state.scheduleDecisionIfIdle(gameTick);
        return true;
    }

    public static void clearPlayer(UUID playerId) {
        for (ActorState state : STATES.values()) state.threat.remove(playerId);
    }

    private static void suppressDonorCombatTargets(ServerLevel level) {
        for (ActorState state : STATES.values()) {
            if (state.actor.level() == level && state.actor instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }

    private static void tickLevel(ServerLevel level) {
        List<UUID> remove = new ArrayList<>();
        for (Map.Entry<UUID, ActorState> entry : STATES.entrySet()) {
            ActorState state = entry.getValue();
            if (state.actor.level() != level) continue;
            if (state.actor.isRemoved() || !state.actor.isAlive()) {
                state.releaseActorControl();
                remove.add(entry.getKey());
            } else {
                state.tick(level, level.getGameTime());
                state.clearDonorCombatTarget();
            }
        }
        remove.forEach(STATES::remove);
    }

    private static boolean isEarthloong(LivingEntity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && EARTHLOONG_ID.equals(id.toString());
    }

    private static final class ActorState {
        private final LivingEntity actor;
        private final R01EarthloongActionController actions;
        private final R01EarthloongThreatTable threat = new R01EarthloongThreatTable();
        private long nextDecisionTick = Long.MAX_VALUE;
        private CommittedAction committed;
        private UUID closeTargetId;
        private long closeTargetSinceTick = Long.MIN_VALUE / 4;

        private ActorState(LivingEntity actor, String encounterInstanceId) {
            this.actor = actor;
            this.actions = new R01EarthloongActionController(DATA, encounterInstanceId, actor.getUUID());
        }

        private void tick(ServerLevel level, long gameTick) {
            if (committed != null) {
                tickCommitted(level, gameTick);
                return;
            }

            seedInitialThreatFromMobTarget(gameTick);
            if (threat.isEmpty()) {
                R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
                return;
            }
            if (nextDecisionTick == Long.MAX_VALUE) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }
            if (gameTick < nextDecisionTick) return;

            List<ServerPlayer> validPlayers = validPlayers(level);
            Set<UUID> ids = new HashSet<>();
            for (ServerPlayer player : validPlayers) ids.add(player.getUUID());
            UUID targetId = threat.selectTarget(ids, gameTick).orElse(null);
            ServerPlayer target = targetId == null ? null : playerById(validPlayers, targetId);
            if (target == null) return;

            updateRootBreakerProximity(target, gameTick);
            var decision = actions.select(currentPhase(), actionLegality(level, target, gameTick), gameTick);
            if (decision.reposition()) {
                R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
                if (actor instanceof Mob mob && !mob.isNoAi()) {
                    mob.getNavigation().moveTo(target, 1.0);
                }
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }
            commit(decision.action().orElseThrow(), target, gameTick);
        }

        private void seedInitialThreatFromMobTarget(long gameTick) {
            if (!(actor instanceof Mob mob)) return;
            if (mob.getTarget() instanceof ServerPlayer player
                    && player.isAlive()
                    && !player.isSpectator()
                    && player.level() == actor.level()
                    && !threat.contains(player.getUUID())) {
                threat.engageInitial(player.getUUID(), gameTick);
                scheduleDecisionIfIdle(gameTick);
            }
        }

        private void scheduleDecisionIfIdle(long gameTick) {
            if (committed == null && nextDecisionTick == Long.MAX_VALUE) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
            }
        }

        private R01EarthloongActionController.Legality actionLegality(
                ServerLevel level, ServerPlayer target, long gameTick) {
            var bindings = DATA.physicalBindingsById();
            var claw = bindings.get(R01EarthloongEncounterData.ActionId.CLAW_SWEEP);
            var rush = bindings.get(R01EarthloongEncounterData.ActionId.QUARRY_RUSH);
            Vec3 rushDirection = horizontalDirection(actor, target);
            boolean rushPathClear = actor.hasLineOfSight(target)
                    && rushDirection.lengthSqr() > 1.0e-9
                    && level.noBlockCollision(
                            actor,
                            actor.getBoundingBox().expandTowards(
                                    rushDirection.normalize().scale(rush.forwardPathBlocks())));
            var spatial = R01EarthloongSpatialAuthority.snapshot(actor, target, rushPathClear);
            int engagedClose = 0;
            for (ServerPlayer player : validPlayers(level)) {
                if (horizontalDistance(actor, player) <= 4.5) {
                    engagedClose++;
                }
            }
            boolean targetCloseLongEnough = target.getUUID().equals(closeTargetId)
                    && horizontalDistance(actor, target) <= 3.0
                    && gameTick - closeTargetSinceTick >= 40L;
            boolean rootBreakerLegal = engagedClose >= 2 || targetCloseLongEnough;

            return new R01EarthloongActionController.Legality(
                    R01EarthloongSpatialAuthority.isLegal(claw, spatial),
                    false,
                    R01EarthloongSpatialAuthority.isLegal(rush, spatial),
                    false,
                    rootBreakerLegal,
                    false,
                    false);
        }

        private void updateRootBreakerProximity(ServerPlayer target, long gameTick) {
            if (horizontalDistance(actor, target) > 3.0) {
                closeTargetId = null;
                closeTargetSinceTick = Long.MIN_VALUE / 4;
                return;
            }
            if (!target.getUUID().equals(closeTargetId)) {
                closeTargetId = target.getUUID();
                closeTargetSinceTick = gameTick;
            }
        }

        private void commit(
                R01EarthloongEncounterData.ActionId action, ServerPlayer target, long gameTick) {
            if (action == R01EarthloongEncounterData.ActionId.ROOT_BREAKER) {
                commitRootBreaker(gameTick);
                return;
            }

            var binding = DATA.physicalBindingsById().get(action);
            if (binding == null || !binding.hasDonorPresentationCandidate()) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }

            Vec3 direction = action == R01EarthloongEncounterData.ActionId.QUARRY_RUSH
                    ? horizontalDirection(actor, target) : horizontalLook(actor);
            if (direction.lengthSqr() <= 1.0e-9) direction = horizontalDirection(actor, target);
            if (direction.lengthSqr() <= 1.0e-9) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }
            direction = direction.normalize();

            var presentation = R01EarthloongDonorPresentationBridge.startTechnicalCandidate(actor, action);
            if (!presentation.accepted()) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }

            boolean previousNoAi = false;
            if (actor instanceof Mob mob) {
                previousNoAi = mob.isNoAi();
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setNoAi(true);
            }
            committed = new CommittedAction(
                    action, gameTick, R01EarthloongPhysicalTimeline.from(binding),
                    binding, direction, previousNoAi);
            nextDecisionTick = Long.MAX_VALUE;
            orientActor(direction);
            freezeHorizontalMotion();
        }

        private void commitRootBreaker(long gameTick) {
            var binding = DATA.spaceControlBindingsById().get(
                    R01EarthloongEncounterData.ActionId.ROOT_BREAKER
            );
            if (binding == null || !binding.hasDonorPresentationCandidate()) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }
            var presentation = R01EarthloongDonorPresentationBridge.startTechnicalCandidate(
                    actor,
                    R01EarthloongEncounterData.ActionId.ROOT_BREAKER
            );
            if (!presentation.accepted()) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }

            boolean previousNoAi = false;
            if (actor instanceof Mob mob) {
                previousNoAi = mob.isNoAi();
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setNoAi(true);
            }
            committed = CommittedAction.rootBreaker(
                    gameTick,
                    new R01EarthloongPhysicalTimeline(
                            binding.tellTicks(),
                            1,
                            binding.recoveryTicks()
                    ),
                    binding,
                    previousNoAi
            );
            nextDecisionTick = Long.MAX_VALUE;
            freezeHorizontalMotion();
        }

        private void tickCommitted(ServerLevel level, long gameTick) {
            CommittedAction current = committed;
            long elapsed = gameTick - current.commitTick;
            freezeHorizontalMotion();
            if (current.lockedDirection != null) {
                orientActor(current.lockedDirection);
            }

            var phase = current.timeline.phaseAtElapsedTick(elapsed);
            if (current.action == R01EarthloongEncounterData.ActionId.ROOT_BREAKER
                    && phase == R01EarthloongPhysicalTimeline.Phase.TELEGRAPH) {
                renderRootBreakerTell(level, current, elapsed);
            }

            switch (phase) {
                case TELEGRAPH, RECOVERY -> {}
                case ACTIVE -> {
                    int activeIndex = (int) (elapsed - current.timeline.tellTicks());
                    if (current.action == R01EarthloongEncounterData.ActionId.CLAW_SWEEP && activeIndex == 0) {
                        applyArcContact(level, current);
                    } else if (current.action == R01EarthloongEncounterData.ActionId.QUARRY_RUSH) {
                        moveRushAndApplyContact(level, current);
                    } else if (current.action == R01EarthloongEncounterData.ActionId.ROOT_BREAKER
                            && activeIndex == 0) {
                        applyRootBreaker(level, current);
                    }
                }
                case COMPLETE -> finishCommitted(gameTick);
            }
        }

        private void applyArcContact(ServerLevel level, CommittedAction current) {
            List<ServerPlayer> players = level.getEntitiesOfClass(
                    ServerPlayer.class,
                    actor.getBoundingBox().inflate(current.binding.maximumRange() + 1.0),
                    player -> player.isAlive() && !player.isSpectator());
            for (ServerPlayer player : players) {
                var snapshot = R01EarthloongSpatialAuthority.evaluate(
                        actor.getX(), actor.getZ(),
                        current.lockedDirection.x, current.lockedDirection.z,
                        player.getX(), player.getZ(), true);
                if (R01EarthloongSpatialAuthority.isLegal(current.binding, snapshot)
                        && current.hitPlayers.add(player.getUUID())) {
                    R01EarthloongImpactAuthority.applyConfirmedContact(actor, player, current.action);
                }
            }
        }

        private void moveRushAndApplyContact(ServerLevel level, CommittedAction current) {
            if (current.pathBlocked) return;
            Vec3 step = current.lockedDirection.scale(
                    current.binding.forwardPathBlocks() / current.timeline.activeTicks());
            AABB oldBox = actor.getBoundingBox();
            Vec3 before = actor.position();
            if (!level.noBlockCollision(actor, oldBox.expandTowards(step))) {
                current.pathBlocked = true;
                return;
            }

            actor.move(MoverType.SELF, step);
            Vec3 actual = actor.position().subtract(before);
            if (Math.hypot(actual.x, actual.z) <= 1.0e-4) {
                current.pathBlocked = true;
                return;
            }

            AABB contact = oldBox.expandTowards(actual).inflate(0.15);
            List<ServerPlayer> players = level.getEntitiesOfClass(
                    ServerPlayer.class, contact,
                    player -> player.isAlive() && !player.isSpectator());
            for (ServerPlayer player : players) {
                if (current.hitPlayers.add(player.getUUID())) {
                    R01EarthloongImpactAuthority.applyConfirmedContact(actor, player, current.action);
                }
            }
        }

        private void renderRootBreakerTell(
                ServerLevel level,
                CommittedAction current,
                long elapsed
        ) {
            if (elapsed % 2L != 0L || current.spaceBinding == null) {
                return;
            }
            double radius = current.spaceBinding.radius();
            BlockParticleOption rootDust = new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    Blocks.ROOTED_DIRT.defaultBlockState()
            );
            int samples = 28;
            for (int i = 0; i < samples; i++) {
                double angle = Math.PI * 2.0 * i / samples;
                double x = actor.getX() + Math.cos(angle) * radius;
                double z = actor.getZ() + Math.sin(angle) * radius;
                level.sendParticles(
                        rootDust,
                        x,
                        actor.getY() + 0.08,
                        z,
                        1,
                        0.03,
                        0.01,
                        0.03,
                        0.0
                );
            }
        }

        private void applyRootBreaker(ServerLevel level, CommittedAction current) {
            var binding = Objects.requireNonNull(current.spaceBinding, "spaceBinding");
            AABB search = actor.getBoundingBox().inflate(binding.radius() + 1.0, 3.0, binding.radius() + 1.0);
            List<ServerPlayer> players = level.getEntitiesOfClass(
                    ServerPlayer.class,
                    search,
                    player -> player.isAlive() && !player.isSpectator()
            );
            for (ServerPlayer player : players) {
                if (horizontalDistance(actor, player) > binding.radius()
                        || !current.hitPlayers.add(player.getUUID())) {
                    continue;
                }
                R01EarthloongImpactAuthority.applyConfirmedContact(
                        actor,
                        player,
                        current.action
                );
                ProjectPlayerPoisePressureRuntime.applyAuthoredPressure(
                        player,
                        binding.playerPoisePressure()
                );
            }

            R01EarthloongArenaBreakAuthority.breakTaggedProps(
                    level,
                    actor,
                    binding.radius()
            );

            BlockParticleOption rootDust = new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    Blocks.ROOTED_DIRT.defaultBlockState()
            );
            level.sendParticles(
                    rootDust,
                    actor.getX(),
                    actor.getY() + 0.15,
                    actor.getZ(),
                    70,
                    binding.radius() * 0.55,
                    0.18,
                    binding.radius() * 0.55,
                    0.12
            );
        }

        private void finishCommitted(long gameTick) {
            releaseActorControl();
            committed = null;
            nextDecisionTick = gameTick + DATA.decisionDelayTicks();
        }

        private void releaseActorControl() {
            R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
            if (actor instanceof Mob mob && committed != null) {
                mob.setTarget(null);
                mob.setNoAi(committed.previousNoAi);
            }
        }

        private void clearDonorCombatTarget() {
            if (actor instanceof Mob mob) {
                mob.setTarget(null);
            }
        }

        private void freezeHorizontalMotion() {
            if (actor instanceof Mob mob) mob.getNavigation().stop();
            Vec3 movement = actor.getDeltaMovement();
            actor.setDeltaMovement(0.0, movement.y, 0.0);
        }

        private void orientActor(Vec3 direction) {
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            actor.setYRot(yaw);
            actor.setYHeadRot(yaw);
            actor.setYBodyRot(yaw);
        }

        private R01EarthloongEncounterData.Phase currentPhase() {
            double fraction = ExternalActorBindingRuntime.canonicalHealthSnapshot(actor)
                    .map(snapshot -> snapshot.fraction()).orElse(1.0);
            return fraction <= DATA.phaseTwoHealthThreshold()
                    ? R01EarthloongEncounterData.Phase.TWO
                    : R01EarthloongEncounterData.Phase.ONE;
        }

        private List<ServerPlayer> validPlayers(ServerLevel level) {
            List<ServerPlayer> result = new ArrayList<>();
            for (UUID id : threat.engagedPlayerIds()) {
                Entity entity = level.getPlayerByUUID(id);
                if (entity instanceof ServerPlayer player
                        && player.isAlive() && !player.isSpectator() && player.level() == level) {
                    result.add(player);
                }
            }
            return result;
        }

        private static ServerPlayer playerById(List<ServerPlayer> players, UUID id) {
            for (ServerPlayer player : players) {
                if (player.getUUID().equals(id)) return player;
            }
            return null;
        }
    }

    private static Vec3 horizontalLook(LivingEntity actor) {
        Vec3 look = actor.getLookAngle();
        return new Vec3(look.x, 0.0, look.z);
    }

    private static Vec3 horizontalDirection(LivingEntity actor, LivingEntity target) {
        return new Vec3(target.getX() - actor.getX(), 0.0, target.getZ() - actor.getZ());
    }

    private static double horizontalDistance(LivingEntity a, LivingEntity b) {
        return Math.hypot(b.getX() - a.getX(), b.getZ() - a.getZ());
    }

    private static final class CommittedAction {
        private final R01EarthloongEncounterData.ActionId action;
        private final long commitTick;
        private final R01EarthloongPhysicalTimeline timeline;
        private final R01EarthloongEncounterData.PhysicalBindingRule binding;
        private final R01EarthloongEncounterData.SpaceControlBindingRule spaceBinding;
        private final Vec3 lockedDirection;
        private final boolean previousNoAi;
        private final Set<UUID> hitPlayers = new HashSet<>();
        private boolean pathBlocked;

        private CommittedAction(
                R01EarthloongEncounterData.ActionId action,
                long commitTick,
                R01EarthloongPhysicalTimeline timeline,
                R01EarthloongEncounterData.PhysicalBindingRule binding,
                Vec3 lockedDirection,
                boolean previousNoAi) {
            this.action = action;
            this.commitTick = commitTick;
            this.timeline = timeline;
            this.binding = binding;
            this.spaceBinding = null;
            this.lockedDirection = lockedDirection;
            this.previousNoAi = previousNoAi;
        }

        private CommittedAction(
                R01EarthloongEncounterData.ActionId action,
                long commitTick,
                R01EarthloongPhysicalTimeline timeline,
                R01EarthloongEncounterData.SpaceControlBindingRule spaceBinding,
                boolean previousNoAi
        ) {
            this.action = action;
            this.commitTick = commitTick;
            this.timeline = timeline;
            this.binding = null;
            this.spaceBinding = spaceBinding;
            this.lockedDirection = null;
            this.previousNoAi = previousNoAi;
        }

        private static CommittedAction rootBreaker(
                long commitTick,
                R01EarthloongPhysicalTimeline timeline,
                R01EarthloongEncounterData.SpaceControlBindingRule spaceBinding,
                boolean previousNoAi
        ) {
            return new CommittedAction(
                    R01EarthloongEncounterData.ActionId.ROOT_BREAKER,
                    commitTick,
                    timeline,
                    spaceBinding,
                    previousNoAi
            );
        }
    }
}
