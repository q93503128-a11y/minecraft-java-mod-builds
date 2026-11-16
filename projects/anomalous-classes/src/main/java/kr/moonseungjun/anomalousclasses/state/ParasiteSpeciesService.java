package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative adapters for the first native/inherited aquatic and ender traits. */
public final class ParasiteSpeciesService {
    private static final Map<UUID, Map<UUID, Long>> PROJECTILES = new HashMap<>();
    private static final Map<UUID, Long> LAST_DODGE = new HashMap<>();
    private static MinecraftServer serverIdentity;
    private ParasiteSpeciesService() {}

    /** Passive water adaptation is independent of V's temporary numeric hotbar mode. */
    public static void tick(ServerPlayer owner, Mob host, ParasiteSavedData.State state) {
        // Water affinity is a common environmental host trait; explicit
        // nonvanilla aquatic profiles inherit it at species mastery 100 too.
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
        if (!ParasiteRules.hasAquaticAdaptation(species, state)) return;
        if (host.isInWaterOrBubble()) {
            // The original host and its passenger each have real air counters.
            // Do not grant breathing to the unhosted human.
            host.setAirSupply(host.getMaxAirSupply());
            owner.setAirSupply(owner.getMaxAirSupply());
        }
    }

    /** Actual host-eye beam: server target, solid block occlusion and friendly-fire rules. */
    public static boolean guardianBeam(ServerPlayer owner, Mob host) {
        ServerLevel level = owner.serverLevel();
        Vec3 eye = host.getEyePosition();
        Vec3 look = ParasiteService.viewDirection(owner).normalize();
        double range = 14.0;
        Vec3 end = eye.add(look.scale(range));
        AABB search = host.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        LivingEntity closest = null;
        double nearest = range * range;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != host && e != owner && e.isAlive() && e.isPickable() && e.isAttackable())) {
            var clip = target.getBoundingBox().inflate(0.15).clip(eye, end);
            if (clip.isEmpty()) continue;
            double d = clip.get().distanceToSqr(eye);
            if (d < nearest) {
                nearest = d;
                closest = target;
            }
        }
        if (closest == null || !ParasiteTraitService.canHit(owner, closest)) return false;
        BlockHitResult block = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host));
        if (block.getType() != HitResult.Type.MISS
                && block.getLocation().distanceToSqr(eye) < nearest) return false;

        // Standard player ownership preserves drops, damage multipliers and BD accounting.
        float damage = 7.0F;
        if (!closest.hurt(owner.damageSources().playerAttack(owner), damage)) return false;
        Vec3 impact = closest.getBoundingBox().getCenter();
        for (int i = 1; i <= 14; i++) {
            Vec3 point = eye.lerp(impact, i / 14.0);
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                    1, 0.005, 0.005, 0.005, 0);
        }
        level.playSound(null, host.blockPosition(), SoundEvents.GUARDIAN_ATTACK,
                SoundSource.HOSTILE, 0.9F, 1.0F);
        return true;
    }

    /** Manual ender movement is a relocation of the same original Mob, never a clone. */
    public static boolean blink(ServerPlayer owner, Mob host) {
        Vec3 origin = host.position();
        Vec3 direction = ParasiteService.viewDirection(owner);
        // Search from furthest aim-point backwards; avoid unloaded areas and protected space.
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() < 0.0001) horizontal = host.getLookAngle();
        if (horizontal.lengthSqr() < 0.0001) return false;
        horizontal = horizontal.normalize();
        for (int step = 8; step >= 3; step--) {
            Vec3 ahead = origin.add(horizontal.scale(step));
            int levelOffset = (int) Math.round(direction.y * step);
            for (int dy = 2; dy >= -3; dy--) {
                Vec3 destination = new Vec3(ahead.x, Math.floor(origin.y) + levelOffset + dy, ahead.z);
                if (safe(owner, host, destination)
                        && teleport(owner, host, destination)) return true;
            }
        }
        return false;
    }

    /** 35% server-side dodge for one physical projectile; no loops or immunity. */
    public static boolean dodge(ServerPlayer owner, Mob host, DamageSource source) {
        if (!(source.getDirectEntity() instanceof Projectile projectile)) return false;
        var state = ParasiteSavedData.get(owner.getServer()).player(owner.getUUID());
        if (!ParasiteRules.hasEnderDodge(host.getType() == EntityType.ENDERMAN,
                state.mastery("minecraft:enderman"))) return false;

        MinecraftServer server = owner.getServer();
        if (server != serverIdentity) {
            serverIdentity = server;
            PROJECTILES.clear();
            LAST_DODGE.clear();
        }
        long now = owner.serverLevel().getGameTime();
        Map<UUID, Long> processed = PROJECTILES.computeIfAbsent(owner.getUUID(), ignored -> new HashMap<>());
        if (processed.size() > 80) processed.entrySet().removeIf(e -> now - e.getValue() > 100);
        if (processed.containsKey(projectile.getUUID()) || now - LAST_DODGE.getOrDefault(
                owner.getUUID(), Long.MIN_VALUE / 2) < 12L) return false;
        if (processed.size() >= 96) return false;
        processed.put(projectile.getUUID(), now);
        if (owner.getRandom().nextFloat() >= 0.35F) return false;

        Vec3 away = host.position().subtract(projectile.position());
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 0.01) away = ParasiteService.viewDirection(owner).scale(-1.0);
        away = new Vec3(away.x, 0, away.z).normalize();
        if (away.lengthSqr() < 0.01) return false;
        Vec3 side = new Vec3(-away.z, 0, away.x);
        double[][] offsets = {{5,0},{4,3},{4,-3},{-2,4},{-2,-4}};
        for (double[] v : offsets) {
            Vec3 intended = host.position().add(away.scale(v[0])).add(side.scale(v[1]));
            for (int dy = 1; dy >= -2; dy--) {
                Vec3 dest = new Vec3(intended.x, Math.floor(host.getY()) + dy, intended.z);
                if (safe(owner, host, dest) && teleport(owner, host, dest)) {
                    LAST_DODGE.put(owner.getUUID(), now);
                    return true;
                }
            }
        }
        return false;
    }

    public static void logout(UUID owner) {
        PROJECTILES.remove(owner);
        LAST_DODGE.remove(owner);
    }

    /** Only loaded, standable, non-fluid, world-border-safe spaces are candidates. */
    private static boolean safe(ServerPlayer owner, Mob host, Vec3 pos) {
        ServerLevel level = owner.serverLevel();
        if (owner.getVehicle() != host || host.getPassengers().size() != 1
                || host.getPassengers().get(0) != owner) return false;
        BlockPos feet = BlockPos.containing(pos);
        BlockPos floor = feet.below();
        BlockPos top = feet.above((int)Math.ceil(host.getBbHeight()));
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(floor) || !level.hasChunkAt(top)
                || !level.getWorldBorder().isWithinBounds(feet)) return false;
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                || !level.getFluidState(feet).isEmpty() || !level.getFluidState(top).isEmpty()) return false;
        Vec3 shift = pos.subtract(host.position());
        if (!level.noCollision(host, host.getBoundingBox().move(shift))) return false;
        // Validate the real mounted PLAYER body at its projected vehicle seat,
        // rather than moving a safe Mob into an unsafe player-size ceiling.
        Vec3 passengerAt = passengerDestination(host.position(),
                host.getPassengerRidingPosition(owner), pos);
        BlockPos riderFeet = BlockPos.containing(passengerAt);
        BlockPos riderTop = BlockPos.containing(passengerAt.add(0, owner.getBbHeight(), 0));
        if (!level.hasChunkAt(riderFeet) || !level.hasChunkAt(riderTop)
                || !level.getWorldBorder().isWithinBounds(riderFeet)
                || !level.getWorldBorder().isWithinBounds(riderTop)) return false;
        return level.noCollision(owner,
                owner.getBoundingBox().move(passengerAt.subtract(owner.position())));
    }

    /** Immutable geometry; the actual teleport still happens only on the server. */
    public static Vec3 passengerDestination(Vec3 hostOrigin, Vec3 seatOrigin, Vec3 hostDestination) {
        return seatOrigin.add(hostDestination.subtract(hostOrigin));
    }

    private static boolean teleport(ServerPlayer owner, Mob host, Vec3 pos) {
        ServerLevel level = owner.serverLevel();
        if (!safe(owner, host, pos)) return false;
        Vec3 before = host.position();
        host.teleportTo(pos.x, pos.y, pos.z);
        if (host.position().distanceToSqr(pos) > .02) return false;
        if (owner.getVehicle() != host && !owner.startRiding(host, true)) {
            // An external Mob may disallow remounting after relocation. No
            // completed skill, no cooldown charge, and the original body goes back.
            host.teleportTo(before.x, before.y, before.z);
            return false;
        }
        host.positionRider(owner);
        owner.connection.resetPosition();
        ParasiteService.syncView(owner);
        level.sendParticles(ParticleTypes.PORTAL, before.x, before.y + host.getBbHeight() / 2,
                before.z, 32, 0.3, 0.6, 0.3, 0.15);
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + host.getBbHeight() / 2,
                pos.z, 32, 0.3, 0.6, 0.3, 0.15);
        level.playSound(null, host.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 0.9F, 1.0F);
        return true;
    }
}
