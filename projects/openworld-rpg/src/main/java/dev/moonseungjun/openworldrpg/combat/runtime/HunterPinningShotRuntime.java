package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.HunterQuickstepVolleyRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.progression.r01.R01ClassInsightService;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative damage, poise, Focus and Snared resolution for Hunter Pinning Shot. */
public final class HunterPinningShotRuntime {
    public static final int ACTION_TICKS = 10;
    public static final long CAST_STALE_TICKS = 100L;

    private static final ConcurrentHashMap<UUID, AcceptedCast>
            CASTS = new ConcurrentHashMap<>();

    private HunterPinningShotRuntime() {
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
        var build = CombatStateServices.combatBuilds()
                .build(player.getUUID())
                .orElse(null);
        return build != null
                && HunterQuickstepVolleyRules
                        .supportsCurrentProductionRangedWeapon(
                                build.equipment().weaponFamily()
                        );
    }

    public static AcceptedCastResult onAcceptedCast(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        if (!canActivate(player)) {
            return AcceptedCastResult.rejected();
        }

        long nowTick = player.level().getGameTime();
        var action = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        ProjectSpellSpec.HUNTER_PINNING_SHOT_ID,
                        ACTION_TICKS,
                        ACTION_TICKS,
                        1.0
                )
        );
        if (!action.accepted()) {
            throw new IllegalStateException(
                    "Pinning Shot passed preflight but could not claim its action window."
            );
        }

        boolean empowered =
                HunterSkillRuntime.consumeFocusSpenderIfFull(
                        player
                );
        CASTS.put(
                player.getUUID(),
                new AcceptedCast(
                        (ServerLevel) player.level(),
                        player.position(),
                        HunterSkillRuntime.quarryId(player)
                                .orElse(null),
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
                || hunter.level() != target.level()
                || !ProjectHostileStatusRuntime
                        .canApplySnared(target)) {
            return HitResult.rejected();
        }

        long nowTick = hunter.level().getGameTime();
        AcceptedCast cast = CASTS.get(hunter.getUUID());
        if (cast == null
                || cast.resolved()
                || cast.level() != hunter.level()
                || nowTick - cast.acceptedAtTick()
                        > CAST_STALE_TICKS) {
            return HitResult.rejected();
        }

        var build = CombatStateServices.combatBuilds()
                .build(hunter.getUUID())
                .orElse(null);
        var snapshot = ExternalActorBindingRuntime
                .projectTargetSnapshot(target, nowTick)
                .orElse(null);
        if (build == null || snapshot == null) {
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

        var source = build.damageSource(
                ProjectImpactTransaction.DamageSchool.PHYSICAL
        );
        double damage = ProjectImpactTransaction
                .resolveDirectDamage(
                        new ProjectImpactTransaction.DirectDamageRequest(
                                source,
                                snapshot,
                                ProjectImpactTransaction
                                        .DamageSchool.PHYSICAL,
                                ProjectSpellSpec
                                        .HUNTER_PINNING_ACTION_COEFFICIENT,
                                weakPointMultiplier,
                                1.0
                        )
                ).finalDamage();
        double poiseCoefficient = cast.empowered()
                ? ProjectSpellSpec
                        .HUNTER_PINNING_EMPOWERED_POISE_COEFFICIENT
                : ProjectSpellSpec
                        .HUNTER_PINNING_POISE_COEFFICIENT;
        double poiseDamage = snapshot.poiseMax() > 0.0
                ? ProjectImpactTransaction.resolvePoise(
                        new ProjectImpactTransaction.PoiseRequest(
                                snapshot.poiseMax(),
                                snapshot.poiseMax(),
                                source.poiseOutputMultiplier(),
                                poiseCoefficient,
                                1.0,
                                1.0
                        )
                ).poiseDamage()
                : 0.0;

        if (!ProjectMinecraftDamageApplicator
                .applyDirectPhysical(
                        hunter,
                        target,
                        damage
                )) {
            return HitResult.rejected();
        }
        cast.resolve();

        boolean poiseBreak = false;
        if (poiseDamage > 0.0) {
            var poise = ExternalActorBindingRuntime
                    .applyProjectPoiseDamage(
                            target,
                            poiseDamage,
                            nowTick
                    )
                    .orElse(null);
            poiseBreak = poise != null
                    && poise.breakTriggered();
        }

        double distance = cast.origin()
                .distanceTo(hitPosition);
        var hunterHit = HunterSkillRuntime.onRangedSkillHit(
                hunter,
                target,
                distance,
                weakPointHit,
                poiseBreak,
                nowTick
        );
        var snared = ProjectHostileStatusRuntime.applySnared(
                target,
                cast.empowered(),
                nowTick
        ).orElseThrow();

        CombatStateServices.markCombatActivity(
                hunter.getUUID(),
                nowTick
        );
        R01ClassInsightService.recordHunterCrownedMarkHit(
                hunter,
                target,
                cast.quarryAtCast(),
                cast.empowered(),
                weakPointHit
        );

        boolean activeQuarryCharge = false;
        if (cast.quarryAtCast() != null
                && cast.quarryAtCast()
                        .equals(target.getUUID())) {
            ProjectUltimateChargeRuntime
                    .recordHunterActiveQuarryHit(
                            hunter,
                            target
                    );
            activeQuarryCharge = true;
        }

        return new HitResult(
                true,
                cast.empowered(),
                damage,
                poiseDamage,
                poiseBreak,
                hunterHit.focusAfter(),
                snared.movementMultiplier(),
                snared.durationTicks(),
                activeQuarryCharge
        );
    }

    public static void tick(MinecraftServer server) {
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

    public record AcceptedCastResult(
            boolean accepted,
            boolean empowered
    ) {
        public AcceptedCastResult {
            if (!accepted && empowered) {
                throw new IllegalArgumentException(
                        "Rejected Pinning Shot cannot be empowered."
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
            double damage,
            double poiseDamage,
            boolean poiseBreak,
            int focusAfter,
            double snaredMovementMultiplier,
            long snaredDurationTicks,
            boolean activeQuarryChargePublished
    ) {
        public HitResult {
            if (!Double.isFinite(damage)
                    || damage < 0.0
                    || !Double.isFinite(poiseDamage)
                    || poiseDamage < 0.0
                    || focusAfter < 0
                    || focusAfter
                            > HunterQuarryFocusRuntimeState.MAX_FOCUS
                    || !Double.isFinite(
                            snaredMovementMultiplier
                    )
                    || snaredMovementMultiplier < 0.0
                    || snaredMovementMultiplier > 1.0
                    || snaredDurationTicks < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Pinning Shot hit result."
                );
            }
            if (!accepted
                    && (empowered
                    || damage != 0.0
                    || poiseDamage != 0.0
                    || poiseBreak
                    || focusAfter != 0
                    || snaredMovementMultiplier != 0.0
                    || snaredDurationTicks != 0L
                    || activeQuarryChargePublished)) {
                throw new IllegalArgumentException(
                        "Rejected Pinning Shot hit cannot carry state."
                );
            }
        }

        public static HitResult rejected() {
            return new HitResult(
                    false,
                    false,
                    0.0,
                    0.0,
                    false,
                    0,
                    0.0,
                    0L,
                    false
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
