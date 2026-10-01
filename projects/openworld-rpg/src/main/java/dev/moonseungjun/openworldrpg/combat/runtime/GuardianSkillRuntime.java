package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerDefenseRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
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

/** Server-authoritative Guardian root mechanic, active-skill and Ultimate runtime. */
public final class GuardianSkillRuntime {
    public static final double STAND_FIRM_POISE_MULTIPLIER = 1.25;
    public static final double STAND_FIRM_ORDINARY_KNOCKBACK_MULTIPLIER = 0.70;

    public static final double BULWARK_RUSH_ACTION = 1.45;
    public static final double BULWARK_RUSH_POISE = 1.80;
    public static final double BULWARK_RUSH_DISTANCE = 3.8;
    public static final double BULWARK_RUSH_HALF_ANGLE_DEGREES = 36.0;
    public static final double BULWARK_RUSH_GUARD_ABSORPTION_FACTOR = 0.50;
    public static final long BULWARK_RUSH_DEFENSE_TICKS = 8L;

    public static final double WARDING_STRIKE_ACTION = 1.60;
    public static final double WARDING_STRIKE_POISE = 1.40;
    public static final double WARDING_STRIKE_RANGE = 3.5;
    public static final double WARDING_STRIKE_HALF_ANGLE_DEGREES = 70.0;

    public static final double AEGIS_FIELD_RADIUS = 5.0;
    public static final double AEGIS_FIELD_BASE_BARRIER = 0.18;
    public static final double AEGIS_FIELD_BARRIER_PER_RESOLVE = 0.04;

    public static final long COUNTERWALL_STANCE_TICKS = 15L;
    public static final long COUNTERWALL_RECOVERY_TICKS = 11L;
    public static final double COUNTERWALL_ACTION = 1.70;
    public static final double COUNTERWALL_POISE = 2.20;
    public static final double COUNTERWALL_RANGE = 4.0;
    public static final double COUNTERWALL_HALF_ANGLE_DEGREES = 65.0;

    public static final long UNBROKEN_LINE_DURATION_TICKS = 160L;
    public static final double UNBROKEN_LINE_RADIUS = 7.0;
    public static final double UNBROKEN_LINE_INITIAL_BARRIER = 0.22;
    public static final double UNBROKEN_LINE_DAMAGE_TAKEN_MULTIPLIER = 0.80;
    public static final double UNBROKEN_LINE_GUARD_COST_MULTIPLIER = 0.80;
    public static final double UNBROKEN_LINE_GUARDIAN_POISE_MULTIPLIER = 1.75;
    public static final long UNBROKEN_LINE_PROVOKE_REFRESH_TICKS = 40L;

    public static final int BULWARK_RUSH_ACTION_TICKS = 16;
    public static final int WARDING_STRIKE_ACTION_TICKS = 14;
    public static final int AEGIS_FIELD_ACTION_TICKS = 18;
    public static final int COUNTERWALL_ACTION_TICKS =
            (int) (COUNTERWALL_STANCE_TICKS + COUNTERWALL_RECOVERY_TICKS);
    public static final int UNBROKEN_LINE_ACTION_TICKS = 20;

    private static final long ACCEPTED_CAST_STALE_TICKS = 80L;
    private static final String AEGIS_BARRIER_PREFIX =
            "openworld_rpg:aegis_field/";
    private static final String UNBROKEN_BARRIER_PREFIX =
            "openworld_rpg:unbroken_line/";

    private static final ConcurrentHashMap<CastKey, AcceptedCast>
            ACCEPTED_CASTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long>
            BULWARK_DEFENSE_UNTIL = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long>
            COUNTERWALL_UNTIL = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ActiveAura>
            ACTIVE_AURAS = new ConcurrentHashMap<>();

    private GuardianSkillRuntime() {
    }

