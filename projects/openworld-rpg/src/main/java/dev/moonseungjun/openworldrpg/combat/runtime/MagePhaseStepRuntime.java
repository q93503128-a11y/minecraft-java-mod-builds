package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative Mage Phase Step displacement, brief invulnerability and Weave field.
 *
 * <p>Spell Engine owns input/cast presentation only. The project resolves path blocking, final
 * collision-safe destination, invulnerability and the non-damaging Weave field.</p>
 */
public final class MagePhaseStepRuntime {
    public static final double MAX_DISTANCE_BLOCKS = 5.0;
    public static final double MIN_ACCEPTED_DISTANCE_BLOCKS = 0.50;
    public static final double DESTINATION_SEARCH_STEP_BLOCKS = 0.25;
    public static final long INVULNERABILITY_TICKS = 2L;
    public static final double WEAVE_FIELD_RADIUS_BLOCKS = 2.5;
    public static final long WEAVE_FIELD_DURATION_TICKS = 60L;
    public static final long FIELD_VISUAL_INTERVAL_TICKS = 5L;
    private static final double FIELD_VERTICAL_TOLERANCE_BLOCKS = 2.0;
    private static final int FIELD_RING_PARTICLES = 12;

    private static final ConcurrentHashMap<UUID, Long>
            INVULNERABLE_UNTIL = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ActiveField>
            ACTIVE_FIELDS = new ConcurrentHashMap<>();

    private MagePhaseStepRuntime() {
    }

    public static boolean canActivate(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return isMage(player)
                && player.isAlive()
                && !player.isSpectator()
                && ProjectPlayerActionRuntime.canStartAction(player)
                && findSafeDestination(player).isPresent();
    }

    public static Activation activate(
            ServerPlayer player,
            boolean weaveEmpowered,
            double weaveUtilityMagnitudeMultiplier
    ) {
        Objects.requireNonNull(player, "player");
        if (!Double.isFinite(weaveUtilityMagnitudeMultiplier)
                || weaveUtilityMagnitudeMultiplier < 1.0
                || !isMage(player)
                || !player.isAlive()
                || player.isSpectator()) {
            return Activation.rejected();
        }

        var destination = findSafeDestination(player).orElse(null);
        if (destination == null) {
            return Activation.rejected();
        }

        ServerLevel level = (ServerLevel) player.level();
        long nowTick = level.getGameTime();
        Vec3 origin = player.position();
        CombatStateServices.defenseStates()
                .getOrCreate(player.getUUID())
                .releaseGuard();

        player.teleportTo(
                level,
                destination.position().x,
                destination.position().y,
                destination.position().z,
                Set.of(),
                player.getYRot(),
                player.getXRot(),
                false
        );
        INVULNERABLE_UNTIL.put(
                player.getUUID(),
                Math.addExact(nowTick, INVULNERABILITY_TICKS)
        );

        if (weaveEmpowered) {
            ACTIVE_FIELDS.put(
                    player.getUUID(),
                    new ActiveField(
                            level,
                            origin,
                            weaveUtilityMagnitudeMultiplier,
                            Math.addExact(
                                    nowTick,
                                    WEAVE_FIELD_DURATION_TICKS
                            ),
                            nowTick
                    )
            );
            spawnFieldRing(level, origin);
        }

        return new Activation(
                true,
                origin,
                destination.position(),
                destination.horizontalDistance(),
                Math.addExact(nowTick, INVULNERABILITY_TICKS),
                weaveEmpowered,
                weaveEmpowered
                        ? Math.addExact(
                                nowTick,
                                WEAVE_FIELD_DURATION_TICKS
                        )
                        : 0L
        );
    }

