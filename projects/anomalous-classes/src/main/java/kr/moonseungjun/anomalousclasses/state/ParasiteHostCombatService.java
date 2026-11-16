package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * The host, not the hidden passenger, determines the melee line and physical
 * reach. Player.attack still runs vanilla weapon effects, combat cooldown,
 * advancements and loot attribution after the authoritative host raycast.
 */
public final class ParasiteHostCombatService {
    private static final Set<UUID> AUTHORIZED_ATTACKS = new HashSet<>();
    private static final Map<UUID, Long> LAST_INTENT = new HashMap<>();
    private static MinecraftServer activeServer;
    private ParasiteHostCombatService() {}

    /** Reject forged/duplicate vanilla player attack packets while controlling a host. */
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner
                && ParasiteService.activeHost(owner) != null
                && !AUTHORIZED_ATTACKS.contains(owner.getUUID()))
            event.setCanceled(true);
    }

    public static void input(ServerPlayer owner) {
        if (owner == null || owner.getServer() == null || !owner.isAlive()
                || owner.isSpectator()) return;
        Mob host = ParasiteService.activeHost(owner);
        if (host == null || !host.isAlive()) return;
        MinecraftServer server = owner.getServer();
        if (server != activeServer) {
            LAST_INTENT.clear();
            AUTHORIZED_ATTACKS.clear();
            activeServer = server;
        }
        long now = owner.serverLevel().getGameTime();
        long previous = LAST_INTENT.getOrDefault(owner.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous < 4) return; // Resist replay/spam; do not double-hit.
        if (owner.getAttackStrengthScale(0.5F) < .80F) return;
        LAST_INTENT.put(owner.getUUID(), now);

        LivingEntity target = aimedEnemy(owner, host);
        if (target == null || !ParasiteTraitService.canHit(owner, target)) return;

        // The normal vanilla damage pipeline still determines armor,
        // enchantments, weapon durability and hit attribution.
        AUTHORIZED_ATTACKS.add(owner.getUUID());
        try {
            owner.attack(target);
        } finally {
            AUTHORIZED_ATTACKS.remove(owner.getUUID());
        }
    }

    public static void logout(UUID player) {
        LAST_INTENT.remove(player);
        AUTHORIZED_ATTACKS.remove(player);
    }

    /**
     * The natural reach of a large original body may extend beyond a human
     * sword swing, but it never becomes a server-side 10-block attack.
     * Any vanilla/modded reach bonuses are considered before the bounded
     * width adjustment; the temporary mount buffer is NOT an attack bonus.
     */
    public static double bodyRange(double realPlayerRange, double hostWidth) {
        double base = ParasiteHostReachService.entityLimit(realPlayerRange - 2.75);
        double size = Math.max(0.0, Math.min(1.5, (hostWidth - 1.0) * 0.6));
        return Math.min(5.5, base + size);
    }

    /**
     * Large mobs can naturally strike a small enemy in front of their chest
     * even if their high eye-to-eye ray misses it. This is a narrow forward
     * body swing, never a rear hit or an attack through blocks.
     */
    public static boolean bodySwingCone(Vec3 look, Vec3 delta, double hostHeight) {
        if (hostHeight < 1.8 || delta.lengthSqr() < 0.01
                || Math.abs(look.y) > .65) return false;
        Vec3 flatLook = new Vec3(look.x, 0, look.z);
        Vec3 flatDelta = new Vec3(delta.x, 0, delta.z);
        return flatLook.lengthSqr() > .01 && flatDelta.lengthSqr() > .01
                && flatLook.normalize().dot(flatDelta.normalize()) >= .93;
    }

    /** Server-only raycast. The packet never supplies target, range or damage. */
    private static LivingEntity aimedEnemy(ServerPlayer owner, Mob host) {
        double range = bodyRange(owner.entityInteractionRange(), host.getBbWidth());
        Vec3 eye = host.getEyePosition();
        Vec3 look = ParasiteService.viewDirection(owner).normalize();
        Vec3 far = eye.add(look.scale(range));
        AABB search = host.getBoundingBox().inflate(range);
        LivingEntity closest = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (LivingEntity candidate : owner.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                e -> e != host && e != owner && e.isAlive() && e.isPickable() && e.isAttackable())) {
            var rayHit = candidate.getBoundingBox().inflate(.15).clip(eye, far);
            boolean eyeHit = rayHit.isPresent();
            Vec3 sightOrigin = eye;
            Vec3 hitPoint = eyeHit ? rayHit.get() : null;
            if (!eyeHit) {
                Vec3 chest = host.getBoundingBox().getCenter();
                Vec3 center = candidate.getBoundingBox().getCenter();
                if (!bodySwingCone(look, center.subtract(chest), host.getBbHeight())
                        || host.getBoundingBox().distanceToSqr(center) > range * range) continue;
                sightOrigin = chest;
                hitPoint = center;
            }
            double dist = sightOrigin.distanceToSqr(hitPoint);
            if (dist >= nearest || dist > (range + .2) * (range + .2)) continue;
            BlockHitResult wall = owner.serverLevel().clip(new ClipContext(
                    sightOrigin, hitPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host));
            if (wall.getType() != HitResult.Type.MISS
                    && sightOrigin.distanceToSqr(wall.getLocation()) + .001 < dist) continue;
            nearest = dist;
            closest = candidate;
        }
        return closest;
    }

}
