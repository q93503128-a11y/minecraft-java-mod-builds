package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectBasicAttackRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerDefenseRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative Warrior root mechanic and root-skill runtime. */
public final class WarriorSkillRuntime {
    public static final long IRON_COUNTER_STANCE_TICKS = 10L;
    public static final double DRIVING_SLASH_ACTION = 1.65;
    public static final double DRIVING_SLASH_POISE = 1.30;
    public static final double DRIVING_SLASH_EMPOWERED_ACTION = 1.90;
    public static final double DRIVING_SLASH_EMPOWERED_POISE = 1.80;
    public static final double IRON_COUNTER_ACTION = 1.55;
    public static final double IRON_COUNTER_POISE = 2.00;
    public static final double CYCLONE_FIRST_ACTION = 0.90;
    public static final double CYCLONE_SECOND_ACTION = 1.20;
    public static final double CYCLONE_EMPOWERED_SECOND_ACTION = 1.40;
    public static final double CYCLONE_POISE = 1.50;
    public static final double CYCLONE_EMPOWERED_POISE = 2.00;
    public static final double BREAKER_SLAM_ACTION = 2.90;
    public static final double BREAKER_SLAM_POISE = 3.00;
    public static final double BREAKER_SLAM_EMPOWERED_ACTION = 3.35;
    public static final double BREAKER_SLAM_EMPOWERED_POISE = 4.00;
    public static final double EARTHSHATTER_ACTION = 6.00;
    public static final double EARTHSHATTER_POISE = 6.00;
    public static final long CYCLONE_SECOND_HIT_DELAY_TICKS = 5L;
    public static final long BREAKER_HYPERARMOR_TICKS = 18L;
    public static final long EARTHSHATTER_HYPERARMOR_TICKS = 24L;
    public static final double BREAKER_HYPERARMOR_MULTIPLIER = 1.60;
    public static final double EARTHSHATTER_HYPERARMOR_MULTIPLIER = 2.00;
    private static final long ACCEPTED_CAST_STALE_TICKS = 80L;

    private static final ConcurrentHashMap<UUID, WarriorMomentumRuntimeState>
            MOMENTUM = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<CastKey, AcceptedCast>
            ACCEPTED_CASTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingCycloneSecondHit>
            PENDING_CYCLONE = new ConcurrentHashMap<>();

    private WarriorSkillRuntime() {
    }

    public static boolean canActivate(
            ServerPlayer player,
            String spellId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spellId, "spellId");
        if (player.level().isClientSide()
                || PlayerProgressionService.state(player)
                        .activeClass()
                        .filter(RootClass.WARRIOR::equals)
                        .isEmpty()) {
            return false;
        }
        var build = CombatStateServices.combatBuilds()
                .build(player.getUUID())
                .orElse(null);
        if (build == null
                || !ProjectBasicAttackRules.supportsBetterCombatMelee(
                        build.equipment().weaponFamily()
                )) {
            return false;
        }

