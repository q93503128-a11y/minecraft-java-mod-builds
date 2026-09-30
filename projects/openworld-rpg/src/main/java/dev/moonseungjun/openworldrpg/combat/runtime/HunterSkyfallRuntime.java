package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.HunterPowerShotRules;
import dev.moonseungjun.openworldrpg.combat.authority.HunterSkyfallRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HunterSkyfallRuntime {
    public static final long PENDING_CAST_STALE_TICKS = 40L;
    private static final double TARGET_VERTICAL_RANGE_BLOCKS = 4.0;

    private static final ConcurrentHashMap<UUID, PendingCast> PENDING =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ActiveZone> ACTIVE =
            new ConcurrentHashMap<>();

    private HunterSkyfallRuntime() {
    }

    public static boolean canActivate(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!isHunter(player)
                || player.level().isClientSide()
                || !player.isAlive()
                || player.isSpectator()
                || !ProjectPlayerActionRuntime.canStartAction(player)
                || !ProjectUltimateChargeRuntime.canActivateUltimate(player)) {
            return false;
        }
        var build = CombatStateServices.combatBuilds()
                .build(player.getUUID())
                .orElse(null);
        return build != null
                && HunterPowerShotRules
                        .supportsCurrentProductionRangedWeapon(
                                build.equipment().weaponFamily()
                        );
    }

    public static boolean onAcceptedCast(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!canActivate(player)) {
            return false;
        }
        long nowTick = player.level().getGameTime();
        var action = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        ProjectSpellSpec.HUNTER_SKYFALL_ID,
                        HunterSkyfallRules.CAST_TICKS,
                        HunterSkyfallRules.CAST_TICKS,
                        1.0
                )
        );
        if (!action.accepted()) {
            return false;
        }
        PENDING.put(
                player.getUUID(),
                new PendingCast(
                        (ServerLevel) player.level(),
                        nowTick
                )
        );
        return true;
    }

    public static MeteorPlacement onMeteorLaunch(
            ServerPlayer hunter,
            Vec3 engineLaunchPosition,
            int sequenceIndex
    ) {
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(
                engineLaunchPosition,
                "engineLaunchPosition"
        );
        if (sequenceIndex < 0
                || sequenceIndex
                        >= HunterSkyfallRules.VISUAL_PROJECTILE_COUNT
                || !isHunter(hunter)
                || !(hunter.level() instanceof ServerLevel level)) {
            return MeteorPlacement.rejected();
        }

        UUID hunterId = hunter.getUUID();
        ActiveZone zone = ACTIVE.get(hunterId);
        if (sequenceIndex == 0) {
            PendingCast pending = PENDING.get(hunterId);
            if (pending == null
                    || pending.level() != level
                    || level.getGameTime()
                            - pending.acceptedAtTick()
                            > PENDING_CAST_STALE_TICKS) {
                return MeteorPlacement.rejected();
            }

            Vec3 rawTarget = engineLaunchPosition.subtract(
                    0.0,
                    HunterSkyfallRules.VISUAL_LAUNCH_HEIGHT_BLOCKS,
                    0.0
            );
            Vec3 center = resolveGroundCenter(
                    level,
                    rawTarget
            ).orElse(null);
            if (center == null
                    || hunter.position().distanceToSqr(center)
                            > HunterSkyfallRules.TARGET_RANGE_BLOCKS
                            * HunterSkyfallRules.TARGET_RANGE_BLOCKS
                    || !columnClear(
                            level,
                            center.x,
                            center.z,
                            center.y + 0.05,
                            center.y
                                    + HunterSkyfallRules
                                            .VISUAL_LAUNCH_HEIGHT_BLOCKS
                    )
                    || !ProjectUltimateChargeRuntime
                            .tryActivateUltimate(hunter)) {
                PENDING.remove(hunterId, pending);
                return MeteorPlacement.rejected();
            }

            long nowTick = level.getGameTime();
            zone = new ActiveZone(
                    level,
                    center,
                    nowTick,
                    nowTick
                            + HunterSkyfallRules
                                    .FIRST_PULSE_DELAY_TICKS,
                    nowTick
                            + HunterSkyfallRules.DURATION_TICKS,
                    0
            );
            ACTIVE.put(hunterId, zone);
            PENDING.remove(hunterId, pending);
        } else if (zone == null
                || zone.level() != level
                || level.getGameTime() >= zone.expiresAtTick()) {
            return MeteorPlacement.rejected();
        }

        var offset = HunterSkyfallRules.visualOffset(
                sequenceIndex
        );
        Vec3 approximate = zone.origin().add(
                offset.x(),
                0.0,
                offset.z()
        );
        Vec3 localGround = resolveGroundCenter(
                level,
                approximate
        ).orElse(null);
        if (localGround == null
                || horizontalDistanceSqr(
                        zone.origin(),
                        localGround
                ) > HunterSkyfallRules.RADIUS_BLOCKS
                        * HunterSkyfallRules.RADIUS_BLOCKS
                || !columnClear(
                        level,
                        localGround.x,
                        localGround.z,
                        localGround.y + 0.05,
                        localGround.y
                                + HunterSkyfallRules
                                        .VISUAL_LAUNCH_HEIGHT_BLOCKS
                )) {
            return MeteorPlacement.rejected();
        }

        return new MeteorPlacement(
                true,
                localGround.add(
                        0.0,
                        HunterSkyfallRules
                                .VISUAL_LAUNCH_HEIGHT_BLOCKS,
                        0.0
                )
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        PENDING.entrySet().removeIf(entry -> {
            ServerPlayer hunter = server.getPlayerList()
                    .getPlayer(entry.getKey());
            PendingCast pending = entry.getValue();
            if (hunter == null
                    || !hunter.isAlive()
                    || hunter.level() != pending.level()
                    || !isHunter(hunter)) {
                return true;
            }
            return pending.level().getGameTime()
                    - pending.acceptedAtTick()
                    > PENDING_CAST_STALE_TICKS;
        });

        ACTIVE.entrySet().removeIf(entry -> {
            ServerPlayer hunter = server.getPlayerList()
                    .getPlayer(entry.getKey());
            ActiveZone zone = entry.getValue();
            if (hunter == null
                    || !hunter.isAlive()
                    || hunter.level() != zone.level()
                    || !isHunter(hunter)) {
                return true;
            }

            long nowTick = zone.level().getGameTime();
            if (nowTick >= zone.expiresAtTick()) {
                return true;
            }

            List<LivingEntity> targets =
                    targetsInside(zone);
            for (LivingEntity target : targets) {
                ProjectHostileStatusRuntime
                        .applySkyfallSlow(
                                target,
                                nowTick
                        );
            }

            while (zone.pulseIndex()
                            < HunterSkyfallRules.PULSE_COUNT
                    && nowTick >= zone.nextPulseTick()) {
                applyPulse(
                        hunter,
                        zone,
                        targets,
                        nowTick
                );
                zone.advancePulse();
            }
            return false;
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

    public static int activeZoneCount() {
        return ACTIVE.size();
    }

    private static void applyPulse(
            ServerPlayer hunter,
            ActiveZone zone,
            List<LivingEntity> targets,
            long nowTick
    ) {
        var build = CombatStateServices.combatBuilds()
                .build(hunter.getUUID())
                .orElse(null);
        if (build == null) {
            return;
        }
        var source = build.damageSource(
                ProjectImpactTransaction.DamageSchool.PHYSICAL
        );

        for (LivingEntity target : targets) {
            var targetSnapshot = ExternalActorBindingRuntime
                    .projectTargetSnapshot(
                            target,
                            nowTick
                    )
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
                                                    .DamageSchool.PHYSICAL,
                                            HunterSkyfallRules
                                                    .ACTION_COEFFICIENT_PER_PULSE,
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
                                    HunterSkyfallRules
                                            .POISE_COEFFICIENT_PER_PULSE,
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
                continue;
            }
            CombatStateServices.markCombatActivity(
                    hunter.getUUID(),
                    nowTick
            );

            if (poiseDamage <= 0.0) {
                continue;
            }
            var poise = ExternalActorBindingRuntime
                    .applyProjectPoiseDamage(
                            target,
                            poiseDamage,
                            nowTick
                    )
                    .orElse(null);
            if (poise != null
                    && poise.breakTriggered()) {
                ProjectUltimateChargeRuntime
                        .recordHunterRangedPoiseBreak(
                                hunter,
                                target
                        );
            }
        }
    }

    private static List<LivingEntity> targetsInside(
            ActiveZone zone
    ) {
        Vec3 origin = zone.origin();
        double radius = HunterSkyfallRules.RADIUS_BLOCKS;
        AABB bounds = new AABB(
                origin.x - radius,
                origin.y - TARGET_VERTICAL_RANGE_BLOCKS,
                origin.z - radius,
                origin.x + radius,
                origin.y + TARGET_VERTICAL_RANGE_BLOCKS,
                origin.z + radius
        );
        return zone.level().getEntitiesOfClass(
                LivingEntity.class,
                bounds,
                target -> target.isAlive()
                        && ExternalActorBindingRuntime
                                .combatProfile(target)
                                .isPresent()
                        && horizontalDistanceSqr(
                                origin,
                                target.position()
                        ) <= radius * radius
                        && rainExposed(
                                zone.level(),
                                target
                        )
        ).stream()
                .sorted(
                        Comparator.comparingInt(
                                LivingEntity::getId
                        )
                )
                .toList();
    }

    private static boolean rainExposed(
            ServerLevel level,
            LivingEntity target
    ) {
        double x = target.getX();
        double z = target.getZ();
        double startY = target.getBoundingBox().maxY
                + 0.05;
        return columnClear(
                level,
                x,
                z,
                startY,
                startY
                        + HunterSkyfallRules
                                .VISUAL_LAUNCH_HEIGHT_BLOCKS
        );
    }

    private static java.util.Optional<Vec3> resolveGroundCenter(
            ServerLevel level,
            Vec3 approximate
    ) {
        int x = Mth.floor(approximate.x);
        int z = Mth.floor(approximate.z);
        int startY = Mth.floor(approximate.y) + 2;
        int minY = Math.max(
                level.getMinY(),
                startY - 8
        );
        for (int y = Math.min(
                level.getMaxY() - 1,
                startY
        ); y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            var shape = level.getBlockState(pos)
                    .getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            double top = y + shape.max(Direction.Axis.Y);
            if (!Double.isFinite(top)) {
                continue;
            }
            return java.util.Optional.of(
                    new Vec3(
                            approximate.x,
                            top,
                            approximate.z
                    )
            );
        }
        return java.util.Optional.empty();
    }

    private static boolean columnClear(
            ServerLevel level,
            double x,
            double z,
            double minY,
            double maxY
    ) {
        if (maxY <= minY) {
            return false;
        }
        AABB column = new AABB(
                x - 0.12,
                minY,
                z - 0.12,
                x + 0.12,
                maxY,
                z + 0.12
        );
        return !level.getBlockCollisions(
                null,
                column
        ).iterator().hasNext();
    }

    private static double horizontalDistanceSqr(
            Vec3 a,
            Vec3 b
    ) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static boolean isHunter(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.HUNTER::equals)
                .isPresent();
    }

    public record MeteorPlacement(
            boolean accepted,
            Vec3 launchPosition
    ) {
        public MeteorPlacement {
            if (accepted
                    && launchPosition == null) {
                throw new IllegalArgumentException(
                        "Accepted Skyfall meteor requires a launch position."
                );
            }
            if (!accepted
                    && launchPosition != null) {
                throw new IllegalArgumentException(
                        "Rejected Skyfall meteor cannot carry a launch position."
                );
            }
        }

        public static MeteorPlacement rejected() {
            return new MeteorPlacement(
                    false,
                    null
            );
        }
    }

    private record PendingCast(
            ServerLevel level,
            long acceptedAtTick
    ) {
    }

    private static final class ActiveZone {
        private final ServerLevel level;
        private final Vec3 origin;
        private final long startedAtTick;
        private long nextPulseTick;
        private final long expiresAtTick;
        private int pulseIndex;

        private ActiveZone(
                ServerLevel level,
                Vec3 origin,
                long startedAtTick,
                long nextPulseTick,
                long expiresAtTick,
                int pulseIndex
        ) {
            this.level = level;
            this.origin = origin;
            this.startedAtTick = startedAtTick;
            this.nextPulseTick = nextPulseTick;
            this.expiresAtTick = expiresAtTick;
            this.pulseIndex = pulseIndex;
        }

        private ServerLevel level() {
            return level;
        }

        private Vec3 origin() {
            return origin;
        }

        private long nextPulseTick() {
            return nextPulseTick;
        }

        private long expiresAtTick() {
            return expiresAtTick;
        }

        private int pulseIndex() {
            return pulseIndex;
        }

        private void advancePulse() {
            pulseIndex++;
            nextPulseTick +=
                    HunterSkyfallRules.PULSE_INTERVAL_TICKS;
        }
    }
}
