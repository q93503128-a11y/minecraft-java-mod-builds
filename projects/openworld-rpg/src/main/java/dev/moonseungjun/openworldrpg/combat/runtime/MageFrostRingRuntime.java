package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative Mage Active Skill 3: Frost Ring. */
public final class MageFrostRingRuntime {
    public static final double RADIUS_BLOCKS = 4.2;
    public static final double VERTICAL_RANGE_BLOCKS = 2.5;
    public static final double WEAVE_BARRIER_COEFFICIENT = 0.10;
    public static final long PENDING_CAST_STALE_TICKS = 40L;

    private static final String BARRIER_SOURCE_ID =
            "openworld_rpg:frost_ring";
    private static final ConcurrentHashMap<UUID, PendingCast> PENDING =
            new ConcurrentHashMap<>();

    private MageFrostRingRuntime() {
    }

    public static boolean canActivate(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        return isMage(caster)
                && caster.isAlive()
                && !caster.isSpectator()
                && ProjectPlayerActionRuntime.canStartAction(caster)
                && CombatStateServices.combatBuilds()
                        .build(caster.getUUID())
                        .isPresent();
    }

    public static boolean onAcceptedCast(
            ServerPlayer caster,
            boolean weaveEmpowered,
            double weaveMagnitudeMultiplier,
            long nowTick
    ) {
        Objects.requireNonNull(caster, "caster");
        if (!Double.isFinite(weaveMagnitudeMultiplier)
                || weaveMagnitudeMultiplier < 1.0
                || nowTick < 0L
                || !canActivate(caster)) {
            return false;
        }
        PENDING.put(
                caster.getUUID(),
                new PendingCast(
                        caster.level(),
                        nowTick,
                        weaveEmpowered,
                        weaveMagnitudeMultiplier
                )
        );
        return true;
    }