        if (ProjectSpellSpec.WARRIOR_EARTHSHATTER_ID.equals(spellId)) {
            return ProjectUltimateChargeRuntime.canActivateUltimate(
                    player
            );
        }
        return isWarriorSpell(spellId);
    }

    public static AcceptedCastResult onAcceptedCast(
            ServerPlayer player,
            String spellId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spellId, "spellId");
        if (!canActivate(player, spellId)) {
            return AcceptedCastResult.rejected();
        }

        long nowTick = player.level().getGameTime();
        WarriorMomentumRuntimeState momentum = state(player);
        boolean spender = isMomentumSpender(spellId);
        boolean empowered = spender
                && momentum.consumeSpenderIfFull(nowTick);

        if (ProjectSpellSpec.WARRIOR_IRON_COUNTER_ID.equals(spellId)) {
            momentum.beginCounter(
                    nowTick,
                    IRON_COUNTER_STANCE_TICKS
            );
        } else if (ProjectSpellSpec.WARRIOR_BREAKER_SLAM_ID.equals(spellId)) {
            momentum.beginHyperarmor(
                    BREAKER_HYPERARMOR_MULTIPLIER,
                    nowTick,
                    BREAKER_HYPERARMOR_TICKS
            );
        } else if (ProjectSpellSpec.WARRIOR_EARTHSHATTER_ID.equals(spellId)) {
            if (!ProjectUltimateChargeRuntime.tryActivateUltimate(player)) {
                return AcceptedCastResult.rejected();
            }
            momentum.beginHyperarmor(
                    EARTHSHATTER_HYPERARMOR_MULTIPLIER,
                    nowTick,
                    EARTHSHATTER_HYPERARMOR_TICKS
            );
        }

        ACCEPTED_CASTS.put(
                new CastKey(player.getUUID(), spellId),
                new AcceptedCast(empowered, nowTick)
        );
        return new AcceptedCastResult(
                true,
                empowered,
                momentum.pips(nowTick)
        );
    }

    public static ReleaseResult release(
            ServerPlayer player,
            String spellId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spellId, "spellId");

        AcceptedCast cast = ACCEPTED_CASTS.remove(
                new CastKey(player.getUUID(), spellId)
        );
        if (cast == null
                || PlayerProgressionService.state(player)
                        .activeClass()
                        .filter(RootClass.WARRIOR::equals)
                        .isEmpty()) {
            return ReleaseResult.rejected();
        }

        long nowTick = player.level().getGameTime();
        return switch (spellId) {
            case ProjectSpellSpec.WARRIOR_DRIVING_SLASH_ID ->
                    releaseDrivingSlash(player, cast, nowTick);
            case ProjectSpellSpec.WARRIOR_IRON_COUNTER_ID ->
                    new ReleaseResult(true, false, 0, 0, 0.0, 0.0);
            case ProjectSpellSpec.WARRIOR_CYCLONE_CUT_ID ->
                    releaseCycloneCut(player, cast, nowTick);
            case ProjectSpellSpec.WARRIOR_BREAKER_SLAM_ID ->
                    releaseBreakerSlam(player, cast, nowTick);
            case ProjectSpellSpec.WARRIOR_EARTHSHATTER_ID ->
                    releaseEarthshatter(player, cast, nowTick);
            default -> ReleaseResult.rejected();
        };
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ACCEPTED_CASTS.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey().playerId());
            if (player == null || !player.isAlive()) {
                return true;
            }
            long nowTick = player.level().getGameTime();
            return nowTick - entry.getValue().acceptedAtTick()
                    > ACCEPTED_CAST_STALE_TICKS;
        });
        PENDING_CYCLONE.entrySet().removeIf(entry -> {
            PendingCycloneSecondHit pending = entry.getValue();
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey());
            if (player == null
                    || !player.isAlive()
                    || player.level() != pending.level()) {
                return true;
            }

            long nowTick = pending.level().getGameTime();
            if (nowTick < pending.resolveAtTick()) {
                return false;
            }

            double secondAction = pending.empowered()
                    ? CYCLONE_EMPOWERED_SECOND_ACTION
                    : CYCLONE_SECOND_ACTION;
            double wholePoise = pending.empowered()
                    ? CYCLONE_EMPOWERED_POISE
                    : CYCLONE_POISE;
            double totalAction = CYCLONE_FIRST_ACTION + secondAction;
            double secondPoise = wholePoise
                    * secondAction
                    / totalAction;
            resolveRadial(
                    player,
                    pending.origin(),
                    WarriorSkillShape.CYCLONE_RADIUS,
                    secondAction,
                    secondPoise,
                    pending.empowered(),
                    false
            );
            return true;
        });
    }

    public static Optional<PlayerDefenseRuntimeState.IncomingDefenseResult>
    tryResolveIronCounter(
            LivingEntity attacker,
            ServerPlayer warrior,
            PlayerDefenseAuthority.IncomingHit hit,
            PlayerDefenseAuthority.DefenseSnapshot defense,
            long nowTick
    ) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(warrior, "warrior");
        Objects.requireNonNull(hit, "hit");
        Objects.requireNonNull(defense, "defense");

        if (!hit.perfectGuardable()
                || !isWarrior(warrior)
                || ExternalActorBindingRuntime
                        .combatProfile(attacker)
                        .isEmpty()) {
            return Optional.empty();
        }

        WarriorMomentumRuntimeState momentum = state(warrior);
        if (!momentum.consumeCounterWindow(nowTick)) {
            return Optional.empty();
        }

        double mitigated =
                PlayerDefenseAuthority
                        .mitigatedDamageBeforeActiveDefense(
                                hit,
                                defense
                        );
        momentum.recordSuccessfulCounter(nowTick);
        CombatStateServices.markCombatActivity(
                warrior.getUUID(),
                nowTick
        );

        resolveSingleTarget(
                warrior,
                attacker,
                IRON_COUNTER_ACTION,
                IRON_COUNTER_POISE
        );
        ProjectUltimateChargeRuntime.recordWarriorPerfectGuard(
                warrior,
                attacker
        );

        return Optional.of(
                new PlayerDefenseRuntimeState.IncomingDefenseResult(
                        mitigated,
                        0.0,
                        0.0,
                        false,
                        true,
                        true,
                        false
                )
        );
    }

    public static void onSuccessfulPerfectGuard(
            ServerPlayer warrior,
            LivingEntity attacker,
            long nowTick
    ) {
        if (!isWarrior(warrior)
                || ExternalActorBindingRuntime
                        .combatProfile(attacker)
                        .isEmpty()) {
            return;
        }
        state(warrior).recordPerfectGuardActivity(nowTick);
        ProjectUltimateChargeRuntime.recordWarriorPerfectGuard(
                warrior,
                attacker
        );
    }

    public static void onMeleeBasicHit(
            ServerPlayer warrior,
            LivingEntity target,
            boolean cycleFinisher,
            boolean poiseBreakTriggered,
            long nowTick
    ) {
        if (!isWarrior(warrior)) {
            return;
        }
        WarriorMomentumRuntimeState momentum = state(warrior);
        momentum.recordMeleeBasicHit(cycleFinisher, nowTick);
        if (cycleFinisher
                && momentum.claimBasicCyclePublication(nowTick)) {
            ProjectUltimateChargeRuntime.recordWarriorBasicCycle(
                    warrior,
                    target
            );
        }
        if (poiseBreakTriggered) {
            onPersonalEliteBossPoiseBreak(
                    warrior,
                    target,
                    nowTick
            );
        }
    }

    public static void onPersonalEliteBossPoiseBreak(
            ServerPlayer warrior,
            LivingEntity target,
            long nowTick
    ) {
        if (!isWarrior(warrior)) {
            return;
        }
        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElse(null);
        if (profile == null
                || profile.combatRank()
                        != ExternalActorCombatProfile.CombatRank
                                .MINIBOSS_BOSS) {
            return;
        }

        state(warrior).recordEliteBossPoiseBreak(nowTick);
        ProjectUltimateChargeRuntime.recordWarriorPoiseBreak(
                warrior,
                target
        );
    }

    public static double momentumPoiseOutputMultiplier(
            ServerPlayer player,
            long nowTick
    ) {
        if (!isWarrior(player)) {
            return 1.0;
        }
        return state(player).poiseOutputMultiplier(nowTick);
    }

    public static double ordinaryHitStaggerTakenMultiplier(
            ServerPlayer player,
            long nowTick
    ) {
        if (!isWarrior(player)) {
            return 1.0;
        }
        return state(player)
                .ordinaryHitStaggerTakenMultiplier(nowTick);
    }

    public static double hyperarmorMultiplier(
            ServerPlayer player,
            long nowTick
    ) {
        if (!isWarrior(player)) {
            return 1.0;
        }
        return state(player).hyperarmorMultiplier(nowTick);
    }

    public static int momentumPips(
            ServerPlayer player,
            long nowTick
    ) {
        return isWarrior(player)
                ? state(player).pips(nowTick)
                : 0;
    }

    public static void reset(UUID playerId) {
        if (playerId == null) {
            return;
        }
        WarriorMomentumRuntimeState state = MOMENTUM.remove(playerId);
        if (state != null) {
            state.reset();
        }
        ACCEPTED_CASTS.keySet().removeIf(
                key -> key.playerId().equals(playerId)
        );
        PENDING_CYCLONE.remove(playerId);
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    private static ReleaseResult releaseDrivingSlash(
            ServerPlayer player,
            AcceptedCast cast,
            long nowTick
    ) {
        double movement = cast.empowered() ? 2.6 : 2.2;
        moveCollisionSafeForward(player, movement);
        HitResolution hit = resolveFrontal(
                player,
                cast.empowered()
                        ? DRIVING_SLASH_EMPOWERED_ACTION
                        : DRIVING_SLASH_ACTION,
                cast.empowered()
                        ? DRIVING_SLASH_EMPOWERED_POISE
                        : DRIVING_SLASH_POISE,
                WarriorSkillShape.DRIVING_SLASH_RANGE,
                WarriorSkillShape
                        .DRIVING_SLASH_HALF_ANGLE_DEGREES,
                true
        );
        return hit.asRelease(cast.empowered());
    }

    private static ReleaseResult releaseCycloneCut(
            ServerPlayer player,
            AcceptedCast cast,
            long nowTick
    ) {
        Vec3 origin = player.position();
        double secondAction = cast.empowered()
                ? CYCLONE_EMPOWERED_SECOND_ACTION
                : CYCLONE_SECOND_ACTION;
        double wholePoise = cast.empowered()
                ? CYCLONE_EMPOWERED_POISE
                : CYCLONE_POISE;
        double totalAction = CYCLONE_FIRST_ACTION + secondAction;
        double firstPoise = wholePoise
                * CYCLONE_FIRST_ACTION
                / totalAction;

        HitResolution first = resolveRadial(
                player,
                origin,
                WarriorSkillShape.CYCLONE_RADIUS,
                CYCLONE_FIRST_ACTION,
                firstPoise,
                false,
                true
        );
        PENDING_CYCLONE.put(
                player.getUUID(),
                new PendingCycloneSecondHit(
                        (ServerLevel) player.level(),
                        origin,
                        cast.empowered(),
                        Math.addExact(
                                nowTick,
                                CYCLONE_SECOND_HIT_DELAY_TICKS
                        )
                )
        );
        return first.asRelease(cast.empowered());
    }

    private static ReleaseResult releaseBreakerSlam(
            ServerPlayer player,
            AcceptedCast cast,
            long nowTick
    ) {
        HitResolution hit = resolveFrontal(
                player,
                cast.empowered()
                        ? BREAKER_SLAM_EMPOWERED_ACTION
                        : BREAKER_SLAM_ACTION,
                cast.empowered()
                        ? BREAKER_SLAM_EMPOWERED_POISE
                        : BREAKER_SLAM_POISE,
                WarriorSkillShape.BREAKER_SLAM_RANGE,
                WarriorSkillShape
                        .BREAKER_SLAM_HALF_ANGLE_DEGREES,
                true
        );
        return hit.asRelease(cast.empowered());
    }

    private static ReleaseResult releaseEarthshatter(
            ServerPlayer player,
            AcceptedCast cast,
            long nowTick
    ) {
        HitResolution hit = resolveFrontal(
                player,
                EARTHSHATTER_ACTION,
                EARTHSHATTER_POISE,
                WarriorSkillShape.EARTHSHATTER_RANGE,
                WarriorSkillShape
                        .EARTHSHATTER_HALF_ANGLE_DEGREES,
                false
        );
        return hit.asRelease(false);
    }

    private static HitResolution resolveFrontal(
            ServerPlayer player,
            double actionCoefficient,
            double poiseCoefficient,
            double range,
            double halfAngleDegrees,
            boolean publishActiveCharge
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return HitResolution.empty();
        }
        long nowTick = level.getGameTime();
        List<LivingEntity> targets = level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(range + 1.0),
                        target -> ExternalActorBindingRuntime
                                .projectTargetSnapshot(target, nowTick)
                                .isPresent()
                                && WarriorSkillShape.frontalContains(
                                        player,
                                        target,
                                        range,
                                        halfAngleDegrees
                                )
                                && player.hasLineOfSight(target)
                ).stream()
                .sorted(
                        Comparator.comparingDouble(
                                (LivingEntity target) ->
                                        player.distanceToSqr(target)
                        ).thenComparingInt(LivingEntity::getId)
                )
                .toList();
        return resolveTargets(
                player,
                targets,
                actionCoefficient,
                poiseCoefficient,
                false,
                publishActiveCharge
        );
    }

    private static HitResolution resolveRadial(
            ServerPlayer player,
            Vec3 origin,
            double radius,
            double actionCoefficient,
            double poiseCoefficient,
            boolean pullNormalTargets,
            boolean publishActiveCharge
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return HitResolution.empty();
        }
        long nowTick = level.getGameTime();
        List<LivingEntity> targets = level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(radius + 1.0),
                        target -> ExternalActorBindingRuntime
                                .projectTargetSnapshot(target, nowTick)
                                .isPresent()
                                && WarriorSkillShape.radialContains(
                                        origin,
                                        target,
                                        radius
                                )
                                && player.hasLineOfSight(target)
                ).stream()
                .sorted(
                        Comparator.comparingDouble(
                                (LivingEntity target) ->
                                        target.position()
                                                .distanceToSqr(origin)
                        ).thenComparingInt(LivingEntity::getId)
                )
                .toList();
        return resolveTargets(
                player,
                targets,
                actionCoefficient,
                poiseCoefficient,
                pullNormalTargets,
                publishActiveCharge
        );
    }

    private static HitResolution resolveSingleTarget(
            ServerPlayer player,
            LivingEntity target,
            double actionCoefficient,
            double poiseCoefficient
    ) {
        return resolveTargets(
                player,
                List.of(target),
                actionCoefficient,
                poiseCoefficient,
                false,
                true
        );
    }

    private static HitResolution resolveTargets(
            ServerPlayer player,
            List<LivingEntity> targets,
            double actionCoefficient,
            double poiseCoefficient,
            boolean pullNormalTargets,
            boolean publishActiveCharge
    ) {
        var build = CombatStateServices.combatBuilds()
                .build(player.getUUID())
                .orElse(null);
        if (build == null) {
            return HitResolution.empty();
        }

        long nowTick = player.level().getGameTime();
        var source = build.damageSource(
                ProjectImpactTransaction.DamageSchool.PHYSICAL
        );
        double momentumPoiseMultiplier =
                momentumPoiseOutputMultiplier(
                        player,
                        nowTick
                );

        int applied = 0;
        int poiseBreaks = 0;
        double totalDamage = 0.0;
        double totalPoise = 0.0;
        LivingEntity primary = null;

        for (LivingEntity target : targets) {
            var snapshot = ExternalActorBindingRuntime
                    .projectTargetSnapshot(target, nowTick)
                    .orElse(null);
            if (snapshot == null) {
                continue;
            }

            double damage = ProjectImpactTransaction
                    .resolveDirectDamage(
                            new ProjectImpactTransaction
                                    .DirectDamageRequest(
                                            source,
                                            snapshot,
                                            ProjectImpactTransaction
                                                    .DamageSchool.PHYSICAL,
                                            actionCoefficient,
                                            1.0,
                                            1.0
                                    )
                    ).finalDamage();
            double poiseDamage =
                    ProjectImpactTransaction.resolvePoise(
                            new ProjectImpactTransaction.PoiseRequest(
                                    snapshot.poiseMax(),
                                    snapshot.poiseMax(),
                                    source.poiseOutputMultiplier()
                                            * momentumPoiseMultiplier,
                                    poiseCoefficient,
                                    1.0,
                                    1.0
                            )
                    ).poiseDamage();

            if (!ProjectMinecraftDamageApplicator
                    .applyDirectPhysical(
                            player,
                            target,
                            damage
                    )) {
                continue;
            }

            applied++;
            totalDamage += damage;
            if (primary == null) {
                primary = target;
            }

            if (poiseDamage > 0.0) {
                var poise = ExternalActorBindingRuntime
                        .applyProjectPoiseDamage(
                                target,
                                poiseDamage,
                                nowTick
                        )
                        .orElse(null);
                totalPoise += poiseDamage;
                if (poise != null && poise.breakTriggered()) {
                    poiseBreaks++;
                    R01EarthloongMythicRuntime
                            .onPersonalEliteBossPoiseBreak(
                                    player,
                                    target,
                                    build,
                                    nowTick
                            );
                    onPersonalEliteBossPoiseBreak(
                            player,
                            target,
                            nowTick
                    );
                }
            }

            if (pullNormalTargets) {
                var profile = ExternalActorBindingRuntime
                        .combatProfile(target)
                        .orElse(null);
                /*
                 * Current actor rank data merges normal and elite into one value. Pulling that
                 * merged bucket would incorrectly displace elites, so the normal-only empowered
                 * pull stays fail-closed until those ranks are separated.
                 */
                if (profile != null
                        && profile.combatRank()
                                != ExternalActorCombatProfile
                                        .CombatRank.MINIBOSS_BOSS) {
                    // Deliberately no displacement until NORMAL vs ELITE is authoritative.
                }
            }
        }

        if (applied > 0) {
            state(player).recordMeleeActiveHit(nowTick);
            CombatStateServices.markCombatActivity(
                    player.getUUID(),
                    nowTick
            );
            if (publishActiveCharge && primary != null) {
                ProjectUltimateChargeRuntime
                        .recordWarriorActiveHit(
                                player,
                                primary
                        );
            }
        }

        return new HitResolution(
                targets.size(),
                applied,
                poiseBreaks,
                totalDamage,
                totalPoise
        );
    }

    private static void moveCollisionSafeForward(
            ServerPlayer player,
            double distance
    ) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() <= 1.0e-9) {
            return;
        }
        player.move(
                MoverType.SELF,
                horizontal.normalize().scale(distance)
        );
    }

    private static WarriorMomentumRuntimeState state(
            ServerPlayer player
    ) {
        return MOMENTUM.computeIfAbsent(
                player.getUUID(),
                ignored -> new WarriorMomentumRuntimeState()
        );
    }

    private static boolean isWarrior(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.WARRIOR::equals)
                .isPresent();
    }

    private static boolean isWarriorSpell(String spellId) {
        return ProjectSpellSpec.WARRIOR_DRIVING_SLASH_ID
                        .equals(spellId)
                || ProjectSpellSpec.WARRIOR_IRON_COUNTER_ID
                        .equals(spellId)
                || ProjectSpellSpec.WARRIOR_CYCLONE_CUT_ID
                        .equals(spellId)
                || ProjectSpellSpec.WARRIOR_BREAKER_SLAM_ID
                        .equals(spellId)
                || ProjectSpellSpec.WARRIOR_EARTHSHATTER_ID
                        .equals(spellId);
    }

    private static boolean isMomentumSpender(String spellId) {
        return ProjectSpellSpec.WARRIOR_DRIVING_SLASH_ID
                        .equals(spellId)
                || ProjectSpellSpec.WARRIOR_CYCLONE_CUT_ID
                        .equals(spellId)
                || ProjectSpellSpec.WARRIOR_BREAKER_SLAM_ID
                        .equals(spellId);
    }

    private record CastKey(UUID playerId, String spellId) {
    }

    private record AcceptedCast(
            boolean empowered,
            long acceptedAtTick
    ) {
    }

    private record PendingCycloneSecondHit(
            ServerLevel level,
            Vec3 origin,
            boolean empowered,
            long resolveAtTick
    ) {
    }

    private record HitResolution(
            int eligibleTargets,
            int appliedTargets,
            int poiseBreaks,
            double totalDamage,
            double totalPoiseDamage
    ) {
        private static HitResolution empty() {
            return new HitResolution(
                    0,
                    0,
                    0,
                    0.0,
                    0.0
            );
        }

        private ReleaseResult asRelease(boolean empowered) {
            return new ReleaseResult(
                    true,
                    empowered,
                    eligibleTargets,
                    appliedTargets,
                    totalDamage,
                    totalPoiseDamage
            );
        }
    }

    public record AcceptedCastResult(
            boolean accepted,
            boolean empowered,
            int momentumPipsAfter
    ) {
        public AcceptedCastResult {
            if (momentumPipsAfter < 0
                    || momentumPipsAfter
                            > WarriorMomentumRuntimeState.MAX_PIPS) {
                throw new IllegalArgumentException(
                        "Invalid Warrior Momentum result."
                );
            }
            if (!accepted
                    && (empowered || momentumPipsAfter != 0)) {
                throw new IllegalArgumentException(
                        "Rejected Warrior cast cannot carry state."
                );
            }
        }

        public static AcceptedCastResult rejected() {
            return new AcceptedCastResult(false, false, 0);
        }
    }

    public record ReleaseResult(
            boolean accepted,
            boolean empowered,
            int eligibleTargets,
            int appliedTargets,
            double totalDamage,
            double totalPoiseDamage
    ) {
        public ReleaseResult {
            if (eligibleTargets < 0
                    || appliedTargets < 0
                    || appliedTargets > eligibleTargets
                    || !Double.isFinite(totalDamage)
                    || totalDamage < 0.0
                    || !Double.isFinite(totalPoiseDamage)
                    || totalPoiseDamage < 0.0) {
                throw new IllegalArgumentException(
                        "Invalid Warrior release result."
                );
            }
            if (!accepted
                    && (empowered
                    || eligibleTargets != 0
                    || appliedTargets != 0
                    || totalDamage != 0.0
                    || totalPoiseDamage != 0.0)) {
                throw new IllegalArgumentException(
                        "Rejected Warrior release cannot carry state."
                );
            }
        }

        public static ReleaseResult rejected() {
            return new ReleaseResult(
                    false,
                    false,
                    0,
                    0,
                    0.0,
                    0.0
            );
        }
    }
}
