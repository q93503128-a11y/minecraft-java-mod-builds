package kr.moonseungjun.anomalousclasses.state;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Small, conservative player-controlled adaptations of two audited external
 * species. Uses only their REAL possessed Mob and vanilla server-authoritative
 * damage/physics; does not attempt to call unverified mod animation internals.
 *
 * Grottol's flight and Bluff's raised-earth combat are source references,
 * NOT claims that the native mod AI or animation is reproduced here.
 */
public final class ParasiteExternalTraitService {
    private ParasiteExternalTraitService() {}

    public static boolean canScuttle(boolean onGround, boolean inWater,
                                     boolean collisionAhead, Vec3 look) {
        if (!onGround || inWater || collisionAhead || look == null) return false;
        double flat = look.x * look.x + look.z * look.z;
        return Double.isFinite(flat) && flat > 0.01;
    }

    /** V+5: Grottol-style physical escape burst; never teleports through blocks. */
    public static boolean scuttle(ServerPlayer owner, Mob host) {
        Vec3 look = ParasiteService.viewDirection(owner);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < .01) return false;
        Vec3 aim = flat.normalize();
        boolean blocked = !owner.serverLevel().noCollision(host,
                host.getBoundingBox().move(aim.scale(.65)));
        if (!canScuttle(host.onGround(), host.isInWaterOrBubble(), blocked, look)) return false;

        host.push(aim.x * .85, .10, aim.z * .85);
        host.hurtMarked = true;
        owner.serverLevel().sendParticles(ParticleTypes.CRIT,
                host.getX(), host.getY() + host.getBbHeight() / 2, host.getZ(),
                18, .25, .22, .25, .10);
        owner.serverLevel().playSound(null, host.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.HOSTILE, .75F, 1.4F);
        return true;
    }

    /**
     * V+6: a bounded ground-rise hit based on Bluff's earth combat theme.
     * Does not create fake minions, alter or grief terrain, or bypass team rules.
     */
    public static boolean earthRise(ServerPlayer owner, Mob host) {
        ServerLevel level = owner.serverLevel();
        Vec3 eye = host.getEyePosition();
        Vec3 look = ParasiteService.viewDirection(owner).normalize();
        if (look.lengthSqr() < .01) return false;
        final double reach = 9.0;
        Vec3 far = eye.add(look.scale(reach));
        AABB search = host.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.2);
        LivingEntity target = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (LivingEntity possible : level.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != host && e != owner && e.isAlive() && e.isPickable() && e.isAttackable())) {
            if (!possible.onGround() || !ParasiteTraitService.canHit(owner, possible)) continue;
            var hit = possible.getBoundingBox().inflate(.2).clip(eye, far);
            if (hit.isEmpty()) continue;
            double distance = eye.distanceToSqr(hit.get());
            if (distance >= nearest || distance > reach * reach) continue;
            BlockPos floor = possible.blockPosition().below();
            if (!level.hasChunkAt(floor) || level.getBlockState(floor).isAir()) continue;
            BlockHitResult wall = level.clip(new ClipContext(eye, hit.get(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host));
            if (wall.getType() != HitResult.Type.MISS
                    && eye.distanceToSqr(wall.getLocation()) + .001 < distance) continue;
            nearest = distance;
            target = possible;
        }
        if (target == null || !target.hurt(owner.damageSources().playerAttack(owner), 5.0F))
            return false;

        Vec3 impact = target.getBoundingBox().getCenter();
        BlockPos floor = target.blockPosition().below();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                        level.getBlockState(floor)),
                impact.x, target.getY() + .08, impact.z, 28, .55, .08, .55, .15);
        target.push(0, .38, 0);
        level.playSound(null, floor, SoundEvents.STONE_BREAK,
                SoundSource.HOSTILE, .9F, .82F);
        return true;
    }
}
