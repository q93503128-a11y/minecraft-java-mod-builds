package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative Mage root Ultimate: Astral Convergence. */
public final class MageAstralConvergenceRuntime {
    public static final double RADIUS_BLOCKS = 6.0;
    public static final double VERTICAL_RANGE_BLOCKS = 3.0;
    public static final long DURATION_TICKS = 100L;
    public static final long PULSE_INTERVAL_TICKS = 20L;
    public static final int PULSE_COUNT = 5;
    public static final long PENDING_CAST_STALE_TICKS = 60L;
    public static final double FINAL_DIRECT_BONUS = 0.15;
    public static final double FINAL_POISE_BONUS = 0.25;
    public static final double MAX_PULL_BLOCKS_PER_PULSE = 0.40;
    public static final double ACTION_COEFFICIENT_PER_PULSE =
            ProjectSpellSpec.ASTRAL_CONVERGENCE_ACTION_COEFFICIENT
                    / PULSE_COUNT;
    public static final double POISE_COEFFICIENT_PER_PULSE =
            ProjectSpellSpec.ASTRAL_CONVERGENCE_POISE_COEFFICIENT
                    / PULSE_COUNT;

    private static final ConcurrentHashMap<UUID, PendingCast> PENDING =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ActiveZone> ACTIVE =
            new ConcurrentHashMap<>();

    private MageAstralConvergenceRuntime() {
    }

    public static boolean canActivate(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        return isMage(caster)
                && caster.isAlive()
                && !caster.isSpectator()
                && ProjectPlayerActionRuntime.canStartAction(caster)
                && CombatStateServices.combatBuilds()
                        .build(caster.getUUID())
                        .isPresent()
                && ProjectUltimateChargeRuntime
                        .canActivateUltimate(caster);
    }

    public static boolean onAcceptedCast(
            ServerPlayer caster,
            long nowTick
    ) {
        Objects.requireNonNull(caster, "caster");
        if (nowTick < 0L
                || !canActivate(caster)
                || !ProjectUltimateChargeRuntime
                        .tryActivateUltimate(caster)) {
            return false;
        }

        OptionalDouble weave =
                MageArcaneWeaveRuntime
                        .consumeWeaveReadyForUltimate(
                                caster,
                                nowTick
                        );
        PENDING.put(
                caster.getUUID(),
                new PendingCast(
                        caster.level(),
                        nowTick,
                        weave.isPresent(),
                        weave.orElse(1.0)
                )
        );
        return true;
    }

    public static boolean release(
            ServerPlayer caster,
            long nowTick
    ) {
        Objects.requireNonNull(caster, "caster");
        PendingCast pending = PENDING.remove(caster.getUUID());
        if (pending == null
                || pending.level() != caster.level()
                || nowTick < pending.acceptedAtTick()
                || nowTick - pending.acceptedAtTick()
                        > PENDING_CAST_STALE_TICKS
                || !isMage(caster)
                || !(caster.level() instanceof ServerLevel level)) {
            return false;
        }

        var build = CombatStateServices.combatBuilds()
                .build(caster.getUUID())
                .orElse(null);
        if (build == null) {
            return false;
        }
        var source = MageRootPassiveEffects.applyMagicPowerBonus(
                caster,
                build.damageSource(
                        ProjectImpactTransaction.DamageSchool.MAGIC
                )
        );
        ACTIVE.put(
                caster.getUUID(),
                new ActiveZone(
                        level,
                        caster.position(),
                        source,
                        pending.weaveEmpowered(),
                        pending.weaveMagnitudeMultiplier(),
                        nowTick,
                        Math.addExact(
                                nowTick,
                                PULSE_INTERVAL_TICKS
                        )
                )
        );
        return true;
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        PENDING.entrySet().removeIf(entry -> {
            ServerPlayer caster = server.getPlayerList()
                    .getPlayer(entry.getKey());
            PendingCast pending = entry.getValue();
            return caster == null
                    || !caster.isAlive()
                    || caster.level() != pending.level()
                    || !isMage(caster)
                    || caster.level().getGameTime()
                            - pending.acceptedAtTick()
                            > PENDING_CAST_STALE_TICKS;
        });

        ACTIVE.entrySet().removeIf(entry -> {
            ActiveZone zone = entry.getValue();
            ServerPlayer caster = server.getPlayerList()
                    .getPlayer(entry.getKey());
            if (caster == null
                    || !caster.isAlive()
                    || caster.level() != zone.level()
                    || !isMage(caster)) {
                return true;
            }

            long nowTick = zone.level().getGameTime();
            while (zone.pulseIndex() < PULSE_COUNT
                    && nowTick >= zone.nextPulseTick()) {
                boolean finalDetonation =
                        zone.pulseIndex() == PULSE_COUNT - 1;
                applyPulse(
                        caster,
                        zone,
                        finalDetonation,
                        nowTick
                );
                zone.advancePulse();
            }
            return zone.pulseIndex() >= PULSE_COUNT
                    || nowTick
                            > Math.addExact(
                                    zone.releasedAtTick(),
                                    DURATION_TICKS
                                            + PULSE_INTERVAL_TICKS
                            );
        });
    }

