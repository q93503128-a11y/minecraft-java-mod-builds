package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.HunterQuickstepVolleyRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public final class HunterQuickstepVolleyRuntime {
    public static final int ACTION_TICKS = 10;
    public static final int DASH_STEPS = 6;
    public static final long CAST_STALE_TICKS = 100L;
    public static final long INTENT_STALE_TICKS = 4L;
    public static final double PER_PROJECTILE_POISE_COEFFICIENT =
            HunterQuickstepVolleyRules.WHOLE_ACTION_POISE_COEFFICIENT
                    / HunterQuickstepVolleyRules.PROJECTILE_COUNT;

    private static final double INPUT_EPSILON = 1.0e-4;
    private static final double MAX_PACKET_COMPONENT = 1.001;

    private static final ConcurrentHashMap<UUID, Long> LAST_INTENT_SEQUENCE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingIntent> PENDING_INTENTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, AcceptedCast> CASTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ActiveDash> DASHES = new ConcurrentHashMap<>();

    private HunterQuickstepVolleyRuntime() {
    }

    public static boolean recordMovementIntent(ServerPlayer player, float forwardIntent, float strafeIntent, long sequence) {
        Objects.requireNonNull(player, "player");
        if (!isHunter(player) || !validIntent(forwardIntent, strafeIntent) || sequence < 0L) {
            return false;
        }
        UUID playerId = player.getUUID();
        long previous = LAST_INTENT_SEQUENCE.getOrDefault(playerId, -1L);
        if (sequence <= previous) {
            return false;
        }
        LAST_INTENT_SEQUENCE.put(playerId, sequence);
        long nowTick = player.level().getGameTime();
        PENDING_INTENTS.put(playerId, new PendingIntent(
                player.level(),
                MovementIntent.resolve(forwardIntent, strafeIntent),
                nowTick
        ));
        return true;
    }

    public static boolean canActivate(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!isHunter(player)
                || player.level().isClientSide()
                || !player.isAlive()
                || player.isSpectator()
                || !ProjectPlayerActionRuntime.canStartAction(player)) {
            return false;
        }
        var build = CombatStateServices.combatBuilds().build(player.getUUID()).orElse(null);
        return build != null
                && HunterQuickstepVolleyRules.supportsCurrentProductionRangedWeapon(
                        build.equipment().weaponFamily()
                );
    }

    public static AcceptedCastResult onAcceptedCast(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!canActivate(player)) {
            return AcceptedCastResult.rejected();
        }

        long nowTick = player.level().getGameTime();
        UUID playerId = player.getUUID();
        UUID quarryAtCast = HunterSkillRuntime.quarryId(player).orElse(null);

        var action = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        ProjectSpellSpec.HUNTER_QUICKSTEP_VOLLEY_ID,
                        ACTION_TICKS,
                        ACTION_TICKS,
                        1.0
                )
        );
        if (!action.accepted()) {
            throw new IllegalStateException(
                    "Quickstep Volley passed preflight but could not claim its action window."
            );
        }

        boolean empowered = HunterSkillRuntime.consumeFocusSpenderIfFull(player);
        AcceptedCast cast = new AcceptedCast(
                (ServerLevel) player.level(),
                player.position(),
                quarryAtCast,
                empowered,
                nowTick
        );
        CASTS.put(playerId, cast);

        PendingIntent pending = PENDING_INTENTS.remove(playerId);
        Vec3 direction = Vec3.ZERO;
        if (pending != null
                && pending.level() == player.level()
                && nowTick - pending.receivedAtTick() <= INTENT_STALE_TICKS) {
            direction = worldDirection(player, pending.intent());
        }

        double requestedDash = empowered
                ? HunterQuickstepVolleyRules.EMPOWERED_DASH_BLOCKS
                : HunterQuickstepVolleyRules.BASE_DASH_BLOCKS;
        if (direction.lengthSqr() > 1.0e-9) {
            ActiveDash dash = new ActiveDash(
                    (ServerLevel) player.level(),
                    direction,
                    requestedDash / DASH_STEPS,
                    0,
                    Long.MIN_VALUE / 4
            );
            DASHES.put(playerId, dash);
            advanceOneStep(player, dash);
        } else {
            DASHES.remove(playerId);
        }

        return new AcceptedCastResult(true, empowered, requestedDash, direction.lengthSqr() > 1.0e-9);
    }

    public static HitResult applyProjectileHit(ServerPlayer hunter, LivingEntity target, Vec3 hitPosition) {
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(hitPosition, "hitPosition");

        if (!isHunter(hunter)
                || hunter.level().isClientSide()
                || hunter.level() != target.level()) {
            return HitResult.rejected();
        }

        long nowTick = hunter.level().getGameTime();
        AcceptedCast cast = CASTS.get(hunter.getUUID());
        if (cast == null
                || cast.level() != hunter.level()
                || nowTick - cast.acceptedAtTick() > CAST_STALE_TICKS) {
            return HitResult.rejected();
        }

        int priorHits = cast.targetHits().getOrDefault(target.getUUID(), 0);
        if (priorHits >= HunterQuickstepVolleyRules.PROJECTILE_COUNT) {
            return HitResult.rejected();
        }

        var build = CombatStateServices.combatBuilds().build(hunter.getUUID()).orElse(null);
        var targetSnapshot = ExternalActorBindingRuntime.projectTargetSnapshot(target, nowTick).orElse(null);
        if (build == null || targetSnapshot == null) {
            return HitResult.rejected();
        }

        boolean weakPointHit =
                ExternalActorBindingRuntime.isAuthoredWeakPointHit(
                        target,
                        hitPosition
                );
        double weakPointMultiplier =
                HunterRootPassiveEffects.weakPointMultiplier(
                        hunter,
                        weakPointHit
                );

        var source = build.damageSource(ProjectImpactTransaction.DamageSchool.PHYSICAL);
        double damage = ProjectImpactTransaction.resolveDirectDamage(
                new ProjectImpactTransaction.DirectDamageRequest(
                        source,
                        targetSnapshot,
                        ProjectImpactTransaction.DamageSchool.PHYSICAL,
                        HunterQuickstepVolleyRules.PER_PROJECTILE_ACTION_COEFFICIENT,
                        1.0,
                        1.0
                )
        ).finalDamage();

        double poiseDamage = targetSnapshot.poiseMax() > 0.0
                ? ProjectImpactTransaction.resolvePoise(
                        new ProjectImpactTransaction.PoiseRequest(
                                targetSnapshot.poiseMax(),
                                targetSnapshot.poiseMax(),
                                source.poiseOutputMultiplier(),
                                PER_PROJECTILE_POISE_COEFFICIENT,
                                1.0,
                                1.0
                        )
                ).poiseDamage()
                : 0.0;

        if (!ProjectMinecraftDamageApplicator.applyDirectPhysical(hunter, target, damage)) {
            return HitResult.rejected();
        }

        cast.targetHits().put(target.getUUID(), priorHits + 1);

        boolean poiseBreak = false;
        if (poiseDamage > 0.0) {
            var poise = ExternalActorBindingRuntime
                    .applyProjectPoiseDamage(target, poiseDamage, nowTick)
                    .orElse(null);
            poiseBreak = poise != null && poise.breakTriggered();
        }

        double distance = cast.origin().distanceTo(hitPosition);
        var hunterHit = HunterSkillRuntime.onRangedSkillHit(
                hunter,
                target,
                distance,
                weakPointHit,
                poiseBreak,
                nowTick
        );
        CombatStateServices.markCombatActivity(hunter.getUUID(), nowTick);

        boolean activeQuarryCharge = false;
        if (!cast.activeQuarryChargePublished()
                && cast.quarryAtCast() != null
                && cast.quarryAtCast().equals(target.getUUID())) {
            ProjectUltimateChargeRuntime.recordHunterActiveQuarryHit(hunter, target);
            cast.publishActiveQuarryCharge();
            activeQuarryCharge = true;
        }

        return new HitResult(
                true,
                cast.empowered(),
                priorHits + 1,
                damage,
                poiseDamage,
                poiseBreak,
                hunterHit.focusAfter(),
                activeQuarryCharge
        );
    }

    public static boolean canEmpoweredPierce(ServerPlayer hunter, LivingEntity target) {
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(target, "target");
        AcceptedCast cast = CASTS.get(hunter.getUUID());
        if (cast == null
                || !cast.empowered()
                || cast.level() != hunter.level()
                || hunter.level() != target.level()
                || !isHunter(hunter)) {
            return false;
        }
        return ExternalActorBindingRuntime.combatProfile(target)
                .map(profile -> profile.reactionCapabilities().hunterQuickstepPierceable())
                .orElse(false);
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        CASTS.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            AcceptedCast cast = entry.getValue();
            if (player == null
                    || !player.isAlive()
                    || player.level() != cast.level()
                    || !isHunter(player)) {
                return true;
            }
            return player.level().getGameTime() - cast.acceptedAtTick() > CAST_STALE_TICKS;
        });

        PENDING_INTENTS.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PendingIntent pending = entry.getValue();
            return player == null
                    || player.level() != pending.level()
                    || player.level().getGameTime() - pending.receivedAtTick() > INTENT_STALE_TICKS;
        });

        DASHES.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ActiveDash dash = entry.getValue();
            if (player == null
                    || !player.isAlive()
                    || player.level() != dash.level()
                    || !isHunter(player)
                    || ProjectPlayerActionRuntime.hardReactionActive(
                            player.getUUID(),
                            player.level().getGameTime()
                    )) {
                return true;
            }
            advanceOneStep(player, dash);
            return dash.completed();
        });
    }

    public static void reset(UUID playerId) {
        if (playerId == null) {
            return;
        }
        LAST_INTENT_SEQUENCE.remove(playerId);
        PENDING_INTENTS.remove(playerId);
        CASTS.remove(playerId);
        DASHES.remove(playerId);
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    private static void advanceOneStep(ServerPlayer player, ActiveDash dash) {
        if (dash.completed()) {
            return;
        }
        long nowTick = player.level().getGameTime();
        if (dash.lastStepTick() == nowTick) {
            return;
        }
        player.move(MoverType.SELF, dash.direction().scale(dash.blocksPerStep()));
        dash.advance(nowTick);
    }

    private static Vec3 worldDirection(ServerPlayer player, MovementIntent intent) {
        if (intent.noDirectionalInput()) {
            return Vec3.ZERO;
        }

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        if (forward.lengthSqr() <= 1.0e-9) {
            double yaw = Math.toRadians(player.getYRot());
            forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        } else {
            forward = forward.normalize();
        }

        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 combined = forward.scale(intent.forward()).add(right.scale(intent.strafe()));
        return combined.lengthSqr() <= 1.0e-9 ? Vec3.ZERO : combined.normalize();
    }

    private static boolean validIntent(float forward, float strafe) {
        return Float.isFinite(forward)
                && Float.isFinite(strafe)
                && Math.abs(forward) <= MAX_PACKET_COMPONENT
                && Math.abs(strafe) <= MAX_PACKET_COMPONENT;
    }

    private static boolean isHunter(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.HUNTER::equals)
                .isPresent();
    }

    public record AcceptedCastResult(
            boolean accepted,
            boolean empowered,
            double requestedDashBlocks,
            boolean directionalDash
    ) {
        public AcceptedCastResult {
            if (!Double.isFinite(requestedDashBlocks) || requestedDashBlocks < 0.0) {
                throw new IllegalArgumentException("Invalid Quickstep Volley dash result.");
            }
            if (!accepted && (empowered || requestedDashBlocks != 0.0 || directionalDash)) {
                throw new IllegalArgumentException("Rejected Quickstep Volley cast cannot carry state.");
            }
        }

        public static AcceptedCastResult rejected() {
            return new AcceptedCastResult(false, false, 0.0, false);
        }
    }

    public record HitResult(
            boolean accepted,
            boolean empowered,
            int targetHitCount,
            double damage,
            double poiseDamage,
            boolean poiseBreak,
            int focusAfter,
            boolean activeQuarryChargePublished
    ) {
        public HitResult {
            if (targetHitCount < 0
                    || targetHitCount > HunterQuickstepVolleyRules.PROJECTILE_COUNT
                    || !Double.isFinite(damage)
                    || damage < 0.0
                    || !Double.isFinite(poiseDamage)
                    || poiseDamage < 0.0
                    || focusAfter < 0
                    || focusAfter > HunterQuarryFocusRuntimeState.MAX_FOCUS) {
                throw new IllegalArgumentException("Invalid Quickstep Volley hit result.");
            }
            if (!accepted && (empowered
                    || targetHitCount != 0
                    || damage != 0.0
                    || poiseDamage != 0.0
                    || poiseBreak
                    || focusAfter != 0
                    || activeQuarryChargePublished)) {
                throw new IllegalArgumentException("Rejected Quickstep Volley hit cannot carry state.");
            }
        }

        public static HitResult rejected() {
            return new HitResult(false, false, 0, 0.0, 0.0, false, 0, false);
        }
    }

    private record PendingIntent(
            net.minecraft.world.level.Level level,
            MovementIntent intent,
            long receivedAtTick
    ) {
    }

    private record MovementIntent(double forward, double strafe, boolean noDirectionalInput) {
        private static MovementIntent resolve(float rawForward, float rawStrafe) {
            double magnitude = Math.hypot(rawForward, rawStrafe);
            if (magnitude <= INPUT_EPSILON) {
                return new MovementIntent(0.0, 0.0, true);
            }
            return new MovementIntent(rawForward / magnitude, rawStrafe / magnitude, false);
        }
    }

    private static final class AcceptedCast {
        private final ServerLevel level;
        private final Vec3 origin;
        private final UUID quarryAtCast;
        private final boolean empowered;
        private final long acceptedAtTick;
        private final Map<UUID, Integer> targetHits = new HashMap<>();
        private boolean activeQuarryChargePublished;

        private AcceptedCast(
                ServerLevel level,
                Vec3 origin,
                UUID quarryAtCast,
                boolean empowered,
                long acceptedAtTick
        ) {
            this.level = level;
            this.origin = origin;
            this.quarryAtCast = quarryAtCast;
            this.empowered = empowered;
            this.acceptedAtTick = acceptedAtTick;
        }

        private ServerLevel level() { return level; }
        private Vec3 origin() { return origin; }
        private UUID quarryAtCast() { return quarryAtCast; }
        private boolean empowered() { return empowered; }
        private long acceptedAtTick() { return acceptedAtTick; }
        private Map<UUID, Integer> targetHits() { return targetHits; }
        private boolean activeQuarryChargePublished() { return activeQuarryChargePublished; }
        private void publishActiveQuarryCharge() { activeQuarryChargePublished = true; }
    }

    private static final class ActiveDash {
        private final ServerLevel level;
        private final Vec3 direction;
        private final double blocksPerStep;
        private int completedSteps;
        private long lastStepTick;

        private ActiveDash(
                ServerLevel level,
                Vec3 direction,
                double blocksPerStep,
                int completedSteps,
                long lastStepTick
        ) {
            this.level = level;
            this.direction = direction;
            this.blocksPerStep = blocksPerStep;
            this.completedSteps = completedSteps;
            this.lastStepTick = lastStepTick;
        }

        private ServerLevel level() { return level; }
        private Vec3 direction() { return direction; }
        private double blocksPerStep() { return blocksPerStep; }
        private long lastStepTick() { return lastStepTick; }
        private void advance(long nowTick) { completedSteps++; lastStepTick = nowTick; }
        private boolean completed() { return completedSteps >= DASH_STEPS; }
    }
}