    public static boolean invulnerable(
            ServerPlayer player,
            long nowTick
    ) {
        Objects.requireNonNull(player, "player");
        Long until = INVULNERABLE_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (nowTick >= until) {
            INVULNERABLE_UNTIL.remove(
                    player.getUUID(),
                    until
            );
            return false;
        }
        return true;
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        INVULNERABLE_UNTIL.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(entry.getKey());
            return player == null
                    || !player.isAlive()
                    || player.level().getGameTime()
                            >= entry.getValue();
        });

        ACTIVE_FIELDS.entrySet().removeIf(entry -> {
            ActiveField field = entry.getValue();
            ServerPlayer caster = server.getPlayerList()
                    .getPlayer(entry.getKey());
            long nowTick = field.level().getGameTime();
            if (caster == null
                    || !caster.isAlive()
                    || caster.level() != field.level()
                    || !isMage(caster)
                    || nowTick >= field.expiresAtTick()) {
                return true;
            }

            applyFieldSlow(caster, field, nowTick);
            if (nowTick >= field.nextVisualTick()) {
                spawnFieldRing(field.level(), field.origin());
                field.advanceVisualTick();
            }
            return false;
        });
    }

    public static void reset(UUID playerId) {
        if (playerId == null) {
            return;
        }
        INVULNERABLE_UNTIL.remove(playerId);
        ACTIVE_FIELDS.remove(playerId);
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    static double pathLimitBeforeWall(
            double blockHitDistance,
            double playerHalfWidth
    ) {
        if (!Double.isFinite(blockHitDistance)
                || blockHitDistance < 0.0
                || !Double.isFinite(playerHalfWidth)
                || playerHalfWidth < 0.0) {
            throw new IllegalArgumentException(
                    "Invalid Phase Step path-limit input."
            );
        }
        double padding = Math.max(
                0.35,
                playerHalfWidth + 0.05
        );
        return Math.max(
                0.0,
                Math.min(
                        MAX_DISTANCE_BLOCKS,
                        blockHitDistance - padding
                )
        );
    }

    private static Optional<SafeDestination> findSafeDestination(
            ServerPlayer player
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }

        Vec3 direction = horizontalLook(player);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(
                direction.scale(MAX_DISTANCE_BLOCKS)
        );
        HitResult hit = level.clip(
                new ClipContext(
                        eye,
                        end,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        double maxDistance = MAX_DISTANCE_BLOCKS;
        if (hit.getType() == HitResult.Type.BLOCK) {
            maxDistance = pathLimitBeforeWall(
                    eye.distanceTo(hit.getLocation()),
                    player.getBbWidth() * 0.5
            );
        }

        Vec3 origin = player.position();
        double[] verticalOffsets = {0.0, 1.0, -1.0};
        for (double distance = maxDistance;
                distance + 1.0e-9
                        >= MIN_ACCEPTED_DISTANCE_BLOCKS;
                distance -= DESTINATION_SEARCH_STEP_BLOCKS) {
            for (double vertical : verticalOffsets) {
                Vec3 candidate = origin
                        .add(direction.scale(distance))
                        .add(0.0, vertical, 0.0);
                Vec3 delta = candidate.subtract(origin);
                AABB movedBox = player.getBoundingBox()
                        .move(delta);
                if (level.noCollision(player, movedBox)) {
                    return Optional.of(
                            new SafeDestination(
                                    candidate,
                                    Math.hypot(
                                            candidate.x - origin.x,
                                            candidate.z - origin.z
                                    )
                            )
                    );
                }
            }
        }
        return Optional.empty();
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(
                look.x,
                0.0,
                look.z
        );
        if (horizontal.lengthSqr() > 1.0e-9) {
            return horizontal.normalize();
        }

        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(
                -Math.sin(yaw),
                0.0,
                Math.cos(yaw)
        );
    }

    private static void applyFieldSlow(
            ServerPlayer caster,
            ActiveField field,
            long nowTick
    ) {
        double radius = WEAVE_FIELD_RADIUS_BLOCKS;
        AABB bounds = new AABB(
                field.origin().x - radius,
                field.origin().y - FIELD_VERTICAL_TOLERANCE_BLOCKS,
                field.origin().z - radius,
                field.origin().x + radius,
                field.origin().y + FIELD_VERTICAL_TOLERANCE_BLOCKS,
                field.origin().z + radius
        );

        for (LivingEntity target
                : field.level().getEntitiesOfClass(
                        LivingEntity.class,
                        bounds,
                        target -> target.isAlive()
                                && ExternalActorBindingRuntime
                                        .combatProfile(target)
                                        .isPresent()
                )) {
            double dx = target.getX() - field.origin().x;
            double dz = target.getZ() - field.origin().z;
            if (dx * dx + dz * dz
                    > radius * radius) {
                continue;
            }
            if (ProjectHostileStatusRuntime.applyPhaseFieldSlow(
                    target,
                    field.utilityMagnitudeMultiplier(),
                    nowTick
            ).isPresent()) {
                ProjectUltimateChargeRuntime.recordMageMeaningfulControl(
                        caster,
                        target,
                        nowTick
                );
            }
        }
    }

    private static void spawnFieldRing(
            ServerLevel level,
            Vec3 origin
    ) {
        for (int i = 0; i < FIELD_RING_PARTICLES; i++) {
            double angle = Math.PI * 2.0 * i
                    / FIELD_RING_PARTICLES;
            double x = origin.x
                    + Math.cos(angle)
                    * WEAVE_FIELD_RADIUS_BLOCKS;
            double z = origin.z
                    + Math.sin(angle)
                    * WEAVE_FIELD_RADIUS_BLOCKS;
            level.sendParticles(
                    ParticleTypes.PORTAL,
                    x,
                    origin.y + 0.08,
                    z,
                    1,
                    0.0,
                    0.02,
                    0.0,
                    0.0
            );
        }
    }

    private static boolean isMage(ServerPlayer player) {
        return player != null
                && !player.level().isClientSide()
                && PlayerProgressionService.state(player)
                        .activeClass()
                        .filter(RootClass.MAGE::equals)
                        .isPresent();
    }

    private record SafeDestination(
            Vec3 position,
            double horizontalDistance
    ) {
        private SafeDestination {
            Objects.requireNonNull(position, "position");
            if (!Double.isFinite(horizontalDistance)
                    || horizontalDistance
                            < MIN_ACCEPTED_DISTANCE_BLOCKS
                    || horizontalDistance
                            > MAX_DISTANCE_BLOCKS + 1.0e-9) {
                throw new IllegalArgumentException(
                        "Invalid Phase Step destination."
                );
            }
        }
    }

    private static final class ActiveField {
        private final ServerLevel level;
        private final Vec3 origin;
        private final double utilityMagnitudeMultiplier;
        private final long expiresAtTick;
        private long nextVisualTick;

        private ActiveField(
                ServerLevel level,
                Vec3 origin,
                double utilityMagnitudeMultiplier,
                long expiresAtTick,
                long nextVisualTick
        ) {
            this.level = Objects.requireNonNull(level, "level");
            this.origin = Objects.requireNonNull(origin, "origin");
            if (!Double.isFinite(utilityMagnitudeMultiplier)
                    || utilityMagnitudeMultiplier < 1.0
                    || expiresAtTick < 0L
                    || nextVisualTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Phase Step field."
                );
            }
            this.utilityMagnitudeMultiplier =
                    utilityMagnitudeMultiplier;
            this.expiresAtTick = expiresAtTick;
            this.nextVisualTick = nextVisualTick;
        }

        private ServerLevel level() {
            return level;
        }

        private Vec3 origin() {
            return origin;
        }

        private double utilityMagnitudeMultiplier() {
            return utilityMagnitudeMultiplier;
        }

        private long expiresAtTick() {
            return expiresAtTick;
        }

        private long nextVisualTick() {
            return nextVisualTick;
        }

        private void advanceVisualTick() {
            nextVisualTick = Math.addExact(
                    nextVisualTick,
                    FIELD_VISUAL_INTERVAL_TICKS
            );
        }
    }

    public record Activation(
            boolean accepted,
            Vec3 origin,
            Vec3 destination,
            double horizontalDistance,
            long invulnerableUntilTick,
            boolean weaveFieldCreated,
            long weaveFieldExpiresAtTick
    ) {
        public Activation {
            if (!Double.isFinite(horizontalDistance)
                    || horizontalDistance < 0.0
                    || invulnerableUntilTick < 0L
                    || weaveFieldExpiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Phase Step activation result."
                );
            }
            if (!accepted
                    && (origin != null
                    || destination != null
                    || horizontalDistance != 0.0
                    || invulnerableUntilTick != 0L
                    || weaveFieldCreated
                    || weaveFieldExpiresAtTick != 0L)) {
                throw new IllegalArgumentException(
                        "Rejected Phase Step cannot carry applied state."
                );
            }
            if (accepted
                    && (origin == null
                    || destination == null
                    || horizontalDistance
                            < MIN_ACCEPTED_DISTANCE_BLOCKS)) {
                throw new IllegalArgumentException(
                        "Accepted Phase Step requires a valid displacement."
                );
            }
            if (weaveFieldCreated
                    != (weaveFieldExpiresAtTick > 0L)) {
                throw new IllegalArgumentException(
                        "Phase Step Weave field state is inconsistent."
                );
            }
        }

        public static Activation rejected() {
            return new Activation(
                    false,
                    null,
                    null,
                    0.0,
                    0L,
                    false,
                    0L
            );
        }
    }
}