    public static void reset(UUID playerId) {
        if (playerId == null) {
            return;
        }
        PENDING.remove(playerId);
        ACTIVE.remove(playerId);
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    static PulseResult applyPulse(
            ServerPlayer caster,
            ActiveZone zone,
            boolean finalDetonation,
            long nowTick
    ) {
        double actionCoefficient =
                actionCoefficientForPulse(
                        finalDetonation,
                        zone.weaveEmpowered(),
                        zone.weaveMagnitudeMultiplier()
                );
        double poiseCoefficient =
                poiseCoefficientForPulse(
                        finalDetonation,
                        zone.weaveEmpowered(),
                        zone.weaveMagnitudeMultiplier()
                );

        int damagedTargets = 0;
        double totalDamage = 0.0;
        double totalPoiseDamage = 0.0;
        double totalPulledBlocks = 0.0;
        for (LivingEntity target : targetsInside(
                zone.level(),
                caster,
                zone.origin(),
                nowTick
        )) {
            var targetSnapshot = ExternalActorBindingRuntime
                    .projectTargetSnapshot(target, nowTick)
                    .orElse(null);
            var profile = ExternalActorBindingRuntime
                    .combatProfile(target)
                    .orElse(null);
            if (targetSnapshot == null || profile == null) {
                continue;
            }

            double damage = ProjectImpactTransaction
                    .resolveDirectDamage(
                            new ProjectImpactTransaction
                                    .DirectDamageRequest(
                                            zone.sourceSnapshot(),
                                            targetSnapshot,
                                            ProjectImpactTransaction
                                                    .DamageSchool.MAGIC,
                                            actionCoefficient,
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
                                                            zone.sourceSnapshot()
                                                                    .poiseOutputMultiplier(),
                                                            poiseCoefficient,
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
                ExternalActorBindingRuntime
                        .applyProjectPoiseDamage(
                                target,
                                poiseDamage,
                                nowTick
                        );
                totalPoiseDamage += poiseDamage;
            }

            double pullBlocks = maximumPullBlocks(profile);
            if (pullBlocks > 0.0) {
                var pull = ProjectHostileReactionRuntime.pullToward(
                        target,
                        zone.origin(),
                        pullBlocks
                );
                if (pull.accepted()) {
                    totalPulledBlocks += pull.appliedBlocks();
                }
            }
        }

        if (damagedTargets > 0) {
            CombatStateServices.markCombatActivity(
                    caster.getUUID(),
                    nowTick
            );
        }
        return new PulseResult(
                damagedTargets,
                totalDamage,
                totalPoiseDamage,
                totalPulledBlocks,
                finalDetonation
        );
    }

    static double actionCoefficientForPulse(
            boolean finalDetonation,
            boolean weaveEmpowered,
            double weaveMagnitudeMultiplier
    ) {
        validateMagnitude(weaveMagnitudeMultiplier);
        if (!finalDetonation || !weaveEmpowered) {
            return ACTION_COEFFICIENT_PER_PULSE;
        }
        return ACTION_COEFFICIENT_PER_PULSE
                * (1.0
                        + FINAL_DIRECT_BONUS
                        * weaveMagnitudeMultiplier);
    }

    static double poiseCoefficientForPulse(
            boolean finalDetonation,
            boolean weaveEmpowered,
            double weaveMagnitudeMultiplier
    ) {
        validateMagnitude(weaveMagnitudeMultiplier);
        if (!finalDetonation || !weaveEmpowered) {
            return POISE_COEFFICIENT_PER_PULSE;
        }
        return POISE_COEFFICIENT_PER_PULSE
                * (1.0
                        + FINAL_POISE_BONUS
                        * weaveMagnitudeMultiplier);
    }

    static double maximumPullBlocks(
            ExternalActorCombatProfile profile
    ) {
        Objects.requireNonNull(profile, "profile");
        if (!profile.reactionCapabilities().pullToward()) {
            return 0.0;
        }
        return MAX_PULL_BLOCKS_PER_PULSE
                * profile.reactionCapabilities()
                        .pullStrengthMultiplier();
    }

    static boolean contains(
            Vec3 origin,
            AABB targetBounds
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(targetBounds, "targetBounds");
        if (targetBounds.maxY
                < origin.y - VERTICAL_RANGE_BLOCKS
                || targetBounds.minY
                > origin.y + VERTICAL_RANGE_BLOCKS) {
            return false;
        }

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
        return dx * dx + dz * dz
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
                                                target.position()
                                                        .distanceToSqr(
                                                                origin
                                                        )
                                )
                                .thenComparingInt(
                                        LivingEntity::getId
                                )
                )
                .toList();
    }

