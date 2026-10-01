package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative Mage Active Skill 4: Flame Burst. */
public final class MageFlameBurstRuntime {
    public static final double GROUND_TARGET_RANGE_BLOCKS = 10.0;
    public static final double RADIUS_BLOCKS = 3.5;
    public static final double BELOW_ORIGIN_BLOCKS = 1.25;
    public static final double ABOVE_ORIGIN_BLOCKS = 3.0;
    public static final long DURATION_TICKS = 50L;
    public static final long CLOUD_PULSE_INTERVAL_TICKS = 10L;
    public static final int FULL_DURATION_PULSE_COUNT = 4;
    public static final long PENDING_CAST_STALE_TICKS = 40L;

    private static final ConcurrentHashMap<UUID, PendingCast> PENDING =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ActiveCast> ACTIVE =
            new ConcurrentHashMap<>();

    private MageFlameBurstRuntime() {
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
                new ActiveCast(
                        level,
                        caster.position(),
                        source,
                        pending.weaveEmpowered(),
                        pending.weaveMagnitudeMultiplier(),
                        nowTick,
                        Math.addExact(nowTick, DURATION_TICKS)
                )
        );
        return true;
    }

    public static PulseApplication applyCloudPulse(
            ServerPlayer caster,
            LivingEntity target,
            Vec3 center,
            long nowTick
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(center, "center");
        ActiveCast active = ACTIVE.get(caster.getUUID());
        if (active == null
                || active.level() != caster.level()
                || target.level() != caster.level()
                || !target.isAlive()
                || target == caster
                || nowTick < active.releasedAtTick()
                || nowTick >= active.expiresAtTick()
                || !isMage(caster)
                || !validCenter(active.releaseOrigin(), center)
                || !contains(center, target.getBoundingBox())
                || !active.tryClaimPulse(target.getUUID(), nowTick)) {
            return PulseApplication.rejected();
        }

        var targetSnapshot = ExternalActorBindingRuntime
                .projectTargetSnapshot(target, nowTick)
                .orElse(null);
        if (targetSnapshot == null) {
            return PulseApplication.rejected();
        }

        double directTotal =
                effectiveDirectTotalCoefficient(
                        active.weaveEmpowered(),
                        active.weaveMagnitudeMultiplier()
                );
        double directCoefficient =
                directTotal / FULL_DURATION_PULSE_COUNT;
        double poiseCoefficient =
                ProjectSpellSpec.FLAME_BURST_POISE_COEFFICIENT
                        / FULL_DURATION_PULSE_COUNT;

        double damage = ProjectImpactTransaction
                .resolveDirectDamage(
                        new ProjectImpactTransaction
                                .DirectDamageRequest(
                                        active.sourceSnapshot(),
                                        targetSnapshot,
                                        ProjectImpactTransaction
                                                .DamageSchool.MAGIC,
                                        directCoefficient,
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
                                                        active.sourceSnapshot()
                                                                .poiseOutputMultiplier(),
                                                        poiseCoefficient,
                                                        1.0,
                                                        1.0
                                                )
                                ).poiseDamage()
                        : 0.0;

        if (!ProjectMinecraftDamageApplicator.applyDirectMagic(
                caster,
                target,
                damage
        )) {
            return PulseApplication.rejected();
        }

        if (poiseDamage > 0.0) {
            ExternalActorBindingRuntime.applyProjectPoiseDamage(
                    target,
                    poiseDamage,
                    nowTick
            );
        }

        boolean burningApplied = false;
        if (active.markBurningApplied(target.getUUID())) {
            double burningTotal =
                    effectiveBurningTotalCoefficient(
                            active.weaveEmpowered(),
                            active.weaveMagnitudeMultiplier()
                    );
            burningApplied = ProjectBurningRuntime.apply(
                    caster,
                    target,
                    active.sourceSnapshot(),
                    burningTotal,
                    nowTick
            ).accepted();
        }

        CombatStateServices.markCombatActivity(
                caster.getUUID(),
                nowTick
        );
        return new PulseApplication(
                true,
                damage,
                poiseDamage,
                directCoefficient,
                burningApplied
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        PENDING.entrySet().removeIf(entry -> {
            ServerPlayer caster = server.getPlayerList()
                    .getPlayer(entry.getKey());
            PendingCast pending = entry.getValue();
            return caster == null
                    || caster.level() != pending.level()
                    || !caster.isAlive()
                    || !isMage(caster)
                    || caster.level().getGameTime()
                            - pending.acceptedAtTick()
                            > PENDING_CAST_STALE_TICKS;
        });
        ACTIVE.entrySet().removeIf(entry -> {
            ServerPlayer caster = server.getPlayerList()
                    .getPlayer(entry.getKey());
            ActiveCast active = entry.getValue();
            if (caster == null
                    || caster.level() != active.level()
                    || !caster.isAlive()
                    || !isMage(caster)) {
                return true;
            }
            return caster.level().getGameTime()
                    >= active.expiresAtTick();
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

    static double effectiveDirectTotalCoefficient(
            boolean weaveEmpowered,
            double weaveMagnitudeMultiplier
    ) {
        validateMagnitude(weaveMagnitudeMultiplier);
        if (!weaveEmpowered) {
            return ProjectSpellSpec
                    .FLAME_BURST_DIRECT_ACTION_COEFFICIENT;
        }
        double bonus = ProjectSpellSpec
                .FLAME_BURST_WEAVE_DIRECT_ACTION_COEFFICIENT
                - ProjectSpellSpec
                        .FLAME_BURST_DIRECT_ACTION_COEFFICIENT;
        return ProjectSpellSpec
                .FLAME_BURST_DIRECT_ACTION_COEFFICIENT
                + bonus * weaveMagnitudeMultiplier;
    }

    static double effectiveBurningTotalCoefficient(
            boolean weaveEmpowered,
            double weaveMagnitudeMultiplier
    ) {
        validateMagnitude(weaveMagnitudeMultiplier);
        if (!weaveEmpowered) {
            return ProjectSpellSpec
                    .FLAME_BURST_BURNING_ACTION_COEFFICIENT;
        }
        double bonus = ProjectSpellSpec
                .FLAME_BURST_WEAVE_BURNING_ACTION_COEFFICIENT
                - ProjectSpellSpec
                        .FLAME_BURST_BURNING_ACTION_COEFFICIENT;
        return ProjectSpellSpec
                .FLAME_BURST_BURNING_ACTION_COEFFICIENT
                + bonus * weaveMagnitudeMultiplier;
    }

    static boolean validCenter(
            Vec3 releaseOrigin,
            Vec3 center
    ) {
        Objects.requireNonNull(releaseOrigin, "releaseOrigin");
        Objects.requireNonNull(center, "center");
        double dx = center.x - releaseOrigin.x;
        double dy = center.y - releaseOrigin.y;
        double dz = center.z - releaseOrigin.z;
        double tolerance = GROUND_TARGET_RANGE_BLOCKS + 1.0;
        return dx * dx + dy * dy + dz * dz
                <= tolerance * tolerance;
    }

    static boolean contains(
            Vec3 center,
            AABB targetBounds
    ) {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(targetBounds, "targetBounds");
        if (targetBounds.maxY
                < center.y - BELOW_ORIGIN_BLOCKS
                || targetBounds.minY
                > center.y + ABOVE_ORIGIN_BLOCKS) {
            return false;
        }

        double nearestX = Math.max(
                targetBounds.minX,
                Math.min(center.x, targetBounds.maxX)
        );
        double nearestZ = Math.max(
                targetBounds.minZ,
                Math.min(center.z, targetBounds.maxZ)
        );
        double dx = nearestX - center.x;
        double dz = nearestZ - center.z;
        return dx * dx + dz * dz
                <= RADIUS_BLOCKS * RADIUS_BLOCKS;
    }

    private static void validateMagnitude(
            double weaveMagnitudeMultiplier
    ) {
        if (!Double.isFinite(weaveMagnitudeMultiplier)
                || weaveMagnitudeMultiplier < 1.0) {
            throw new IllegalArgumentException(
                    "Flame Burst Weave magnitude must be >= 1."
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
                        "Invalid Flame Burst pending cast."
                );
            }
        }
    }

    private static final class ActiveCast {
        private final ServerLevel level;
        private final Vec3 releaseOrigin;
        private final ProjectImpactTransaction.DamageSourceSnapshot
                sourceSnapshot;
        private final boolean weaveEmpowered;
        private final double weaveMagnitudeMultiplier;
        private final long releasedAtTick;
        private final long expiresAtTick;
        private final Map<UUID, Long> lastPulseByTarget =
                new HashMap<>();
        private final Set<UUID> burningAppliedTargets =
                new HashSet<>();

        private ActiveCast(
                ServerLevel level,
                Vec3 releaseOrigin,
                ProjectImpactTransaction.DamageSourceSnapshot
                        sourceSnapshot,
                boolean weaveEmpowered,
                double weaveMagnitudeMultiplier,
                long releasedAtTick,
                long expiresAtTick
        ) {
            this.level = Objects.requireNonNull(level, "level");
            this.releaseOrigin =
                    Objects.requireNonNull(
                            releaseOrigin,
                            "releaseOrigin"
                    );
            this.sourceSnapshot =
                    Objects.requireNonNull(
                            sourceSnapshot,
                            "sourceSnapshot"
                    );
            validateMagnitude(weaveMagnitudeMultiplier);
            if (releasedAtTick < 0L
                    || expiresAtTick <= releasedAtTick) {
                throw new IllegalArgumentException(
                        "Invalid Flame Burst active window."
                );
            }
            this.weaveEmpowered = weaveEmpowered;
            this.weaveMagnitudeMultiplier =
                    weaveMagnitudeMultiplier;
            this.releasedAtTick = releasedAtTick;
            this.expiresAtTick = expiresAtTick;
        }

        private ServerLevel level() {
            return level;
        }

        private Vec3 releaseOrigin() {
            return releaseOrigin;
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

        private long expiresAtTick() {
            return expiresAtTick;
        }

        private boolean tryClaimPulse(
                UUID targetId,
                long nowTick
        ) {
            Long previous = lastPulseByTarget.put(
                    targetId,
                    nowTick
            );
            return previous == null
                    || previous.longValue() != nowTick;
        }

        private boolean markBurningApplied(UUID targetId) {
            return burningAppliedTargets.add(targetId);
        }
    }

    public record PulseApplication(
            boolean accepted,
            double damage,
            double poiseDamage,
            double directCoefficient,
            boolean burningApplied
    ) {
        public PulseApplication {
            if (!accepted
                    && (damage != 0.0
                    || poiseDamage != 0.0
                    || directCoefficient != 0.0
                    || burningApplied)) {
                throw new IllegalArgumentException(
                        "Rejected Flame Burst pulse cannot carry output."
                );
            }
            if (accepted
                    && (!Double.isFinite(damage)
                    || damage <= 0.0
                    || !Double.isFinite(poiseDamage)
                    || poiseDamage < 0.0
                    || !Double.isFinite(directCoefficient)
                    || directCoefficient <= 0.0)) {
                throw new IllegalArgumentException(
                        "Invalid Flame Burst pulse result."
                );
            }
        }

        public static PulseApplication rejected() {
            return new PulseApplication(
                    false,
                    0.0,
                    0.0,
                    0.0,
                    false
            );
        }
    }
}