    public static boolean canActivate(
            ServerPlayer player,
            String spellId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spellId, "spellId");
        if (player.level().isClientSide()
                || !isGuardian(player)
                || !isGuardianSpell(spellId)
                || CombatStateServices.combatBuilds()
                        .build(player.getUUID())
                        .isEmpty()
                || !ProjectPlayerActionRuntime.canStartAction(player)) {
            return false;
        }
        if ((ProjectSpellSpec.GUARDIAN_BULWARK_RUSH_ID.equals(spellId)
                || ProjectSpellSpec.GUARDIAN_COUNTERWALL_ID.equals(spellId))
                && !hasGuardCapableSetup(player)) {
            return false;
        }
        if (ProjectSpellSpec.GUARDIAN_UNBROKEN_LINE_ID.equals(spellId)) {
            return ProjectUltimateChargeRuntime.canActivateUltimate(player);
        }
        return true;
    }

    public static boolean onAcceptedCast(
            ServerPlayer player,
            String spellId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spellId, "spellId");
        if (!canActivate(player, spellId)) {
            return false;
        }
        long nowTick = player.level().getGameTime();
        var action = ProjectPlayerActionRuntime.beginAction(
                player,
                actionSpec(spellId)
        );
        if (!action.accepted()) {
            return false;
        }
        if (ProjectSpellSpec.GUARDIAN_UNBROKEN_LINE_ID.equals(spellId)
                && !ProjectUltimateChargeRuntime.tryActivateUltimate(player)) {
            return false;
        }
        if (ProjectSpellSpec.GUARDIAN_BULWARK_RUSH_ID.equals(spellId)) {
            BULWARK_DEFENSE_UNTIL.put(
                    player.getUUID(),
                    Math.addExact(nowTick, BULWARK_RUSH_DEFENSE_TICKS)
            );
        } else if (ProjectSpellSpec.GUARDIAN_COUNTERWALL_ID.equals(spellId)) {
            COUNTERWALL_UNTIL.put(
                    player.getUUID(),
                    Math.addExact(nowTick, COUNTERWALL_STANCE_TICKS)
            );
        }
        ACCEPTED_CASTS.put(
                new CastKey(player.getUUID(), spellId),
                new AcceptedCast(nowTick)
        );
        return true;
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
                || !isGuardian(player)
                || !ownsActiveAction(player, spellId)) {
            return ReleaseResult.rejected();
        }
        long nowTick = player.level().getGameTime();
        return switch (spellId) {
            case ProjectSpellSpec.GUARDIAN_BULWARK_RUSH_ID ->
                    releaseBulwarkRush(player, nowTick);
            case ProjectSpellSpec.GUARDIAN_WARDING_STRIKE_ID ->
                    releaseWardingStrike(player, nowTick);
            case ProjectSpellSpec.GUARDIAN_AEGIS_FIELD_ID ->
                    releaseAegisField(player, nowTick);
            case ProjectSpellSpec.GUARDIAN_COUNTERWALL_ID ->
                    new ReleaseResult(true, 0, 0.0, 0.0, 0, 0, 0L);
            case ProjectSpellSpec.GUARDIAN_UNBROKEN_LINE_ID ->
                    releaseUnbrokenLine(player, nowTick);
            default -> ReleaseResult.rejected();
        };
    }

    public static Optional<PlayerDefenseRuntimeState.IncomingDefenseResult>
    tryResolveBulwarkRushGuard(
            LivingEntity attacker,
            ServerPlayer guardian,
            PlayerDefenseAuthority.IncomingHit hit,
            PlayerDefenseAuthority.DefenseSnapshot defense,
            long nowTick
    ) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(guardian, "guardian");
        Objects.requireNonNull(hit, "hit");
        Objects.requireNonNull(defense, "defense");
        Long until = BULWARK_DEFENSE_UNTIL.get(guardian.getUUID());
        if (until == null
                || nowTick >= until
                || !isGuardian(guardian)
                || !hit.guardable()
                || defense.guardType().isEmpty()
                || !frontalContains(
                        guardian,
                        attacker,
                        6.0,
                        70.0
                )) {
            return Optional.empty();
        }

        double mitigated = PlayerDefenseAuthority
                .mitigatedDamageBeforeActiveDefense(hit, defense);
        double absorption = PlayerDefenseAuthority.guardAbsorption(
                defense.guardType().orElseThrow(),
                hit.school()
        ) * BULWARK_RUSH_GUARD_ABSORPTION_FACTOR;
        double finalDamage = ProjectCombatRules.roundFinal(
                mitigated * (1.0 - absorption)
        );
        return Optional.of(
                new PlayerDefenseRuntimeState.IncomingDefenseResult(
                        mitigated,
                        finalDamage,
                        0.0,
                        false,
                        true,
                        false,
                        false
                )
        );
    }

    public static Optional<PlayerDefenseRuntimeState.IncomingDefenseResult>
    tryResolveCounterwall(
            LivingEntity attacker,
            ServerPlayer guardian,
            PlayerDefenseAuthority.IncomingHit hit,
            PlayerDefenseAuthority.DefenseSnapshot defense,
            long nowTick
    ) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(guardian, "guardian");
        Objects.requireNonNull(hit, "hit");
        Objects.requireNonNull(defense, "defense");
        Long until = COUNTERWALL_UNTIL.get(guardian.getUUID());
        if (until == null
                || nowTick >= until
                || !isGuardian(guardian)
                || !hit.perfectGuardable()
                || defense.guardType().isEmpty()
                || ExternalActorBindingRuntime.combatProfile(attacker).isEmpty()) {
            return Optional.empty();
        }
        if (!COUNTERWALL_UNTIL.remove(guardian.getUUID(), until)) {
            return Optional.empty();
        }

        double mitigated = PlayerDefenseAuthority
                .mitigatedDamageBeforeActiveDefense(hit, defense);
        resolveFrontal(
                guardian,
                COUNTERWALL_ACTION,
                COUNTERWALL_POISE,
                COUNTERWALL_RANGE,
                COUNTERWALL_HALF_ANGLE_DEGREES,
                ProjectImpactTransaction.DamageSchool.PHYSICAL,
                false
        );
        GuardianResolveRuntime.onCounterwallPerfectGuard(
                guardian,
                nowTick
        );
        CombatStateServices.markCombatActivity(
                guardian.getUUID(),
                nowTick
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

    public static double incomingDamageTakenMultiplier(
            ServerPlayer target,
            PlayerDefenseAuthority.IncomingHit hit,
            long nowTick
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(hit, "hit");
        if (!(hit.guardable() || hit.perfectGuardable())) {
            return 1.0;
        }
        return insideAnyUnbrokenAura(target, nowTick)
                ? UNBROKEN_LINE_DAMAGE_TAKEN_MULTIPLIER
                : 1.0;
    }

    public static double guardStaminaCostMultiplier(
            ServerPlayer target,
            long nowTick
    ) {
        Objects.requireNonNull(target, "target");
        return insideAnyUnbrokenAura(target, nowTick)
                ? UNBROKEN_LINE_GUARD_COST_MULTIPLIER
                : 1.0;
    }

    public static double standFirmPoiseMaxMultiplier(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        return isGuardian(player)
                && CombatStateServices.defenseStates()
                        .getOrCreate(player.getUUID())
                        .isGuardHeld()
                ? STAND_FIRM_POISE_MULTIPLIER
                : 1.0;
    }

    public static double unbrokenLinePoiseMultiplier(
            ServerPlayer player,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        if (!isGuardian(player)) {
            return 1.0;
        }
        ActiveAura ownAura = ACTIVE_AURAS.get(player.getUUID());
        return ownAura != null
                && nowTick < ownAura.expiresAtTick()
                && ownAura.level() == player.level()
                ? UNBROKEN_LINE_GUARDIAN_POISE_MULTIPLIER
                : 1.0;
    }

    /**
     * Authored seam for ordinary, non-launch knockback. Current R01 hostile attacks do not publish
     * an ordinary knockback magnitude, so the value remains dormant there rather than inventing one.
     */
    public static double ordinaryIncomingKnockbackMultiplier(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        return isGuardian(player)
                && CombatStateServices.defenseStates()
                        .getOrCreate(player.getUUID())
                        .isGuardHeld()
                ? STAND_FIRM_ORDINARY_KNOCKBACK_MULTIPLIER
                : 1.0;
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ACCEPTED_CASTS.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey().playerId());
            if (player == null || !player.isAlive()) {
                return true;
            }
            return player.level().getGameTime()
                    - entry.getValue().acceptedAtTick()
                    > ACCEPTED_CAST_STALE_TICKS;
        });
        BULWARK_DEFENSE_UNTIL.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey());
            return player == null
                    || !player.isAlive()
                    || player.level().getGameTime() >= entry.getValue();
        });
        COUNTERWALL_UNTIL.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey());
            return player == null
                    || !player.isAlive()
                    || player.level().getGameTime() >= entry.getValue();
        });
        ACTIVE_AURAS.entrySet().removeIf(entry -> {
            ActiveAura aura = entry.getValue();
            ServerPlayer guardian = server.getPlayerList()
                    .getPlayer(entry.getKey());
            if (guardian == null
                    || !guardian.isAlive()
                    || !isGuardian(guardian)
                    || guardian.level() != aura.level()) {
                return true;
            }
            long nowTick = aura.level().getGameTime();
            if (nowTick >= aura.expiresAtTick()) {
                return true;
            }
            if (nowTick >= aura.nextProvokedRefreshTick()) {
                refreshAuraProvoked(guardian, aura, nowTick);
                aura.advanceProvokedRefresh();
            }
            return false;
        });
    }

    public static void reset(UUID playerId) {
        if (playerId == null) {
            return;
        }
        ACCEPTED_CASTS.keySet().removeIf(key -> key.playerId().equals(playerId));
        BULWARK_DEFENSE_UNTIL.remove(playerId);
        COUNTERWALL_UNTIL.remove(playerId);
        ACTIVE_AURAS.remove(playerId);
        GuardianProvokedRuntime.clearGuardian(playerId);
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    private static ReleaseResult releaseBulwarkRush(
            ServerPlayer player,
            long nowTick
    ) {
        LivingEntity target = firstRushTarget(player, nowTick);
        double movement = BULWARK_RUSH_DISTANCE;
        if (target != null) {
            Vec3 delta = target.position().subtract(player.position());
            double horizontalDistance = Math.hypot(delta.x, delta.z);
            double contact = (player.getBbWidth() + target.getBbWidth()) * 0.5;
            movement = Math.max(0.0, Math.min(
                    BULWARK_RUSH_DISTANCE,
                    horizontalDistance - contact
            ));
        }
        moveForward(player, movement);
        if (target == null) {
            return new ReleaseResult(true, 0, 0.0, 0.0, 0, 0, 0L);
        }
        HitResolution hit = resolveTargets(
                player,
                List.of(target),
                BULWARK_RUSH_ACTION,
                BULWARK_RUSH_POISE,
                ProjectImpactTransaction.DamageSchool.PHYSICAL,
                false
        );
        return hit.asRelease(0, 0, 0L);
    }

    private static ReleaseResult releaseWardingStrike(
            ServerPlayer player,
            long nowTick
    ) {
        ProjectImpactTransaction.DamageSchool school =
                wardingStrikeSchool(player);
        HitResolution hit = resolveFrontal(
                player,
                WARDING_STRIKE_ACTION,
                WARDING_STRIKE_POISE,
                WARDING_STRIKE_RANGE,
                WARDING_STRIKE_HALF_ANGLE_DEGREES,
                school,
                true
        );
        return hit.asRelease(0, 0, 0L);
    }

    private static ReleaseResult releaseAegisField(
            ServerPlayer player,
            long nowTick
    ) {
        int consumed = GuardianResolveRuntime.consumeAll(player, nowTick);
        double coefficient = AEGIS_FIELD_BASE_BARRIER
                + AEGIS_FIELD_BARRIER_PER_RESOLVE * consumed;
        int barriers = 0;
        for (ServerPlayer ally : playersInside(
                player,
                AEGIS_FIELD_RADIUS
        )) {
            var grant = ProjectBarrierRuntime.applySkillBarrier(
                    player,
                    ally,
                    AEGIS_BARRIER_PREFIX + player.getUUID(),
                    coefficient,
                    0.0,
                    PlayerBarrierAuthority.DEFAULT_BARRIER_DURATION_TICKS,
                    false
            );
            if (grant.accepted() && grant.appliedAmount() > 0.0) {
                barriers++;
            }
        }
        return new ReleaseResult(
                true,
                0,
                0.0,
                0.0,
                barriers,
                consumed,
                0L
        );
    }

    private static ReleaseResult releaseUnbrokenLine(
            ServerPlayer player,
            long nowTick
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return ReleaseResult.rejected();
        }
        long expiresAt = Math.addExact(
                nowTick,
                UNBROKEN_LINE_DURATION_TICKS
        );
        ActiveAura aura = new ActiveAura(
                level,
                expiresAt,
                nowTick
        );
        ACTIVE_AURAS.put(player.getUUID(), aura);

        int barriers = 0;
        for (ServerPlayer ally : playersInside(
                player,
                UNBROKEN_LINE_RADIUS
        )) {
            var grant = ProjectBarrierRuntime.applySkillBarrier(
                    player,
                    ally,
                    UNBROKEN_BARRIER_PREFIX + player.getUUID(),
                    UNBROKEN_LINE_INITIAL_BARRIER,
                    0.0,
                    PlayerBarrierAuthority.DEFAULT_BARRIER_DURATION_TICKS,
                    false
            );
            if (grant.accepted() && grant.appliedAmount() > 0.0) {
                barriers++;
            }
        }
        refreshAuraProvoked(player, aura, nowTick);
        aura.advanceProvokedRefresh();
        return new ReleaseResult(
                true,
                0,
                0.0,
                0.0,
                barriers,
                0,
                expiresAt
        );
    }

    private static HitResolution resolveFrontal(
            ServerPlayer player,
            double actionCoefficient,
            double poiseCoefficient,
            double range,
            double halfAngleDegrees,
            ProjectImpactTransaction.DamageSchool school,
            boolean applyProvoked
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
                                && frontalContains(
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
                school,
                applyProvoked
        );
    }

    private static HitResolution resolveTargets(
            ServerPlayer player,
            List<LivingEntity> targets,
            double actionCoefficient,
            double poiseCoefficient,
            ProjectImpactTransaction.DamageSchool school,
            boolean applyProvoked
    ) {
        var build = CombatStateServices.combatBuilds()
                .build(player.getUUID())
                .orElse(null);
        if (build == null) {
            return HitResolution.empty();
        }
        long nowTick = player.level().getGameTime();
        var source = build.damageSource(school);
        int applied = 0;
        double totalDamage = 0.0;
        double totalPoise = 0.0;

        for (LivingEntity target : targets) {
            var snapshot = ExternalActorBindingRuntime
                    .projectTargetSnapshot(target, nowTick)
                    .orElse(null);
            if (snapshot == null) {
                continue;
            }
            double damage = ProjectImpactTransaction.resolveDirectDamage(
                    new ProjectImpactTransaction.DirectDamageRequest(
                            source,
                            snapshot,
                            school,
                            actionCoefficient,
                            1.0,
                            1.0
                    )
            ).finalDamage();
            double poiseDamage = ProjectImpactTransaction.resolvePoise(
                    new ProjectImpactTransaction.PoiseRequest(
                            snapshot.poiseMax(),
                            snapshot.poiseMax(),
                            source.poiseOutputMultiplier(),
                            poiseCoefficient,
                            1.0,
                            1.0
                    )
            ).poiseDamage();
            boolean damaged = school
                    == ProjectImpactTransaction.DamageSchool.MAGIC
                    ? ProjectMinecraftDamageApplicator.applyDirectMagic(
                            player,
                            target,
                            damage
                    )
                    : ProjectMinecraftDamageApplicator.applyDirectPhysical(
                            player,
                            target,
                            damage
                    );
            if (!damaged) {
                continue;
            }
            applied++;
            totalDamage += damage;
            if (poiseDamage > 0.0) {
                ExternalActorBindingRuntime.applyProjectPoiseDamage(
                        target,
                        poiseDamage,
                        nowTick
                );
                totalPoise += poiseDamage;
            }
            if (applyProvoked) {
                GuardianProvokedRuntime.apply(
                        player,
                        target,
                        nowTick
                );
            }
        }
        if (applied > 0) {
            CombatStateServices.markCombatActivity(
                    player.getUUID(),
                    nowTick
            );
        }
        return new HitResolution(
                applied,
                totalDamage,
                totalPoise
        );
    }

    private static LivingEntity firstRushTarget(
            ServerPlayer player,
            long nowTick
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox()
                                .inflate(BULWARK_RUSH_DISTANCE + 1.0),
                        target -> ExternalActorBindingRuntime
                                .projectTargetSnapshot(target, nowTick)
                                .isPresent()
                                && frontalContains(
                                        player,
                                        target,
                                        BULWARK_RUSH_DISTANCE,
                                        BULWARK_RUSH_HALF_ANGLE_DEGREES
                                )
                                && player.hasLineOfSight(target)
                ).stream()
                .min(
                        Comparator.comparingDouble(
                                (LivingEntity target) ->
                                        player.distanceToSqr(target)
                        ).thenComparingInt(LivingEntity::getId)
                )
                .orElse(null);
    }

    private static void refreshAuraProvoked(
            ServerPlayer guardian,
            ActiveAura aura,
            long nowTick
    ) {
        if (ProjectActiveEncounterRuntime
                .supportEncounterLevel(guardian, guardian)
                .isEmpty()) {
            return;
        }
        for (LivingEntity target : aura.level().getEntitiesOfClass(
                LivingEntity.class,
                guardian.getBoundingBox()
                        .inflate(UNBROKEN_LINE_RADIUS + 1.0),
                target -> ExternalActorBindingRuntime
                        .projectTargetSnapshot(target, nowTick)
                        .isPresent()
                        && radialContains(
                                guardian.position(),
                                target,
                                UNBROKEN_LINE_RADIUS
                        )
        )) {
            GuardianProvokedRuntime.apply(
                    guardian,
                    target,
                    nowTick
            );
        }
    }

    private static List<ServerPlayer> playersInside(
            ServerPlayer center,
            double radius
    ) {
        double radiusSq = radius * radius;
        return center.level().getServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .filter(player -> player.level() == center.level())
                .filter(ServerPlayer::isAlive)
                .filter(player -> !player.isSpectator())
                .filter(player -> horizontalDistanceSq(
                        center.position(),
                        player.position()
                ) <= radiusSq)
                .toList();
    }

    private static boolean insideAnyUnbrokenAura(
            ServerPlayer target,
            long nowTick
    ) {
        for (var entry : ACTIVE_AURAS.entrySet()) {
            ActiveAura aura = entry.getValue();
            if (aura.level() != target.level()
                    || nowTick >= aura.expiresAtTick()) {
                continue;
            }
            ServerPlayer guardian = target.level().getServer()
                    .getPlayerList()
                    .getPlayer(entry.getKey());
            if (guardian != null
                    && guardian.isAlive()
                    && radialContains(
                            guardian.position(),
                            target,
                            UNBROKEN_LINE_RADIUS
                    )) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasGuardCapableSetup(
            ServerPlayer player
    ) {
        return CombatStateServices.defenseSnapshots()
                .snapshot(player.getUUID())
                .flatMap(PlayerDefenseAuthority.DefenseSnapshot::guardType)
                .isPresent();
    }

    private static ProjectImpactTransaction.DamageSchool
    wardingStrikeSchool(ServerPlayer player) {
        var build = CombatStateServices.combatBuilds()
                .build(player.getUUID())
                .orElseThrow();
        ProjectWeaponFamily family =
                build.equipment().weaponFamily();
        return family == ProjectWeaponFamily.STAFF
                || family == ProjectWeaponFamily.WAND
                ? ProjectImpactTransaction.DamageSchool.MAGIC
                : ProjectImpactTransaction.DamageSchool.PHYSICAL;
    }

    private static boolean isGuardianSpell(String spellId) {
        return ProjectSpellSpec.GUARDIAN_BULWARK_RUSH_ID.equals(spellId)
                || ProjectSpellSpec.GUARDIAN_WARDING_STRIKE_ID.equals(spellId)
                || ProjectSpellSpec.GUARDIAN_AEGIS_FIELD_ID.equals(spellId)
                || ProjectSpellSpec.GUARDIAN_COUNTERWALL_ID.equals(spellId)
                || ProjectSpellSpec.GUARDIAN_UNBROKEN_LINE_ID.equals(spellId);
    }

    private static boolean isGuardian(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.GUARDIAN::equals)
                .isPresent();
    }

    private static boolean ownsActiveAction(
            ServerPlayer player,
            String spellId
    ) {
        var snapshot = ProjectPlayerActionRuntime.snapshot(
                player.getUUID(),
                player.level().getGameTime()
        );
        return snapshot.active()
                && snapshot.kind()
                        == dev.moonseungjun.openworldrpg.combat.state
                                .PlayerActionRuntimeState.WindowKind.ACTION
                && spellId.equals(snapshot.actionId());
    }

    private static ProjectPlayerActionRuntime.ActionSpec actionSpec(
            String spellId
    ) {
        return switch (spellId) {
            case ProjectSpellSpec.GUARDIAN_BULWARK_RUSH_ID ->
                    action(spellId, BULWARK_RUSH_ACTION_TICKS, 0.70, 1.0);
            case ProjectSpellSpec.GUARDIAN_WARDING_STRIKE_ID ->
                    action(spellId, WARDING_STRIKE_ACTION_TICKS, 0.70, 1.0);
            case ProjectSpellSpec.GUARDIAN_AEGIS_FIELD_ID ->
                    action(spellId, AEGIS_FIELD_ACTION_TICKS, 0.75, 0.85);
            case ProjectSpellSpec.GUARDIAN_COUNTERWALL_ID ->
                    action(spellId, COUNTERWALL_ACTION_TICKS, 0.80, 0.75);
            case ProjectSpellSpec.GUARDIAN_UNBROKEN_LINE_ID ->
                    action(spellId, UNBROKEN_LINE_ACTION_TICKS, 0.80, 0.80);
            default -> throw new IllegalArgumentException(
                    "Unknown Guardian spell: " + spellId
            );
        };
    }

    private static ProjectPlayerActionRuntime.ActionSpec action(
            String id,
            int ticks,
            double cancelFraction,
            double movementMultiplier
    ) {
        return new ProjectPlayerActionRuntime.ActionSpec(
                id,
                ticks,
                ProjectPlayerActionRuntime.ActionSpec.cancelOffset(
                        ticks,
                        cancelFraction
                ),
                movementMultiplier
        );
    }

    private static void moveForward(
            ServerPlayer player,
            double distance
    ) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() <= 1.0e-9
                || distance <= 0.0) {
            return;
        }
        player.move(
                MoverType.SELF,
                horizontal.normalize().scale(distance)
        );
    }

    private static boolean frontalContains(
            LivingEntity caster,
            LivingEntity target,
            double range,
            double halfAngleDegrees
    ) {
        if (!target.isAlive() || target == caster) {
            return false;
        }
        Vec3 origin = caster.position();
        Vec3 center = target.getBoundingBox().getCenter();
        double vertical = center.y - origin.y;
        if (vertical < -1.25 || vertical > 3.5) {
            return false;
        }
        Vec3 toTarget = new Vec3(
                center.x - origin.x,
                0.0,
                center.z - origin.z
        );
        double distance = toTarget.length();
        if (distance > range + target.getBbWidth() * 0.5
                || distance <= 1.0e-9) {
            return distance <= range;
        }
        Vec3 look = caster.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
        if (flatLook.lengthSqr() <= 1.0e-9) {
            return false;
        }
        return flatLook.normalize().dot(toTarget.normalize())
                + 1.0e-9
                >= Math.cos(Math.toRadians(halfAngleDegrees));
    }

    private static boolean radialContains(
            Vec3 origin,
            LivingEntity target,
            double radius
    ) {
        if (!target.isAlive()) {
            return false;
        }
        var box = target.getBoundingBox();
        if (box.maxY < origin.y - 1.25
                || box.minY > origin.y + 3.5) {
            return false;
        }
        double closestX = Math.max(
                box.minX,
                Math.min(origin.x, box.maxX)
        );
        double closestZ = Math.max(
                box.minZ,
                Math.min(origin.z, box.maxZ)
        );
        double dx = closestX - origin.x;
        double dz = closestZ - origin.z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static double horizontalDistanceSq(
            Vec3 first,
            Vec3 second
    ) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz;
    }

    private record CastKey(UUID playerId, String spellId) {
        private CastKey {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(spellId, "spellId");
        }
    }

    private record AcceptedCast(long acceptedAtTick) {
    }

    private static final class ActiveAura {
        private final ServerLevel level;
        private final long expiresAtTick;
        private long nextProvokedRefreshTick;

        private ActiveAura(
                ServerLevel level,
                long expiresAtTick,
                long nextProvokedRefreshTick
        ) {
            this.level = Objects.requireNonNull(level, "level");
            this.expiresAtTick = expiresAtTick;
            this.nextProvokedRefreshTick = nextProvokedRefreshTick;
        }

        private ServerLevel level() {
            return level;
        }

        private long expiresAtTick() {
            return expiresAtTick;
        }

        private long nextProvokedRefreshTick() {
            return nextProvokedRefreshTick;
        }

        private void advanceProvokedRefresh() {
            nextProvokedRefreshTick = Math.addExact(
                    nextProvokedRefreshTick,
                    UNBROKEN_LINE_PROVOKE_REFRESH_TICKS
            );
        }
    }

    private record HitResolution(
            int targetsHit,
            double totalDamage,
            double totalPoise
    ) {
        private static HitResolution empty() {
            return new HitResolution(0, 0.0, 0.0);
        }

        private ReleaseResult asRelease(
                int barrierRecipients,
                int resolveConsumed,
                long auraExpiresAtTick
        ) {
            return new ReleaseResult(
                    true,
                    targetsHit,
                    totalDamage,
                    totalPoise,
                    barrierRecipients,
                    resolveConsumed,
                    auraExpiresAtTick
            );
        }
    }

    public record ReleaseResult(
            boolean accepted,
            int targetsHit,
            double totalDamage,
            double totalPoise,
            int barrierRecipients,
            int resolveConsumed,
            long auraExpiresAtTick
    ) {
        public ReleaseResult {
            if (targetsHit < 0
                    || !Double.isFinite(totalDamage)
                    || totalDamage < 0.0
                    || !Double.isFinite(totalPoise)
                    || totalPoise < 0.0
                    || barrierRecipients < 0
                    || resolveConsumed < 0
                    || resolveConsumed > 3
                    || auraExpiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Guardian release result."
                );
            }
            if (!accepted
                    && (targetsHit != 0
                    || totalDamage != 0.0
                    || totalPoise != 0.0
                    || barrierRecipients != 0
                    || resolveConsumed != 0
                    || auraExpiresAtTick != 0L)) {
                throw new IllegalArgumentException(
                        "Rejected Guardian release cannot carry state."
                );
            }
        }

        public static ReleaseResult rejected() {
            return new ReleaseResult(
                    false,
                    0,
                    0.0,
                    0.0,
                    0,
                    0,
                    0L
            );
        }
    }
}
