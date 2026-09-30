package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.HunterPowerShotRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HunterPowerShotRuntime {
    public static final long CAST_STALE_TICKS = 120L;
    private static final ConcurrentHashMap<UUID, AcceptedCast> CASTS =
            new ConcurrentHashMap<>();

    private HunterPowerShotRuntime() {}

    public static boolean canActivate(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!isHunter(player)
                || player.level().isClientSide()
                || !player.isAlive()
                || player.isSpectator()
                || !ProjectPlayerActionRuntime.canStartAction(player)) {
            return false;
        }
        var build = CombatStateServices.combatBuilds()
                .build(player.getUUID())
                .orElse(null);
        return build != null
                && HunterPowerShotRules.supportsCurrentProductionRangedWeapon(
                        build.equipment().weaponFamily()
                );
    }

    public static AcceptedCastResult onAcceptedCast(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!canActivate(player)) return AcceptedCastResult.rejected();
        long nowTick = player.level().getGameTime();
        var action = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        ProjectSpellSpec.HUNTER_POWER_SHOT_ID,
                        HunterPowerShotRules.WINDUP_TICKS,
                        HunterPowerShotRules.WINDUP_TICKS,
                        1.0
                )
        );
        if (!action.accepted()) {
            throw new IllegalStateException(
                    "Power Shot passed preflight but could not claim its wind-up window."
            );
        }
        boolean empowered =
                HunterSkillRuntime.consumeFocusSpenderIfFull(player);
        CASTS.put(
                player.getUUID(),
                new AcceptedCast(
                        (ServerLevel) player.level(),
                        player.position(),
                        HunterSkillRuntime.quarryId(player).orElse(null),
                        empowered,
                        nowTick,
                        false
                )
        );
        return new AcceptedCastResult(true, empowered);
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
                || cast.resolved()
                || cast.level() != hunter.level()
                || nowTick - cast.acceptedAtTick() > CAST_STALE_TICKS) {
            return HitResult.rejected();
        }

        var build = CombatStateServices.combatBuilds()
                .build(hunter.getUUID())
                .orElse(null);
        var targetSnapshot = ExternalActorBindingRuntime
                .projectTargetSnapshot(target, nowTick)
                .orElse(null);
        if (build == null || targetSnapshot == null) return HitResult.rejected();

        boolean weakPointHit =
                ExternalActorBindingRuntime.isAuthoredWeakPointHit(
                        target,
                        hitPosition
                );
        double actionCoefficient =
                HunterPowerShotRules.actionCoefficient(cast.empowered());
        double weakPointMultiplier =
                HunterPowerShotRules.weakPointMultiplier(
                        cast.empowered(),
                        weakPointHit
                );

        var source = build.damageSource(
                ProjectImpactTransaction.DamageSchool.PHYSICAL
        );
        double damage = ProjectImpactTransaction.resolveDirectDamage(
                new ProjectImpactTransaction.DirectDamageRequest(
                        source,
                        targetSnapshot,
                        ProjectImpactTransaction.DamageSchool.PHYSICAL,
                        actionCoefficient,
                        weakPointMultiplier,
                        1.0
                )
        ).finalDamage();
        double poiseDamage = targetSnapshot.poiseMax() > 0.0
                ? ProjectImpactTransaction.resolvePoise(
                        new ProjectImpactTransaction.PoiseRequest(
                                targetSnapshot.poiseMax(),
                                targetSnapshot.poiseMax(),
                                source.poiseOutputMultiplier(),
                                ProjectSpellSpec.HUNTER_POWER_SHOT_POISE_COEFFICIENT,
                                1.0,
                                1.0
                        )
                ).poiseDamage()
                : 0.0;

        if (!ProjectMinecraftDamageApplicator
                .applyDirectPhysical(hunter, target, damage)) {
            return HitResult.rejected();
        }
        cast.resolve();

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
        if (cast.quarryAtCast() != null
                && cast.quarryAtCast().equals(target.getUUID())) {
            ProjectUltimateChargeRuntime.recordHunterActiveQuarryHit(
                    hunter,
                    target
            );
            activeQuarryCharge = true;
        }

        return new HitResult(
                true,
                cast.empowered(),
                weakPointHit,
                actionCoefficient,
                weakPointMultiplier,
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
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey());
            AcceptedCast cast = entry.getValue();
            if (player == null
                    || !player.isAlive()
                    || player.level() != cast.level()
                    || !isHunter(player)) {
                return true;
            }
            return player.level().getGameTime()
                    - cast.acceptedAtTick()
                    > CAST_STALE_TICKS;
        });
    }

    public static void reset(UUID playerId) {
        if (playerId != null) CASTS.remove(playerId);
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

    public record AcceptedCastResult(boolean accepted, boolean empowered) {
        public AcceptedCastResult {
            if (!accepted && empowered) {
                throw new IllegalArgumentException(
                        "Rejected Power Shot cannot be empowered."
                );
            }
        }
        public static AcceptedCastResult rejected() {
            return new AcceptedCastResult(false, false);
        }
    }

    public record HitResult(
            boolean accepted,
            boolean empowered,
            boolean weakPointHit,
            double actionCoefficient,
            double weakPointMultiplier,
            double damage,
            double poiseDamage,
            boolean poiseBreak,
            int focusAfter,
            boolean activeQuarryChargePublished
    ) {
        public HitResult {
            if (!Double.isFinite(actionCoefficient)
                    || actionCoefficient < 0.0
                    || !Double.isFinite(weakPointMultiplier)
                    || weakPointMultiplier < 0.0
                    || !Double.isFinite(damage)
                    || damage < 0.0
                    || !Double.isFinite(poiseDamage)
                    || poiseDamage < 0.0
                    || focusAfter < 0
                    || focusAfter > HunterQuarryFocusRuntimeState.MAX_FOCUS) {
                throw new IllegalArgumentException(
                        "Invalid Power Shot hit result."
                );
            }
            if (!accepted
                    && (empowered
                    || weakPointHit
                    || actionCoefficient != 0.0
                    || weakPointMultiplier != 0.0
                    || damage != 0.0
                    || poiseDamage != 0.0
                    || poiseBreak
                    || focusAfter != 0
                    || activeQuarryChargePublished)) {
                throw new IllegalArgumentException(
                        "Rejected Power Shot hit cannot carry state."
                );
            }
        }

        public static HitResult rejected() {
            return new HitResult(
                    false, false, false,
                    0.0, 0.0, 0.0, 0.0,
                    false, 0, false
            );
        }
    }

    private static final class AcceptedCast {
        private final ServerLevel level;
        private final Vec3 origin;
        private final UUID quarryAtCast;
        private final boolean empowered;
        private final long acceptedAtTick;
        private boolean resolved;

        private AcceptedCast(
                ServerLevel level,
                Vec3 origin,
                UUID quarryAtCast,
                boolean empowered,
                long acceptedAtTick,
                boolean resolved
        ) {
            this.level = level;
            this.origin = origin;
            this.quarryAtCast = quarryAtCast;
            this.empowered = empowered;
            this.acceptedAtTick = acceptedAtTick;
            this.resolved = resolved;
        }
        private ServerLevel level() { return level; }
        private Vec3 origin() { return origin; }
        private UUID quarryAtCast() { return quarryAtCast; }
        private boolean empowered() { return empowered; }
        private long acceptedAtTick() { return acceptedAtTick; }
        private boolean resolved() { return resolved; }
        private void resolve() { resolved = true; }
    }
}
