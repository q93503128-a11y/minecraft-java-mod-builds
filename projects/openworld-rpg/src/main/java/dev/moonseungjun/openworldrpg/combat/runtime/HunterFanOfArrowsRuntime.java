package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.HunterFanOfArrowsRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.progression.r01.R01ClassInsightService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HunterFanOfArrowsRuntime {
    public static final int ACTION_TICKS = 10;
    public static final long CAST_STALE_TICKS = 100L;

    private static final ConcurrentHashMap<UUID, AcceptedCast> CASTS =
            new ConcurrentHashMap<>();

    private HunterFanOfArrowsRuntime() {}

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
                && HunterFanOfArrowsRules.supportsCurrentProductionRangedWeapon(
                        build.equipment().weaponFamily()
                );
    }

    public static AcceptedCastResult onAcceptedCast(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!canActivate(player)) {
            return AcceptedCastResult.rejected();
        }

        long nowTick = player.level().getGameTime();
        var action = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        ProjectSpellSpec.HUNTER_FAN_OF_ARROWS_ID,
                        ACTION_TICKS,
                        ACTION_TICKS,
                        1.0
                )
        );
        if (!action.accepted()) {
            throw new IllegalStateException(
                    "Fan of Arrows passed preflight but could not claim its action window."
            );
        }

        boolean empowered = HunterSkillRuntime.consumeFocusSpenderIfFull(player);
        CASTS.put(
                player.getUUID(),
                new AcceptedCast(
                        (ServerLevel) player.level(),
                        player.position(),
                        HunterSkillRuntime.quarryId(player).orElse(null),
                        empowered,
                        nowTick
                )
        );
        return new AcceptedCastResult(
                true,
                empowered,
                HunterFanOfArrowsRules.projectileCount(empowered)
        );
    }

    public static int projectileCountForLaunch(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        long nowTick = player.level().getGameTime();
        AcceptedCast cast = CASTS.get(player.getUUID());
        if (cast == null
                || cast.level() != player.level()
                || nowTick - cast.acceptedAtTick() > CAST_STALE_TICKS) {
            return HunterFanOfArrowsRules.BASE_PROJECTILE_COUNT;
        }
        return HunterFanOfArrowsRules.projectileCount(cast.empowered());
    }

    public static HitResult applyProjectileHit(
            ServerPlayer hunter,
            LivingEntity target,
            Vec3 hitPosition
    ) {
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
        if (priorHits >= HunterFanOfArrowsRules.PER_TARGET_HIT_CAP) {
            return HitResult.rejected();
        }

        var build = CombatStateServices.combatBuilds().build(hunter.getUUID()).orElse(null);
        var targetSnapshot = ExternalActorBindingRuntime
                .projectTargetSnapshot(target, nowTick)
                .orElse(null);
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
        double actionCoefficient =
                HunterFanOfArrowsRules.perProjectileActionCoefficient(cast.empowered());
        double poiseCoefficient =
                HunterFanOfArrowsRules.perProjectilePoiseCoefficient(cast.empowered());
        double damage = ProjectImpactTransaction.resolveDirectDamage(
                new ProjectImpactTransaction.DirectDamageRequest(
                        source,
                        targetSnapshot,
                        ProjectImpactTransaction.DamageSchool.PHYSICAL,
                        actionCoefficient,
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
                                poiseCoefficient,
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
        R01ClassInsightService.recordHunterCrownedMarkHit(
                hunter,
                target,
                cast.quarryAtCast(),
                cast.empowered(),
                weakPointHit
        );

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
                actionCoefficient,
                poiseCoefficient,
                damage,
                poiseDamage,
                poiseBreak,
                hunterHit.focusAfter(),
                activeQuarryCharge
        );
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
    }

    public static void reset(UUID playerId) {
        if (playerId != null) {
            CASTS.remove(playerId);
        }
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    private static boolean isHunter(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.HUNTER::equals)
                .isPresent();
    }

    public record AcceptedCastResult(boolean accepted, boolean empowered, int projectileCount) {
        public AcceptedCastResult {
            if (projectileCount < 0) {
                throw new IllegalArgumentException("Fan of Arrows projectile count cannot be negative.");
            }
            if (!accepted && (empowered || projectileCount != 0)) {
                throw new IllegalArgumentException("Rejected Fan of Arrows cannot carry cast state.");
            }
        }

        public static AcceptedCastResult rejected() {
            return new AcceptedCastResult(false, false, 0);
        }
    }

    public record HitResult(
            boolean accepted,
            boolean empowered,
            int targetHitCount,
            double actionCoefficient,
            double poiseCoefficient,
            double damage,
            double poiseDamage,
            boolean poiseBreak,
            int focusAfter,
            boolean activeQuarryChargePublished
    ) {
        public HitResult {
            if (targetHitCount < 0
                    || targetHitCount > HunterFanOfArrowsRules.PER_TARGET_HIT_CAP
                    || !Double.isFinite(actionCoefficient)
                    || actionCoefficient < 0.0
                    || !Double.isFinite(poiseCoefficient)
                    || poiseCoefficient < 0.0
                    || !Double.isFinite(damage)
                    || damage < 0.0
                    || !Double.isFinite(poiseDamage)
                    || poiseDamage < 0.0
                    || focusAfter < 0
                    || focusAfter > HunterQuarryFocusRuntimeState.MAX_FOCUS) {
                throw new IllegalArgumentException("Invalid Fan of Arrows hit result.");
            }
            if (!accepted
                    && (empowered
                    || targetHitCount != 0
                    || actionCoefficient != 0.0
                    || poiseCoefficient != 0.0
                    || damage != 0.0
                    || poiseDamage != 0.0
                    || poiseBreak
                    || focusAfter != 0
                    || activeQuarryChargePublished)) {
                throw new IllegalArgumentException(
                        "Rejected Fan of Arrows hit cannot carry state."
                );
            }
        }

        public static HitResult rejected() {
            return new HitResult(false, false, 0, 0.0, 0.0, 0.0, 0.0, false, 0, false);
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
}
