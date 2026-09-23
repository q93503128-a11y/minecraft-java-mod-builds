package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerPoisePressureRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerShockRuntime;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public final class R01EarthloongPhysicalEncounterRuntime {
    private static final String EARTHLOONG_ID = "threateningly_mobs:the_earthloong";
    private static final List<VerificationMotionSpec> VERIFICATION_SEQUENCE = List.of(
            new VerificationMotionSpec(1, 10),
            new VerificationMotionSpec(2, 40),
            new VerificationMotionSpec(3, 25),
            new VerificationMotionSpec(4, 50)
    );
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
                        + "Claw Sweep + Quarry Rush + Lightning Furrow + Root Breaker project "
                        + "scheduling/contact active; Tail Scythe presentation remains gated.");
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

    public static double incomingDamageMultiplier(LivingEntity target) {
        ActorState state = STATES.get(target.getUUID());
        return state != null && state.actor == target && state.isStormshedActive()
                ? 0.50
                : 1.0;
    }

    /**
     * Converts one authored Earthloong into a deterministic M0 presentation fixture.
     *
     * <p>The fixture is isolated from the normal R01 action selector. Idle donor locomotion remains
     * visible, while the raw pinned donor SkillNumber 1..4 animation states can be reviewed without
     * project attack geometry, VFX or damage being layered on top.</p>
     */
    public static boolean armVerificationFixture(
            LivingEntity earthloong,
            ServerPlayer observer
    ) {
        Objects.requireNonNull(earthloong, "earthloong");
        Objects.requireNonNull(observer, "observer");
        if (!isEarthloong(earthloong)
                || !(earthloong.level() instanceof ServerLevel level)
                || observer.level() != level) {
            return false;
        }
        ActorState state = STATES.computeIfAbsent(
                earthloong.getUUID(),
                ignored -> new ActorState(
                        earthloong,
                        "r01-earthloong:" + level.dimension() + ":" + earthloong.getUUID()
                )
        );
        state.armVerificationFixture(observer, level.getGameTime());
        return true;
    }

    /**
     * Starts an explicit M0-only presentation/runtime preview on the nearest authored Earthloong.
     * Normal action selection remains gated by accepted production presentation.
     */
    public static boolean beginVerificationPreview(
            ServerPlayer player,
            R01EarthloongEncounterData.ActionId action
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");
        if (action != R01EarthloongEncounterData.ActionId.TAIL_SCYTHE
                && action != R01EarthloongEncounterData.ActionId.FORKED_HEAVEN
                && action != R01EarthloongEncounterData.ActionId.EARTHLINE_SURGE) {
            return false;
        }

        ActorState nearest = null;
        double nearestDistanceSqr = 48.0 * 48.0;
        for (ActorState state : STATES.values()) {
            if (state.actor.level() != player.level()
                    || state.actor.isRemoved()
                    || !state.actor.isAlive()) {
                continue;
            }
            double distanceSqr = state.actor.distanceToSqr(player);
            if (distanceSqr <= nearestDistanceSqr) {
                nearest = state;
                nearestDistanceSqr = distanceSqr;
            }
        }
        if (nearest == null) {
            return false;
        }
        return nearest.beginVerificationPreview(
                (ServerLevel) player.level(),
                player,
                action,
                player.level().getGameTime()
        );
    }

    /**
     * Starts one raw pinned donor motion on the nearest armed verification fixture.
     *
     * <p>This is deliberately presentation-only: no project action ID, hit geometry, VFX, damage,
     * status or arena effect is committed. It exists so attack roles can be designed from the
     * observed Earthloong motion instead of the other way around.</p>
     */
    public static boolean beginVerificationMotionPreview(
            ServerPlayer player,
            int donorSkillNumber
    ) {
        Objects.requireNonNull(player, "player");
        int animationTicks = switch (donorSkillNumber) {
            case 1 -> 10;
            case 2 -> 40;
            case 3 -> 25;
            case 4 -> 50;
            default -> -1;
        };
        if (animationTicks <= 0) {
            return false;
        }

        ActorState nearest = null;
        double nearestDistanceSqr = 48.0 * 48.0;
        for (ActorState state : STATES.values()) {
            if (state.actor.level() != player.level()
                    || state.actor.isRemoved()
                    || !state.actor.isAlive()) {
                continue;
            }
            double distanceSqr = state.actor.distanceToSqr(player);
            if (distanceSqr <= nearestDistanceSqr) {
                nearest = state;
                nearestDistanceSqr = distanceSqr;
            }
        }
        return nearest != null && nearest.beginVerificationMotionPreview(
                player,
                donorSkillNumber,
                animationTicks,
                player.level().getGameTime()
        );
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
        private UUID currentThreatTargetId;
        private UUID closeTargetId;
        private R01EarthloongEncounterData.Phase phase = R01EarthloongEncounterData.Phase.ONE;
        private boolean stormShedPending;
        private long stormShedStartTick = Long.MIN_VALUE / 4;
        private boolean stormShedPreviousNoAi;
        private long closeTargetSinceTick = Long.MIN_VALUE / 4;
        private boolean verificationFixture;
        private UUID verificationObserverId;
        private int verificationCycleIndex;
        private long verificationNextActionTick = Long.MAX_VALUE;
        private VerificationMotion verificationMotion;

        private ActorState(LivingEntity actor, String encounterInstanceId) {
            this.actor = actor;
            this.actions = new R01EarthloongActionController(DATA, encounterInstanceId, actor.getUUID());
            this.verificationFixture = false;
        }

        private void tick(ServerLevel level, long gameTick) {
            if (verificationFixture) {
                if (verificationMotion != null) {
                    tickVerificationMotion(gameTick);
                } else if (committed != null) {
                    tickCommitted(level, gameTick);
                } else {
                    R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
                    holdVerificationFixtureIdle();
                    tickVerificationSequence(level, gameTick);
                }
                return;
            }

            observeStormshedThreshold();
            updateRootBreakerProximityEveryTick(level, gameTick);
            if (isStormshedActive()) {
                tickStormshed(level, gameTick);
                return;
            }
            if (committed != null) {
                tickCommitted(level, gameTick);
                return;
            }

            if (stormShedPending) {
                beginStormshed(gameTick);
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
            if (target == null) {
                currentThreatTargetId = null;
                resetRootBreakerProximity();
                return;
            }

            if (!target.getUUID().equals(currentThreatTargetId)) {
                currentThreatTargetId = target.getUUID();
                resetRootBreakerProximity();
                updateRootBreakerProximityEveryTick(level, gameTick);
            }
            var decision = actions.select(currentPhase(), actionLegality(level, target, gameTick), gameTick);
            if (decision.reposition()) {
                R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
                if (actor instanceof Mob mob && !mob.isNoAi()) {
                    mob.getNavigation().moveTo(target, 1.0);
                }
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }
            commit(decision, target, level, gameTick);
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
            if (verificationFixture) {
                return;
            }
            if (committed == null && nextDecisionTick == Long.MAX_VALUE) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
            }
        }

        private void armVerificationFixture(ServerPlayer observer, long gameTick) {
            verificationFixture = true;
            verificationObserverId = observer.getUUID();
            verificationCycleIndex = 0;
            verificationNextActionTick = gameTick + 40L;
            verificationMotion = null;
            currentThreatTargetId = observer.getUUID();
            threat.engageInitial(observer.getUUID(), gameTick);
            resetRootBreakerProximity();
            nextDecisionTick = Long.MAX_VALUE;
            stormShedPending = false;
            stormShedStartTick = Long.MIN_VALUE / 4;
            R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
            holdVerificationFixtureIdle();
        }

        private void tickVerificationSequence(ServerLevel level, long gameTick) {
            if (verificationCycleIndex >= VERIFICATION_SEQUENCE.size()
                    || verificationObserverId == null
                    || gameTick < verificationNextActionTick) {
                return;
            }
            Entity entity = level.getPlayerByUUID(verificationObserverId);
            if (!(entity instanceof ServerPlayer observer)
                    || !observer.isAlive()
                    || observer.isSpectator()
                    || observer.level() != level) {
                return;
            }

            VerificationMotionSpec motion = VERIFICATION_SEQUENCE.get(verificationCycleIndex);
            if (!beginVerificationMotionPreview(
                    observer,
                    motion.donorSkillNumber(),
                    motion.donorAnimationTicks(),
                    gameTick
            )) {
                verificationNextActionTick = gameTick + 20L;
                return;
            }

            verificationCycleIndex++;
        }

        private boolean beginVerificationMotionPreview(
                ServerPlayer observer,
                int donorSkillNumber,
                int donorAnimationTicks,
                long gameTick
        ) {
            if (!verificationFixture
                    || verificationMotion != null
                    || committed != null
                    || isStormshedActive()
                    || stormShedPending) {
                return false;
            }

            var presentation = R01EarthloongDonorPresentationBridge.startVerificationCandidate(
                    actor,
                    donorSkillNumber,
                    donorAnimationTicks
            );
            if (!presentation.accepted()) {
                return false;
            }

            boolean previousNoAi = false;
            if (actor instanceof Mob mob) {
                previousNoAi = mob.isNoAi();
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setNoAi(true);
            }

            Vec3 towardObserver = horizontalDirection(actor, observer);
            if (towardObserver.lengthSqr() > 1.0e-9) {
                orientActor(towardObserver.normalize());
            }
            freezeHorizontalMotion();
            verificationMotion = new VerificationMotion(
                    donorSkillNumber,
                    gameTick,
                    donorAnimationTicks,
                    previousNoAi
            );
            nextDecisionTick = Long.MAX_VALUE;
            return true;
        }

        private void tickVerificationMotion(long gameTick) {
            VerificationMotion motion = verificationMotion;
            if (motion == null) {
                return;
            }
            freezeHorizontalMotion();
            if (gameTick - motion.startTick < motion.donorAnimationTicks) {
                return;
            }

            R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
            if (actor instanceof Mob mob) {
                mob.setTarget(null);
                mob.setNoAi(motion.previousNoAi);
            }
            verificationMotion = null;
            verificationNextActionTick = gameTick + 40L;
            holdVerificationFixtureIdle();
        }

        private void holdVerificationFixtureIdle() {
            if (actor instanceof Mob mob) {
                mob.setTarget(null);
                mob.setNoAi(false);
            }
        }

        private boolean beginVerificationPreview(
                ServerLevel level,
                ServerPlayer player,
                R01EarthloongEncounterData.ActionId action,
                long gameTick
        ) {
            if (!verificationFixture
                    || committed != null
                    || isStormshedActive()
                    || stormShedPending) {
                return false;
            }
            double distance = horizontalDistance(actor, player);
            threat.engageInitial(player.getUUID(), gameTick);
            currentThreatTargetId = player.getUUID();

            return switch (action) {
                case TAIL_SCYTHE -> distance <= 4.5
                        && commitTailScytheVerification(player, gameTick);
                case FORKED_HEAVEN -> distance >= DATA.forkedHeaven().minimumTargetRange()
                        && distance <= DATA.forkedHeaven().maximumTargetRange()
                        && commitForkedHeavenVerification(level, gameTick);
                case EARTHLINE_SURGE -> distance >= DATA.earthlineSurge().minimumTargetRange()
                        && distance <= DATA.earthlineSurge().maximumTargetRange()
                        && actor.hasLineOfSight(player)
                        && commitEarthlineSurgeVerification(level, player, gameTick);
                default -> false;
            };
        }

        private boolean commitTailScytheVerification(ServerPlayer target, long gameTick) {
            var binding = DATA.physicalBindingsById().get(
                    R01EarthloongEncounterData.ActionId.TAIL_SCYTHE
            );
            if (binding == null) {
                return false;
            }

            Vec3 towardPlayer = horizontalDirection(actor, target);
            if (towardPlayer.lengthSqr() <= 1.0e-9) {
                return false;
            }
            Vec3 facingAway = towardPlayer.normalize().scale(-1.0);
            var presentation = R01EarthloongDonorPresentationBridge.startVerificationCandidate(
                    actor,
                    1,
                    10
            );
            if (!presentation.accepted()) {
                return false;
            }

            boolean previousNoAi = false;
            if (actor instanceof Mob mob) {
                previousNoAi = mob.isNoAi();
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setNoAi(true);
            }
            committed = new CommittedAction(
                    R01EarthloongEncounterData.ActionId.TAIL_SCYTHE,
                    gameTick,
                    R01EarthloongPhysicalTimeline.from(binding),
                    binding,
                    facingAway,
                    previousNoAi
            );
            nextDecisionTick = Long.MAX_VALUE;
            orientActor(facingAway);
            freezeHorizontalMotion();
            return true;
        }

        private boolean commitForkedHeavenVerification(ServerLevel level, long gameTick) {
            List<ServerPlayer> players = validPlayers(level);
            Set<UUID> ids = new HashSet<>();
            for (ServerPlayer player : players) {
                ids.add(player.getUUID());
            }
            List<UUID> ranked = threat.rankedPlayers(ids, gameTick);
            List<UUID> assignments =
                    R01EarthloongPhaseTwoPatternAuthority.forkedHeavenAssignments(ranked);
            if (assignments.isEmpty()) {
                return false;
            }

            var presentation = R01EarthloongDonorPresentationBridge.startVerificationCandidate(
                    actor,
                    3,
                    25
            );
            if (!presentation.accepted()) {
                return false;
            }

            boolean previousNoAi = false;
            if (actor instanceof Mob mob) {
                previousNoAi = mob.isNoAi();
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setNoAi(true);
            }
            committed = CommittedAction.forkedHeaven(
                    gameTick,
                    assignments,
                    previousNoAi
            );
            nextDecisionTick = Long.MAX_VALUE;
            freezeHorizontalMotion();
            return true;
        }

        private boolean commitEarthlineSurgeVerification(
                ServerLevel level,
                ServerPlayer target,
                long gameTick
        ) {
            Vec3 direction = horizontalDirection(actor, target);
            if (direction.lengthSqr() <= 1.0e-9) {
                return false;
            }
            direction = direction.normalize();
            List<FurrowGroundSample> samples = buildEarthlineSamples(level, direction);
            if (samples.isEmpty()) {
                return false;
            }

            var presentation = R01EarthloongDonorPresentationBridge.startVerificationCandidate(
                    actor,
                    2,
                    40
            );
            if (!presentation.accepted()) {
                return false;
            }

            boolean previousNoAi = false;
            if (actor instanceof Mob mob) {
                previousNoAi = mob.isNoAi();
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setNoAi(true);
            }
            committed = CommittedAction.earthlineSurge(
                    gameTick,
                    direction,
                    samples,
                    actor.getX(),
                    actor.getZ(),
                    previousNoAi
            );
            nextDecisionTick = Long.MAX_VALUE;
            orientActor(direction);
            freezeHorizontalMotion();
            return true;
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
            var furrow = DATA.spaceControlBindingsById().get(
                    R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW
            );
            double targetDistance = horizontalDistance(actor, target);
            boolean lightningFurrowLegal = furrow != null
                    && targetDistance >= furrow.minimumTargetRange()
                    && targetDistance <= furrow.maximumTargetRange();

            var forked = DATA.forkedHeaven();
            boolean forkedHeavenSpatiallyLegal = false;
            for (ServerPlayer participant : validPlayers(level)) {
                double distance = horizontalDistance(actor, participant);
                if (distance >= forked.minimumTargetRange()
                        && distance <= forked.maximumTargetRange()) {
                    forkedHeavenSpatiallyLegal = true;
                    break;
                }
            }
            boolean forkedHeavenLegal = currentPhase() == R01EarthloongEncounterData.Phase.TWO
                    && forked.runtimePresentationReady()
                    && forkedHeavenSpatiallyLegal;

            var earthline = DATA.earthlineSurge();
            boolean earthlineSurgeLegal = currentPhase() == R01EarthloongEncounterData.Phase.TWO
                    && earthline.runtimePresentationReady()
                    && targetDistance >= earthline.minimumTargetRange()
                    && targetDistance <= earthline.maximumTargetRange()
                    && actor.hasLineOfSight(target);

            return new R01EarthloongActionController.Legality(
                    R01EarthloongSpatialAuthority.isLegal(claw, spatial),
                    false,
                    R01EarthloongSpatialAuthority.isLegal(rush, spatial),
                    lightningFurrowLegal,
                    rootBreakerLegal,
                    forkedHeavenLegal,
                    earthlineSurgeLegal);
        }

        private void updateRootBreakerProximityEveryTick(ServerLevel level, long gameTick) {
            if (currentThreatTargetId == null) {
                resetRootBreakerProximity();
                return;
            }
            Entity entity = level.getPlayerByUUID(currentThreatTargetId);
            if (!(entity instanceof ServerPlayer target)
                    || !target.isAlive()
                    || target.isSpectator()
                    || horizontalDistance(actor, target) > 3.0) {
                resetRootBreakerProximity();
                return;
            }
            if (!target.getUUID().equals(closeTargetId)) {
                closeTargetId = target.getUUID();
                closeTargetSinceTick = gameTick;
            }
        }

        private void resetRootBreakerProximity() {
            closeTargetId = null;
            closeTargetSinceTick = Long.MIN_VALUE / 4;
        }

        private void commit(
                R01EarthloongActionController.Decision decision,
                ServerPlayer target,
                ServerLevel level,
                long gameTick
        ) {
            R01EarthloongEncounterData.ActionId action = decision.action().orElseThrow();
            if (action == R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW) {
                commitLightningFurrow(
                        level,
                        target,
                        gameTick,
                        decision.lightningFurrowLaneCount().orElseThrow()
                );
                return;
            }
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

        private void commitLightningFurrow(
                ServerLevel level,
                ServerPlayer target,
                long gameTick,
                int laneCount
        ) {
            var binding = DATA.spaceControlBindingsById().get(
                    R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW
            );
            if (binding == null || !binding.hasDonorPresentationCandidate()) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }

            Vec3 direction = horizontalDirection(actor, target);
            if (direction.lengthSqr() <= 1.0e-9) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }
            direction = direction.normalize();

            var presentation = R01EarthloongDonorPresentationBridge.startTechnicalCandidate(
                    actor,
                    R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW
            );
            if (!presentation.accepted()) {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
                return;
            }

            List<FurrowLane> lanes = buildFurrowLanes(level, direction, laneCount, binding);
            boolean previousNoAi = false;
            if (actor instanceof Mob mob) {
                previousNoAi = mob.isNoAi();
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setNoAi(true);
            }
            committed = CommittedAction.lightningFurrow(
                    gameTick,
                    new R01EarthloongPhysicalTimeline(
                            binding.tellTicks(),
                            1,
                            binding.recoveryTicks()
                    ),
                    binding,
                    direction,
                    laneCount,
                    lanes,
                    actor.getX(),
                    actor.getZ(),
                    previousNoAi
            );
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

            if (current.action == R01EarthloongEncounterData.ActionId.FORKED_HEAVEN) {
                tickForkedHeavenVerification(level, current, elapsed, gameTick);
                return;
            }
            if (current.action == R01EarthloongEncounterData.ActionId.EARTHLINE_SURGE) {
                tickEarthlineSurgeVerification(level, current, elapsed, gameTick);
                return;
            }

            var phase = current.timeline.phaseAtElapsedTick(elapsed);
            if (phase == R01EarthloongPhysicalTimeline.Phase.TELEGRAPH) {
                if (current.action == R01EarthloongEncounterData.ActionId.ROOT_BREAKER) {
                    renderRootBreakerTell(level, current, elapsed);
                } else if (current.action
                        == R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW) {
                    renderLightningFurrowTell(level, current, elapsed);
                }
            }

            switch (phase) {
                case TELEGRAPH, RECOVERY -> {}
                case ACTIVE -> {
                    int activeIndex = (int) (elapsed - current.timeline.tellTicks());
                    if ((current.action == R01EarthloongEncounterData.ActionId.CLAW_SWEEP
                            || current.action == R01EarthloongEncounterData.ActionId.TAIL_SCYTHE)
                            && activeIndex == 0) {
                        applyArcContact(level, current);
                    } else if (current.action == R01EarthloongEncounterData.ActionId.QUARRY_RUSH) {
                        moveRushAndApplyContact(level, current);
                    } else if (current.action == R01EarthloongEncounterData.ActionId.ROOT_BREAKER
                            && activeIndex == 0) {
                        applyRootBreaker(level, current);
                    } else if (current.action
                            == R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW
                            && activeIndex == 0) {
                        applyLightningFurrow(level, current);
                    }
                }
                case COMPLETE -> finishCommitted(gameTick);
            }
        }

        private void tickForkedHeavenVerification(
                ServerLevel level,
                CommittedAction current,
                long elapsed,
                long gameTick
        ) {
            var pattern = DATA.forkedHeaven();
            for (int markerIndex = 0; markerIndex < current.forkedMarkers.length; markerIndex++) {
                ForkedMarkerState marker = current.forkedMarkers[markerIndex];
                int spawnOffset =
                        R01EarthloongPhaseTwoPatternAuthority.forkedMarkerSpawnOffsetTicks(
                                pattern,
                                markerIndex
                        );
                int impactOffset =
                        R01EarthloongPhaseTwoPatternAuthority.forkedMarkerImpactOffsetTicks(
                                pattern,
                                markerIndex
                        );

                if (!marker.created && elapsed >= spawnOffset) {
                    marker.created = true;
                    ServerPlayer assigned = playerById(
                            validPlayers(level),
                            marker.preferredAssignment
                    );
                    if (assigned == null) {
                        List<ServerPlayer> valid = validPlayers(level);
                        Set<UUID> validIds = new HashSet<>();
                        for (ServerPlayer player : valid) {
                            validIds.add(player.getUUID());
                        }
                        List<UUID> ranked = threat.rankedPlayers(validIds, gameTick);
                        assigned = ranked.isEmpty() ? null : playerById(valid, ranked.get(0));
                    }
                    if (assigned != null) {
                        Double groundY = projectLocalGround(
                                level,
                                assigned.getX(),
                                assigned.getZ(),
                                assigned.getY()
                        );
                        if (groundY != null) {
                            marker.center = new Vec3(
                                    assigned.getX(),
                                    groundY,
                                    assigned.getZ()
                            );
                            for (ServerPlayer player : validPlayers(level)) {
                                if (insideForkedMarker(player, marker.center, pattern)) {
                                    marker.insideAtTelegraph.add(player.getUUID());
                                }
                            }
                        }
                    }
                }

                if (marker.created && !marker.impacted && marker.center != null
                        && elapsed < impactOffset) {
                    renderForkedMarkerTell(level, marker.center, pattern.markerRadius());
                }

                if (!marker.impacted && elapsed >= impactOffset) {
                    marker.impacted = true;
                    if (marker.center != null) {
                        applyForkedMarker(level, current, marker);
                        renderForkedMarkerImpact(level, marker.center);
                    }
                }
            }

            if (elapsed >= R01EarthloongPhaseTwoPatternAuthority
                    .forkedCastCompleteOffsetTicks(pattern)) {
                finishCommitted(gameTick);
            }
        }

        private void applyForkedMarker(
                ServerLevel level,
                CommittedAction current,
                ForkedMarkerState marker
        ) {
            var pattern = DATA.forkedHeaven();
            for (ServerPlayer player : validPlayers(level)) {
                if (!insideForkedMarker(player, marker.center, pattern)) {
                    continue;
                }
                boolean alreadyHit = current.hitPlayers.contains(player.getUUID());
                if (!R01EarthloongPhaseTwoPatternAuthority.forkedLaterMarkerMayHitAgain(
                        alreadyHit,
                        marker.insideAtTelegraph.contains(player.getUUID())
                )) {
                    continue;
                }

                var direct = R01EarthloongImpactAuthority.applyForkedHeavenContact(
                        actor,
                        player
                );
                if (!direct.accepted()
                        || !direct.minecraftDamageApplied()
                        || direct.resolution().map(r -> r.dodged()).orElse(true)) {
                    continue;
                }
                current.hitPlayers.add(player.getUUID());
                ProjectPlayerShockRuntime.applyEarthloongBuildup(
                        actor,
                        player,
                        pattern.shockBuildup()
                );
            }
        }

        private static boolean insideForkedMarker(
                ServerPlayer player,
                Vec3 center,
                R01EarthloongEncounterData.ForkedHeavenPattern pattern
        ) {
            return Math.hypot(player.getX() - center.x, player.getZ() - center.z)
                    <= pattern.markerRadius()
                    && Math.abs(player.getY() - center.y)
                    <= pattern.playerVerticalTolerance();
        }

        private void renderForkedMarkerTell(
                ServerLevel level,
                Vec3 center,
                double radius
        ) {
            int samples = 28;
            for (int i = 0; i < samples; i++) {
                double angle = Math.PI * 2.0 * i / samples;
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        center.x + Math.cos(angle) * radius,
                        center.y + 0.08,
                        center.z + Math.sin(angle) * radius,
                        1,
                        0.015,
                        0.01,
                        0.015,
                        0.0
                );
            }
        }

        private void renderForkedMarkerImpact(ServerLevel level, Vec3 center) {
            for (double y = 0.15; y <= 4.5; y += 0.35) {
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        center.x,
                        center.y + y,
                        center.z,
                        3,
                        0.16,
                        0.08,
                        0.16,
                        0.025
                );
            }
        }

        private void tickEarthlineSurgeVerification(
                ServerLevel level,
                CommittedAction current,
                long elapsed,
                long gameTick
        ) {
            var pattern = DATA.earthlineSurge();
            int physicalTick =
                    R01EarthloongPhaseTwoPatternAuthority.earthlinePhysicalImpactOffsetTicks(
                            pattern
                    );
            int lightningTick =
                    R01EarthloongPhaseTwoPatternAuthority.earthlineLightningImpactOffsetTicks(
                            pattern
                    );

            if (elapsed < physicalTick) {
                renderEarthlinePhysicalTell(level, current);
            } else if (elapsed < lightningTick) {
                renderEarthlineLightningTell(level, current);
            }

            if (!current.primaryResolved && elapsed >= physicalTick) {
                current.primaryResolved = true;
                applyEarthlinePhysical(level, current);
                renderEarthlinePhysicalImpact(level, current);
            }
            if (!current.secondaryResolved && elapsed >= lightningTick) {
                current.secondaryResolved = true;
                applyEarthlineLightning(level, current);
                renderEarthlineLightningImpact(level, current);
            }

            if (elapsed >= R01EarthloongPhaseTwoPatternAuthority
                    .earthlineCastCompleteOffsetTicks(pattern)) {
                finishCommitted(gameTick);
            }
        }

        private void applyEarthlinePhysical(ServerLevel level, CommittedAction current) {
            for (ServerPlayer player : validPlayers(level)) {
                if (!earthlineContainsPlayer(current, player)
                        || !current.hitPlayers.add(player.getUUID())) {
                    continue;
                }
                R01EarthloongImpactAuthority.applyEarthlinePhysicalContact(actor, player);
            }
        }

        private void applyEarthlineLightning(ServerLevel level, CommittedAction current) {
            for (ServerPlayer player : validPlayers(level)) {
                if (!earthlineContainsPlayer(current, player)
                        || !current.secondaryHitPlayers.add(player.getUUID())) {
                    continue;
                }
                var direct = R01EarthloongImpactAuthority.applyEarthlineLightningContact(
                        actor,
                        player
                );
                if (!direct.accepted()
                        || !direct.minecraftDamageApplied()
                        || direct.resolution().map(r -> r.dodged()).orElse(true)) {
                    continue;
                }
                ProjectPlayerShockRuntime.applyEarthloongBuildup(
                        actor,
                        player,
                        DATA.earthlineSurge().shockBuildup()
                );
            }
        }

        private boolean earthlineContainsPlayer(
                CommittedAction current,
                ServerPlayer player
        ) {
            var pattern = DATA.earthlineSurge();
            var coordinates = R01EarthloongFurrowGeometry.coordinates(
                    current.furrowOriginX,
                    current.furrowOriginZ,
                    current.lockedDirection.x,
                    current.lockedDirection.z,
                    player.getX(),
                    player.getZ()
            );
            if (!R01EarthloongFurrowGeometry.insideLane(
                    coordinates,
                    0.0,
                    pattern.lineWidth() * 0.5,
                    pattern.lineLength()
            )) {
                return false;
            }
            FurrowGroundSample nearest = nearestSample(
                    current.earthlineSamples,
                    coordinates.longitudinal()
            );
            return nearest != null
                    && Math.abs(player.getY() - nearest.groundY())
                    <= pattern.playerVerticalTolerance();
        }

        private void renderEarthlinePhysicalTell(ServerLevel level, CommittedAction current) {
            BlockParticleOption dust = new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    Blocks.ROOTED_DIRT.defaultBlockState()
            );
            for (int i = 0; i < current.earthlineSamples.size(); i += 2) {
                FurrowGroundSample sample = current.earthlineSamples.get(i);
                level.sendParticles(
                        dust,
                        sample.x(),
                        sample.groundY() + 0.08,
                        sample.z(),
                        1,
                        0.06,
                        0.02,
                        0.06,
                        0.0
                );
            }
        }

        private void renderEarthlineLightningTell(ServerLevel level, CommittedAction current) {
            for (int i = 0; i < current.earthlineSamples.size(); i += 2) {
                FurrowGroundSample sample = current.earthlineSamples.get(i);
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        sample.x(),
                        sample.groundY() + 0.12,
                        sample.z(),
                        1,
                        0.04,
                        0.02,
                        0.04,
                        0.0
                );
            }
        }

        private void renderEarthlinePhysicalImpact(ServerLevel level, CommittedAction current) {
            BlockParticleOption dust = new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    Blocks.ROOTED_DIRT.defaultBlockState()
            );
            for (FurrowGroundSample sample : current.earthlineSamples) {
                level.sendParticles(
                        dust,
                        sample.x(),
                        sample.groundY() + 0.10,
                        sample.z(),
                        2,
                        0.10,
                        0.05,
                        0.10,
                        0.08
                );
            }
        }

        private void renderEarthlineLightningImpact(ServerLevel level, CommittedAction current) {
            for (FurrowGroundSample sample : current.earthlineSamples) {
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        sample.x(),
                        sample.groundY() + 0.18,
                        sample.z(),
                        3,
                        0.12,
                        0.08,
                        0.12,
                        0.035
                );
            }
        }

        private void applyArcContact(ServerLevel level, CommittedAction current) {
            for (ServerPlayer player : validPlayers(level)) {
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

        private void renderLightningFurrowTell(
                ServerLevel level,
                CommittedAction current,
                long elapsed
        ) {
            if (elapsed % 2L != 0L) {
                return;
            }
            for (FurrowLane lane : current.furrowLanes) {
                for (int i = 0; i < lane.samples().size(); i += 2) {
                    FurrowGroundSample sample = lane.samples().get(i);
                    level.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            sample.x(),
                            sample.groundY() + 0.10,
                            sample.z(),
                            1,
                            0.03,
                            0.02,
                            0.03,
                            0.0
                    );
                }
            }
        }

        private void applyLightningFurrow(ServerLevel level, CommittedAction current) {
            var binding = Objects.requireNonNull(current.spaceBinding, "spaceBinding");
            double halfWidth = binding.laneWidth() * 0.5;
            for (ServerPlayer player : validPlayers(level)) {
                if (current.hitPlayers.contains(player.getUUID())
                        || !furrowContainsPlayer(current, player, halfWidth, binding.laneLength())) {
                    continue;
                }

                var direct = R01EarthloongImpactAuthority.applyConfirmedContact(
                        actor,
                        player,
                        current.action
                );
                if (!direct.accepted()
                        || !direct.minecraftDamageApplied()
                        || direct.resolution().map(r -> r.dodged()).orElse(true)) {
                    continue;
                }

                current.hitPlayers.add(player.getUUID());
                ProjectPlayerShockRuntime.applyEarthloongBuildup(
                        actor,
                        player,
                        binding.shockBuildup()
                );
            }
        }

        private boolean furrowContainsPlayer(
                CommittedAction current,
                ServerPlayer player,
                double halfWidth,
                double laneLength
        ) {
            var coordinates = R01EarthloongFurrowGeometry.coordinates(
                    current.furrowOriginX,
                    current.furrowOriginZ,
                    current.lockedDirection.x,
                    current.lockedDirection.z,
                    player.getX(),
                    player.getZ()
            );
            var pattern = DATA.lightningFurrow();
            for (FurrowLane lane : current.furrowLanes) {
                if (!R01EarthloongFurrowGeometry.insideLane(
                        coordinates,
                        lane.centerOffset(),
                        halfWidth,
                        laneLength
                )) {
                    continue;
                }
                FurrowGroundSample nearest = nearestSample(
                        lane.samples(),
                        coordinates.longitudinal()
                );
                if (nearest != null
                        && Math.abs(player.getY() - nearest.groundY())
                                <= pattern.playerVerticalTolerance()) {
                    return true;
                }
            }
            return false;
        }

        private static FurrowGroundSample nearestSample(
                List<FurrowGroundSample> samples,
                double longitudinal
        ) {
            FurrowGroundSample best = null;
            double bestDelta = Double.MAX_VALUE;
            for (FurrowGroundSample sample : samples) {
                double delta = Math.abs(sample.longitudinal() - longitudinal);
                if (delta < bestDelta) {
                    best = sample;
                    bestDelta = delta;
                }
            }
            return bestDelta <= 0.35 ? best : null;
        }

        private List<FurrowGroundSample> buildEarthlineSamples(
                ServerLevel level,
                Vec3 direction
        ) {
            var pattern = DATA.earthlineSurge();
            List<FurrowGroundSample> samples = new ArrayList<>();
            Double previousGroundY = null;
            for (double longitudinal = 0.0;
                    longitudinal <= pattern.lineLength() + 1.0e-9;
                    longitudinal += 0.50) {
                double x = actor.getX() + direction.x * longitudinal;
                double z = actor.getZ() + direction.z * longitudinal;
                Double groundY = projectLocalGround(
                        level,
                        x,
                        z,
                        previousGroundY == null ? actor.getY() : previousGroundY
                );
                if (groundY == null) {
                    break;
                }
                if (previousGroundY != null
                        && Math.abs(groundY - previousGroundY) > pattern.maximumGroundStep()) {
                    break;
                }
                AABB standingColumn = new AABB(
                        x - 0.12,
                        groundY + 0.05,
                        z - 0.12,
                        x + 0.12,
                        groundY + 1.80,
                        z + 0.12
                );
                if (!level.noBlockCollision(actor, standingColumn)) {
                    break;
                }
                samples.add(new FurrowGroundSample(longitudinal, x, groundY, z));
                previousGroundY = groundY;
            }
            return List.copyOf(samples);
        }

        private List<FurrowLane> buildFurrowLanes(
                ServerLevel level,
                Vec3 direction,
                int laneCount,
                R01EarthloongEncounterData.SpaceControlBindingRule binding
        ) {
            List<FurrowLane> lanes = new ArrayList<>();
            Vec3 lateral = new Vec3(-direction.z, 0.0, direction.x);
            for (double offset : DATA.lightningFurrow().offsetsForLaneCount(laneCount)) {
                List<FurrowGroundSample> samples = new ArrayList<>();
                Double previousGroundY = null;
                for (double longitudinal = 0.0;
                        longitudinal <= binding.laneLength() + 1.0e-9;
                        longitudinal += 0.50) {
                    double x = actor.getX()
                            + direction.x * longitudinal
                            + lateral.x * offset;
                    double z = actor.getZ()
                            + direction.z * longitudinal
                            + lateral.z * offset;
                    Double groundY = projectLocalGround(
                            level,
                            x,
                            z,
                            previousGroundY == null ? actor.getY() : previousGroundY
                    );
                    if (groundY == null) {
                        break;
                    }
                    if (previousGroundY != null
                            && Math.abs(groundY - previousGroundY)
                                    > DATA.lightningFurrow().maximumGroundStep()) {
                        break;
                    }

                    AABB standingColumn = new AABB(
                            x - 0.12,
                            groundY + 0.05,
                            z - 0.12,
                            x + 0.12,
                            groundY + 1.80,
                            z + 0.12
                    );
                    if (!level.noBlockCollision(actor, standingColumn)) {
                        break;
                    }

                    samples.add(new FurrowGroundSample(
                            longitudinal,
                            x,
                            groundY,
                            z
                    ));
                    previousGroundY = groundY;
                }
                lanes.add(new FurrowLane(offset, List.copyOf(samples)));
            }
            return List.copyOf(lanes);
        }

        private static Double projectLocalGround(
                ServerLevel level,
                double x,
                double z,
                double referenceY
        ) {
            BlockPos origin = BlockPos.containing(x, referenceY, z);
            Double best = null;
            double bestDelta = Double.MAX_VALUE;
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos pos = origin.offset(0, dy, 0);
                VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                if (shape.isEmpty()) {
                    continue;
                }
                double topY = pos.getY() + shape.max(Direction.Axis.Y);
                double delta = Math.abs(topY - referenceY);
                if (delta < bestDelta) {
                    best = topY;
                    bestDelta = delta;
                }
            }
            return best;
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
            for (ServerPlayer player : validPlayers(level)) {
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
            if (verificationFixture) {
                nextDecisionTick = Long.MAX_VALUE;
                verificationNextActionTick = gameTick + 40L;
                holdVerificationFixtureIdle();
            } else if (stormShedPending) {
                beginStormshed(gameTick);
            } else {
                nextDecisionTick = gameTick + DATA.decisionDelayTicks();
            }
        }

        private void observeStormshedThreshold() {
            if (phase != R01EarthloongEncounterData.Phase.ONE
                    || stormShedPending
                    || isStormshedActive()) {
                return;
            }
            double fraction = ExternalActorBindingRuntime.canonicalHealthSnapshot(actor)
                    .map(snapshot -> snapshot.fraction())
                    .orElse(1.0);
            if (fraction <= DATA.phaseTwoHealthThreshold()) {
                stormShedPending = true;
            }
        }

        private void beginStormshed(long gameTick) {
            if (!stormShedPending || isStormshedActive()) {
                return;
            }
            stormShedStartTick = gameTick;
            nextDecisionTick = Long.MAX_VALUE;
            R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
            if (actor instanceof Mob mob) {
                stormShedPreviousNoAi = mob.isNoAi();
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setNoAi(true);
            }
            freezeHorizontalMotion();
        }

        private void tickStormshed(ServerLevel level, long gameTick) {
            long elapsed = gameTick - stormShedStartTick;
            freezeHorizontalMotion();
            if (elapsed < 28L) {
                if (elapsed % 2L == 0L) {
                    level.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            actor.getX(),
                            actor.getY() + actor.getBbHeight() * 0.65,
                            actor.getZ(),
                            8,
                            Math.max(0.5, actor.getBbWidth() * 0.55),
                            Math.max(0.5, actor.getBbHeight() * 0.35),
                            Math.max(0.5, actor.getBbWidth() * 0.55),
                            0.02
                    );
                }
                return;
            }

            phase = R01EarthloongEncounterData.Phase.TWO;
            stormShedPending = false;
            stormShedStartTick = Long.MIN_VALUE / 4;
            if (actor instanceof Mob mob) {
                mob.setNoAi(stormShedPreviousNoAi);
                mob.setTarget(null);
            }
            nextDecisionTick = gameTick + DATA.decisionDelayTicks();
        }

        private boolean isStormshedActive() {
            return stormShedStartTick > Long.MIN_VALUE / 8;
        }

        private void releaseActorControl() {
            R01EarthloongDonorPresentationBridge.resetTechnicalCandidate(actor);
            if (verificationMotion != null && actor instanceof Mob mob) {
                mob.setTarget(null);
                mob.setNoAi(verificationMotion.previousNoAi);
                verificationMotion = null;
            }
            if (verificationFixture) {
                holdVerificationFixtureIdle();
            } else if (actor instanceof Mob mob && committed != null) {
                mob.setTarget(null);
                mob.setNoAi(committed.previousNoAi);
            } else if (actor instanceof Mob mob && isStormshedActive()) {
                mob.setTarget(null);
                mob.setNoAi(stormShedPreviousNoAi);
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
            return phase;
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

    private record VerificationMotionSpec(
            int donorSkillNumber,
            int donorAnimationTicks
    ) {
        private VerificationMotionSpec {
            if (donorSkillNumber < 1 || donorSkillNumber > 4 || donorAnimationTicks <= 0) {
                throw new IllegalArgumentException("Invalid Earthloong verification motion.");
            }
        }
    }

    private static final class VerificationMotion {
        private final int donorSkillNumber;
        private final long startTick;
        private final int donorAnimationTicks;
        private final boolean previousNoAi;

        private VerificationMotion(
                int donorSkillNumber,
                long startTick,
                int donorAnimationTicks,
                boolean previousNoAi
        ) {
            this.donorSkillNumber = donorSkillNumber;
            this.startTick = startTick;
            this.donorAnimationTicks = donorAnimationTicks;
            this.previousNoAi = previousNoAi;
        }
    }

    private record FurrowGroundSample(
            double longitudinal,
            double x,
            double groundY,
            double z
    ) {}

    private record FurrowLane(
            double centerOffset,
            List<FurrowGroundSample> samples
    ) {}

    private static final class ForkedMarkerState {
        private final UUID preferredAssignment;
        private final Set<UUID> insideAtTelegraph = new HashSet<>();
        private Vec3 center;
        private boolean created;
        private boolean impacted;

        private ForkedMarkerState(UUID preferredAssignment) {
            this.preferredAssignment = Objects.requireNonNull(
                    preferredAssignment,
                    "preferredAssignment"
            );
        }
    }

    private static final class CommittedAction {
        private final R01EarthloongEncounterData.ActionId action;
        private final long commitTick;
        private final R01EarthloongPhysicalTimeline timeline;
        private final R01EarthloongEncounterData.PhysicalBindingRule binding;
        private final R01EarthloongEncounterData.SpaceControlBindingRule spaceBinding;
        private final Vec3 lockedDirection;
        private final boolean previousNoAi;
        private final int furrowLaneCount;
        private final List<FurrowLane> furrowLanes;
        private final double furrowOriginX;
        private final double furrowOriginZ;
        private final Set<UUID> hitPlayers = new HashSet<>();
        private final Set<UUID> secondaryHitPlayers = new HashSet<>();
        private List<FurrowGroundSample> earthlineSamples = List.of();
        private ForkedMarkerState[] forkedMarkers = new ForkedMarkerState[0];
        private boolean primaryResolved;
        private boolean secondaryResolved;
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
            this.furrowLaneCount = 0;
            this.furrowLanes = List.of();
            this.furrowOriginX = 0.0;
            this.furrowOriginZ = 0.0;
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
            this.furrowLaneCount = 0;
            this.furrowLanes = List.of();
            this.furrowOriginX = 0.0;
            this.furrowOriginZ = 0.0;
        }

        private CommittedAction(
                long commitTick,
                R01EarthloongPhysicalTimeline timeline,
                R01EarthloongEncounterData.SpaceControlBindingRule spaceBinding,
                Vec3 lockedDirection,
                int furrowLaneCount,
                List<FurrowLane> furrowLanes,
                double furrowOriginX,
                double furrowOriginZ,
                boolean previousNoAi
        ) {
            this.action = R01EarthloongEncounterData.ActionId.LIGHTNING_FURROW;
            this.commitTick = commitTick;
            this.timeline = timeline;
            this.binding = null;
            this.spaceBinding = spaceBinding;
            this.lockedDirection = lockedDirection;
            this.previousNoAi = previousNoAi;
            this.furrowLaneCount = furrowLaneCount;
            this.furrowLanes = List.copyOf(furrowLanes);
            this.furrowOriginX = furrowOriginX;
            this.furrowOriginZ = furrowOriginZ;
        }

        private static CommittedAction lightningFurrow(
                long commitTick,
                R01EarthloongPhysicalTimeline timeline,
                R01EarthloongEncounterData.SpaceControlBindingRule spaceBinding,
                Vec3 lockedDirection,
                int laneCount,
                List<FurrowLane> furrowLanes,
                double furrowOriginX,
                double furrowOriginZ,
                boolean previousNoAi
        ) {
            return new CommittedAction(
                    commitTick,
                    timeline,
                    spaceBinding,
                    lockedDirection,
                    laneCount,
                    furrowLanes,
                    furrowOriginX,
                    furrowOriginZ,
                    previousNoAi
            );
        }

        private CommittedAction(
                R01EarthloongEncounterData.ActionId action,
                long commitTick,
                Vec3 lockedDirection,
                double originX,
                double originZ,
                boolean previousNoAi
        ) {
            this.action = action;
            this.commitTick = commitTick;
            this.timeline = null;
            this.binding = null;
            this.spaceBinding = null;
            this.lockedDirection = lockedDirection;
            this.previousNoAi = previousNoAi;
            this.furrowLaneCount = 0;
            this.furrowLanes = List.of();
            this.furrowOriginX = originX;
            this.furrowOriginZ = originZ;
        }

        private static CommittedAction forkedHeaven(
                long commitTick,
                List<UUID> assignments,
                boolean previousNoAi
        ) {
            CommittedAction result = new CommittedAction(
                    R01EarthloongEncounterData.ActionId.FORKED_HEAVEN,
                    commitTick,
                    null,
                    0.0,
                    0.0,
                    previousNoAi
            );
            result.forkedMarkers = new ForkedMarkerState[assignments.size()];
            for (int i = 0; i < assignments.size(); i++) {
                result.forkedMarkers[i] = new ForkedMarkerState(assignments.get(i));
            }
            return result;
        }

        private static CommittedAction earthlineSurge(
                long commitTick,
                Vec3 lockedDirection,
                List<FurrowGroundSample> samples,
                double originX,
                double originZ,
                boolean previousNoAi
        ) {
            CommittedAction result = new CommittedAction(
                    R01EarthloongEncounterData.ActionId.EARTHLINE_SURGE,
                    commitTick,
                    lockedDirection,
                    originX,
                    originZ,
                    previousNoAi
            );
            result.earthlineSamples = List.copyOf(samples);
            return result;
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