    private static void validateMagnitude(
            double weaveMagnitudeMultiplier
    ) {
        if (!Double.isFinite(weaveMagnitudeMultiplier)
                || weaveMagnitudeMultiplier < 1.0) {
            throw new IllegalArgumentException(
                    "Astral Convergence Weave magnitude must be >= 1."
            );
        }
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
                        "Invalid Astral Convergence pending cast."
                );
            }
        }
    }

    static final class ActiveZone {
        private final ServerLevel level;
        private final Vec3 origin;
        private final ProjectImpactTransaction.DamageSourceSnapshot
                sourceSnapshot;
        private final boolean weaveEmpowered;
        private final double weaveMagnitudeMultiplier;
        private final long releasedAtTick;
        private long nextPulseTick;
        private int pulseIndex;

        private ActiveZone(
                ServerLevel level,
                Vec3 origin,
                ProjectImpactTransaction.DamageSourceSnapshot
                        sourceSnapshot,
                boolean weaveEmpowered,
                double weaveMagnitudeMultiplier,
                long releasedAtTick,
                long nextPulseTick
        ) {
            this.level = Objects.requireNonNull(level, "level");
            this.origin = Objects.requireNonNull(origin, "origin");
            this.sourceSnapshot = Objects.requireNonNull(
                    sourceSnapshot,
                    "sourceSnapshot"
            );
            validateMagnitude(weaveMagnitudeMultiplier);
            if (releasedAtTick < 0L
                    || nextPulseTick <= releasedAtTick) {
                throw new IllegalArgumentException(
                        "Invalid Astral Convergence active timing."
                );
            }
            this.weaveEmpowered = weaveEmpowered;
            this.weaveMagnitudeMultiplier =
                    weaveMagnitudeMultiplier;
            this.releasedAtTick = releasedAtTick;
            this.nextPulseTick = nextPulseTick;
        }

        private ServerLevel level() {
            return level;
        }

        private Vec3 origin() {
            return origin;
        }

        private ProjectImpactTransaction.DamageSourceSnapshot
                sourceSnapshot() {
            return sourceSnapshot;
        }

        private boolean weaveEmpowered() {
            return weaveEmpowered;
        }

        private double weaveMagnitudeMultiplier() {
            return weaveMagnitudeMultiplier;
        }

        private long releasedAtTick() {
            return releasedAtTick;
        }

        private long nextPulseTick() {
            return nextPulseTick;
        }

        private int pulseIndex() {
            return pulseIndex;
        }

        private void advancePulse() {
            pulseIndex++;
            nextPulseTick = Math.addExact(
                    nextPulseTick,
                    PULSE_INTERVAL_TICKS
            );
        }
    }

    record PulseResult(
            int damagedTargets,
            double totalDamage,
            double totalPoiseDamage,
            double totalPulledBlocks,
            boolean finalDetonation
    ) {
        PulseResult {
            if (damagedTargets < 0
                    || !Double.isFinite(totalDamage)
                    || totalDamage < 0.0
                    || !Double.isFinite(totalPoiseDamage)
                    || totalPoiseDamage < 0.0
                    || !Double.isFinite(totalPulledBlocks)
                    || totalPulledBlocks < 0.0) {
                throw new IllegalArgumentException(
                        "Invalid Astral Convergence pulse result."
                );
            }
        }
    }
}