    public static Application release(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        PendingCast pending = PENDING.remove(caster.getUUID());
        if (pending == null
                || pending.level() != caster.level()
                || !isMage(caster)
                || !(caster.level() instanceof ServerLevel level)) {
            return Application.rejected();
        }

        long nowTick = level.getGameTime();
        if (nowTick - pending.acceptedAtTick()
                > PENDING_CAST_STALE_TICKS) {
            return Application.rejected();
        }

        var build = CombatStateServices.combatBuilds()
                .build(caster.getUUID())
                .orElse(null);
        if (build == null) {
            return Application.rejected();
        }

        Vec3 origin = caster.position();
        var source = MageRootPassiveEffects.applyMagicPowerBonus(
                caster,
                build.damageSource(
                        ProjectImpactTransaction.DamageSchool.MAGIC
                )
        );

        List<LivingEntity> targets = targetsInside(
                level,
                caster,
                origin,
                nowTick
        );
        int damagedTargets = 0;
        int chilledTargets = 0;
        double totalDamage = 0.0;
        double totalPoiseDamage = 0.0;

        for (LivingEntity target : targets) {
            var targetSnapshot = ExternalActorBindingRuntime
                    .projectTargetSnapshot(target, nowTick)
                    .orElse(null);
            if (targetSnapshot == null) {
                continue;
            }

            double damage = ProjectImpactTransaction
                    .resolveDirectDamage(
                            new ProjectImpactTransaction
                                    .DirectDamageRequest(
                                            source,
                                            targetSnapshot,
                                            ProjectImpactTransaction
                                                    .DamageSchool.MAGIC,
                                            ProjectSpellSpec
                                                    .FROST_RING_ACTION_COEFFICIENT,
                                            1.0,
                                            1.0
                                    )
                    ).finalDamage();
            double poiseDamage =
                    targetSnapshot.poiseMax() > 0.0
                            ? ProjectImpactTransaction
                                    .resolvePoise(
                                            new ProjectImpactTransaction
                                                    .PoiseRequest(
                                                            targetSnapshot
                                                                    .poiseMax(),
                                                            targetSnapshot
                                                                    .poiseMax(),
                                                            source.poiseOutputMultiplier(),
                                                            ProjectSpellSpec
                                                                    .FROST_RING_POISE_COEFFICIENT,
                                                            1.0,
                                                            1.0
                                                    )
                                    ).poiseDamage()
                            : 0.0;

            if (!ProjectMinecraftDamageApplicator
                    .applyDirectMagic(
                            caster,
                            target,
                            damage
                    )) {
                continue;
            }

            damagedTargets++;
            totalDamage += damage;
            if (poiseDamage > 0.0) {
                ExternalActorBindingRuntime.applyProjectPoiseDamage(
                        target,
                        poiseDamage,
                        nowTick
                );
                totalPoiseDamage += poiseDamage;
            }
            if (ProjectHostileStatusRuntime.applyChilled(
                    target,
                    nowTick
            ).isPresent()) {
                chilledTargets++;
            }
        }

        if (damagedTargets > 0) {
            CombatStateServices.markCombatActivity(
                    caster.getUUID(),
                    nowTick
            );
        }

        ProjectBarrierRuntime.GrantApplication barrier =
                ProjectBarrierRuntime.GrantApplication.rejected();
        if (pending.weaveEmpowered()) {
            barrier = ProjectBarrierRuntime.applySkillBarrier(
                    caster,
                    caster,
                    BARRIER_SOURCE_ID,
                    WEAVE_BARRIER_COEFFICIENT,
                    pending.weaveMagnitudeMultiplier() - 1.0,
                    PlayerBarrierAuthority
                            .DEFAULT_BARRIER_DURATION_TICKS,
                    false
            );
        }

        return new Application(
                true,
                origin,
                targets.size(),
                damagedTargets,
                chilledTargets,
                totalDamage,
                totalPoiseDamage,
                pending.weaveEmpowered(),
                barrier.appliedAmount(),
                barrier.expiresAtTick()
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        PENDING.entrySet().removeIf(entry -> {
            ServerPlayer caster = server.getPlayerList()
                    .getPlayer(entry.getKey());
            PendingCast pending = entry.getValue();
            if (caster == null
                    || !caster.isAlive()
                    || caster.level() != pending.level()
                    || !isMage(caster)) {
                return true;
            }
            return caster.level().getGameTime()
                    - pending.acceptedAtTick()
                    > PENDING_CAST_STALE_TICKS;
        });
    }

    public static void reset(UUID playerId) {
        if (playerId != null) {
            PENDING.remove(playerId);
        }
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    static boolean contains(
            Vec3 origin,
            AABB targetBounds
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(targetBounds, "targetBounds");

        double nearestX = Math.max(
                targetBounds.minX,
                Math.min(origin.x, targetBounds.maxX)
        );
        double nearestZ = Math.max(
                targetBounds.minZ,
                Math.min(origin.z, targetBounds.maxZ)
        );
        double dx = nearestX - origin.x;
        double dz = nearestZ - origin.z;
        boolean verticalOverlap =
                targetBounds.maxY
                        >= origin.y - VERTICAL_RANGE_BLOCKS
                        && targetBounds.minY
                        <= origin.y + VERTICAL_RANGE_BLOCKS;
        return verticalOverlap
                && dx * dx + dz * dz
                        <= RADIUS_BLOCKS * RADIUS_BLOCKS;
    }

    private static List<LivingEntity> targetsInside(
            ServerLevel level,
            ServerPlayer caster,
            Vec3 origin,
            long nowTick
    ) {
        AABB bounds = new AABB(
                origin.x - RADIUS_BLOCKS,
                origin.y - VERTICAL_RANGE_BLOCKS,
                origin.z - RADIUS_BLOCKS,
                origin.x + RADIUS_BLOCKS,
                origin.y + VERTICAL_RANGE_BLOCKS,
                origin.z + RADIUS_BLOCKS
        );
        return level.getEntitiesOfClass(
                LivingEntity.class,
                bounds,
                target -> target != caster
                        && target.isAlive()
                        && contains(
                                origin,
                                target.getBoundingBox()
                        )
                        && ExternalActorBindingRuntime
                                .projectTargetSnapshot(
                                        target,
                                        nowTick
                                ).isPresent()
        ).stream()
                .sorted(
                        Comparator
                                .comparingDouble(
                                        (LivingEntity target) ->
                                                caster.distanceToSqr(target)
                                )
                                .thenComparingInt(
                                        LivingEntity::getId
                                )
                )
                .toList();
    }

    private static boolean isMage(ServerPlayer caster) {
        return caster != null
                && !caster.level().isClientSide()
                && PlayerProgressionService.state(caster)
                        .activeClass()
                        .filter(RootClass.MAGE::equals)
                        .isPresent();
    }

    private record PendingCast(
            net.minecraft.world.level.Level level,
            long acceptedAtTick,
            boolean weaveEmpowered,
            double weaveMagnitudeMultiplier
    ) {
        private PendingCast {
            Objects.requireNonNull(level, "level");
            if (acceptedAtTick < 0L
                    || !Double.isFinite(weaveMagnitudeMultiplier)
                    || weaveMagnitudeMultiplier < 1.0) {
                throw new IllegalArgumentException(
                        "Invalid Frost Ring pending cast."
                );
            }
        }
    }

    public record Application(
            boolean accepted,
            Vec3 origin,
            int eligibleTargets,
            int damagedTargets,
            int chilledTargets,
            double totalDamage,
            double totalPoiseDamage,
            boolean weaveBarrierTriggered,
            double barrierApplied,
            long barrierExpiresAtTick
    ) {
        public Application {
            if (eligibleTargets < 0
                    || damagedTargets < 0
                    || damagedTargets > eligibleTargets
                    || chilledTargets < 0
                    || chilledTargets > damagedTargets
                    || !Double.isFinite(totalDamage)
                    || totalDamage < 0.0
                    || !Double.isFinite(totalPoiseDamage)
                    || totalPoiseDamage < 0.0
                    || !Double.isFinite(barrierApplied)
                    || barrierApplied < 0.0
                    || barrierExpiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Frost Ring application."
                );
            }
            if (!accepted
                    && (origin != null
                    || eligibleTargets != 0
                    || damagedTargets != 0
                    || chilledTargets != 0
                    || totalDamage != 0.0
                    || totalPoiseDamage != 0.0
                    || weaveBarrierTriggered
                    || barrierApplied != 0.0
                    || barrierExpiresAtTick != 0L)) {
                throw new IllegalArgumentException(
                        "Rejected Frost Ring cannot carry applied state."
                );
            }
        }

        public static Application rejected() {
            return new Application(
                    false,
                    null,
                    0,
                    0,
                    0,
                    0.0,
                    0.0,
                    false,
                    0.0,
                    0L
            );
        }
    }
}
