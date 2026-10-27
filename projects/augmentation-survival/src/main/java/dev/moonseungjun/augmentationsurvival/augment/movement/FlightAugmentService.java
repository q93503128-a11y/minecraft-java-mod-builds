package dev.moonseungjun.augmentationsurvival.augment.movement;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative powered superhero flight. Vanilla Creative movement is never enabled. */
public final class FlightAugmentService {
    public static final String FLIGHT_ID = id("flight");
    private static final ResourceKey<DamageType> FLIGHT_COLLISION_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "flight_collision")
    );
    private static final double HOVER_SPEED = 0.42D;
    private static final double CRUISE_SPEED = 1.30D;
    private static final double BOOST_SPEED = 2.30D;
    private static final double CRUISE_ACCEL = 0.060D;
    private static final double BOOST_ACCEL = 0.075D;
    private static final double BRAKE_DECEL = 0.11D;
    private static final int TAKEOFF_TICKS = 10;
    private static final float COLLISION_DAMAGE = 10.0F;
    private static final int ENTITY_IMPACT_COOLDOWN_TICKS = 10;
    private static final int MAX_BROKEN_BLOCKS_PER_TICK = 12;
    private static final double MIN_IMPACT_SPEED = 0.35D;
    private static final Map<UUID, FlightState> ACTIVE = new HashMap<>();
    private static boolean initialized;

    private FlightAugmentService() { }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        AugmentCatalog.require(FLIGHT_ID);
        ServerTickEvents.END_SERVER_TICK.register(FlightAugmentService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> deactivateImmediately(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
    }

    public static boolean toggle(ServerPlayer player) {
        FlightState current = ACTIVE.get(player.getUUID());
        if (current != null) {
            current.shuttingDown = true;
            current.phase = Phase.BRAKE_LANDING;
            return true;
        }
        if (!owns(player) || player.isDeadOrDying() || player.isSpectator() || PlayerRescueAugmentService.isDowned(player)) return false;
        var abilities = player.getAbilities();
        FlightState state = new FlightState(abilities.mayfly, abilities.flying, player.isNoGravity());
        state.phase = Phase.TAKEOFF;
        state.takeoffTicks = TAKEOFF_TICKS;
        state.direction = player.getLookAngle().normalize();
        state.speed = 0.32D;
        ACTIVE.put(player.getUUID(), state);
        abilities.mayfly = true;
        abilities.flying = false;
        player.onUpdateAbilities();
        player.setNoGravity(true);
        player.resetFallDistance();
        Vec3 launch = state.direction.scale(0.22D).add(0.0D, 0.42D, 0.0D);
        player.setDeltaMovement(launch);
        player.hurtMarked = true;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 at = player.position().add(0.0D, 0.35D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 18, 0.50D, 0.18D, 0.50D, 0.08D);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 8, 0.32D, 0.25D, 0.32D, 0.03D);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.55F, 0.62F);
        return true;
    }

    public static boolean isActive(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
    public static Phase phase(ServerPlayer player) { FlightState state = ACTIVE.get(player.getUUID()); return state == null ? Phase.OFF : state.phase; }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, FlightState>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FlightState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            FlightState state = entry.getValue();
            if (player == null) { iterator.remove(); continue; }
            if (!owns(player) || player.isDeadOrDying() || player.isSpectator() || PlayerRescueAugmentService.isDowned(player)) {
                restore(player, state); iterator.remove(); continue;
            }
            if (applyFlight(player, state)) { restore(player, state); iterator.remove(); }
        }
    }

    /** @return true when the braking/landing shutdown is complete. */
    private static boolean applyFlight(ServerPlayer player, FlightState state) {
        var abilities = player.getAbilities();
        if (!abilities.mayfly) abilities.mayfly = true;
        if (abilities.flying) { abilities.flying = false; player.onUpdateAbilities(); }
        player.setNoGravity(true);
        player.resetFallDistance();
        Input input = player.getLastClientInput();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() > 1.0E-6D) right = right.normalize();
        Phase previous = state.phase;
        Vec3 next;

        if (state.takeoffTicks > 0 && !state.shuttingDown) {
            state.takeoffTicks--;
            state.phase = Phase.TAKEOFF;
            state.direction = steer(state.direction, look, 0.18D);
            state.speed = Math.min(0.62D, state.speed + 0.035D);
            next = state.direction.scale(state.speed).add(0.0D, 0.18D, 0.0D);
        } else if (state.shuttingDown || input.shift()) {
            state.phase = Phase.BRAKE_LANDING;
            Vec3 current = player.getDeltaMovement();
            double magnitude = current.length();
            double nextMagnitude = Math.max(0.0D, magnitude - BRAKE_DECEL);
            Vec3 basis = magnitude > 1.0E-5D ? current.scale(1.0D / magnitude) : Vec3.ZERO;
            next = basis.scale(nextMagnitude).add(0.0D, state.shuttingDown ? -0.055D : -0.025D, 0.0D);
            state.speed = nextMagnitude;
            if (state.shuttingDown && (player.onGround() || (nextMagnitude < 0.10D && player.getY() <= player.blockPosition().getY() + 0.15D))) {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                landingFeedback(player);
                return true;
            }
        } else if (input.forward() && !input.backward()) {
            boolean boost = input.sprint();
            state.phase = boost ? Phase.BOOST : Phase.CRUISE;
            double cap = boost ? BOOST_SPEED : CRUISE_SPEED;
            double accel = boost ? BOOST_ACCEL : CRUISE_ACCEL;
            state.speed = Math.min(cap, Math.max(HOVER_SPEED, state.speed) + accel);
            double steer = boost ? 0.075D : 0.13D;
            state.direction = steer(state.direction, look, steer);
            next = state.direction.scale(state.speed);
        } else {
            state.phase = Phase.HOVER;
            state.speed = Math.max(0.0D, state.speed - 0.09D);
            double forward = axis(input.forward(), input.backward());
            double strafe = axis(input.left(), input.right());
            double vertical = axis(input.jump(), false);
            Vec3 intent = look.scale(forward).add(right.scale(strafe)).add(0.0D, vertical, 0.0D);
            if (intent.lengthSqr() > 1.0D) intent = intent.normalize();
            Vec3 target = intent.scale(HOVER_SPEED);
            next = player.getDeltaMovement().scale(0.72D).add(target.scale(0.28D));
            if (intent.lengthSqr() < 1.0E-6D && next.lengthSqr() < 0.001D) next = Vec3.ZERO;
            if (next.lengthSqr() > 1.0E-6D) state.direction = next.normalize();
        }

        resolveImpacts(player, next);
        player.setDeltaMovement(next);
        player.hurtMarked = true;
        emitFeedback(player, state, previous);
        return false;
    }

    /** Resolves the full player body sweep before vanilla movement collision clips the player. */
    private static void resolveImpacts(ServerPlayer player, Vec3 movement) {
        FlightState state = ACTIVE.get(player.getUUID());
        if (state == null) return;
        double impactSpeed = movement.length();
        if (impactSpeed < MIN_IMPACT_SPEED) return;
        ServerLevel level = (ServerLevel) player.level();
        AABB swept = player.getBoundingBox().expandTowards(movement).inflate(0.24D, 0.18D, 0.24D);
        breakCollisionBlocks(level, player, swept, impactSpeed);
        damageCollisionEnemies(level, player, state, swept, movement);
    }

    private static void breakCollisionBlocks(ServerLevel level, ServerPlayer player, AABB swept, double impactSpeed) {
        double maxHardness = breakableHardness(impactSpeed);
        if (maxHardness <= 0.0D) return;
        int minX = Mth.floor(swept.minX), minY = Mth.floor(swept.minY), minZ = Mth.floor(swept.minZ);
        int maxX = Mth.floor(swept.maxX), maxY = Mth.floor(swept.maxY), maxZ = Mth.floor(swept.maxZ);
        int broken = 0;
        for (int y = minY; y <= maxY && broken < MAX_BROKEN_BLOCKS_PER_TICK; y++) {
            for (int x = minX; x <= maxX && broken < MAX_BROKEN_BLOCKS_PER_TICK; x++) {
                for (int z = minZ; z <= maxZ && broken < MAX_BROKEN_BLOCKS_PER_TICK; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState block = level.getBlockState(pos);
                    if (block.isAir() || level.getBlockEntity(pos) != null) continue;
                    var collision = block.getCollisionShape(level, pos);
                    if (collision.isEmpty() || !collision.bounds().move(pos).intersects(swept)) continue;
                    float hardness = block.getDestroySpeed(level, pos);
                    if (hardness < 0.0F || hardness > maxHardness) continue;
                    if (level.destroyBlock(pos, false, player)) broken++;
                }
            }
        }
    }

    private static double breakableHardness(double impactSpeed) {
        return Math.max(0.0D, impactSpeed * 3.0D - 0.70D);
    }

    private static void damageCollisionEnemies(ServerLevel level, ServerPlayer player, FlightState state, AABB swept, Vec3 movement) {
        long now = level.getGameTime();
        if (now % 100L == 0L) state.entityImpactReadyTick.entrySet().removeIf(entry -> entry.getValue() <= now);
        Vec3 push = movement.lengthSqr() > 1.0E-6D ? movement.normalize() : Vec3.ZERO;
        var source = player.damageSources().source(FLIGHT_COLLISION_DAMAGE, null, player);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                swept,
                target -> CompanionAugmentService.isHostileTarget(player, target)
        )) {
            long ready = state.entityImpactReadyTick.getOrDefault(target.getUUID(), 0L);
            if (ready > now) continue;
            boolean damaged = CombatProvenanceContext.callAs(
                    CombatProvenance.MOVEMENT_IMPACT,
                    () -> target.hurtServer(level, source, COLLISION_DAMAGE)
            );
            if (!damaged) continue;
            state.entityImpactReadyTick.put(target.getUUID(), now + ENTITY_IMPACT_COOLDOWN_TICKS);
            target.push(push.x * 0.75D, Math.max(0.12D, push.y * 0.30D), push.z * 0.75D);
            Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            level.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 12, 0.28D, 0.28D, 0.28D, 0.12D);
        }
    }

    private static Vec3 steer(Vec3 current, Vec3 desired, double amount) {
        Vec3 blended = current.scale(1.0D - amount).add(desired.scale(amount));
        return blended.lengthSqr() < 1.0E-8D ? desired : blended.normalize();
    }

    private static void emitFeedback(ServerPlayer player, FlightState state, Phase previous) {
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        Vec3 trail = player.position().add(0.0D, 0.85D, 0.0D).subtract(state.direction.scale(0.9D));
        if (state.phase == Phase.BOOST && previous != Phase.BOOST) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, trail.x, trail.y, trail.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, trail.x, trail.y, trail.z, 22, 0.50D, 0.50D, 0.50D, 0.12D);
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.70F, 0.45F);
        }
        int cadence = state.phase == Phase.BOOST ? 2 : state.phase == Phase.CRUISE ? 4 : state.phase == Phase.TAKEOFF ? 3 : 10;
        if (now % cadence != 0L) return;
        if (state.phase == Phase.CRUISE || state.phase == Phase.BOOST) {
            int count = state.phase == Phase.BOOST ? 9 : 4;
            double spread = state.phase == Phase.BOOST ? 0.27D : 0.16D;
            level.sendParticles(ParticleTypes.CLOUD, trail.x, trail.y, trail.z, count, spread, spread * 0.7D, spread, 0.04D);
        } else if (state.phase == Phase.HOVER) {
            level.sendParticles(ParticleTypes.CLOUD, trail.x, trail.y, trail.z, 2, 0.12D, 0.08D, 0.12D, 0.01D);
        }
    }

    private static void landingFeedback(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.15D, player.getZ(), 10, 0.45D, 0.08D, 0.45D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.38F, 0.82F);
    }

    private static boolean owns(ServerPlayer player) {
        return AugmentationPlayerStateService.state(player).level(FLIGHT_ID) > 0;
    }

    private static double axis(boolean positive, boolean negative) { return positive == negative ? 0.0D : positive ? 1.0D : -1.0D; }
    private static String id(String path) { return AugmentationSurvival.MOD_ID + ":" + path; }

    private static void deactivateImmediately(ServerPlayer player) {
        FlightState state = ACTIVE.remove(player.getUUID());
        if (state != null) restore(player, state);
    }

    private static void restore(ServerPlayer player, FlightState state) {
        var abilities = player.getAbilities();
        abilities.mayfly = state.hadMayfly;
        abilities.flying = state.wasFlying;
        player.onUpdateAbilities();
        player.setNoGravity(state.hadNoGravity);
        player.resetFallDistance();
        player.hurtMarked = true;
    }

    public enum Phase { OFF, TAKEOFF, HOVER, CRUISE, BOOST, BRAKE_LANDING }

    private static final class FlightState {
        private final boolean hadMayfly;
        private final boolean wasFlying;
        private final boolean hadNoGravity;
        private final Map<UUID, Long> entityImpactReadyTick = new HashMap<>();
        private Phase phase = Phase.OFF;
        private int takeoffTicks;
        private Vec3 direction = Vec3.ZERO;
        private double speed;
        private boolean shuttingDown;

        private FlightState(boolean hadMayfly, boolean wasFlying, boolean hadNoGravity) {
            this.hadMayfly = hadMayfly;
            this.wasFlying = wasFlying;
            this.hadNoGravity = hadNoGravity;
        }
    }
}
