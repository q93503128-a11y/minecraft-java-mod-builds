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

    /** Server-only raycast. The packet never supplies its own target, range or damage. */
    private static LivingEntity aimedEnemy(ServerPlayer owner, Mob host) {
        double range = Math.min(4.0, Math.max(2.5, owner.entityInteractionRange()));
        Vec3 eye = host.getEyePosition();
        Vec3 look = ParasiteService.viewDirection(owner);
        Vec3 far = eye.add(look.scale(range));
        AABB search = host.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        LivingEntity closest = null;
        double nearest = range * range;
        for (LivingEntity candidate : owner.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                e -> e != host && e != owner && e.isAlive() && e.isPickable() && e.isAttackable())) {
            var hit = candidate.getBoundingBox().inflate(.15).clip(eye, far);
            if (hit.isEmpty()) continue;
            double distance = eye.distanceToSqr(hit.get());
            if (distance < nearest) {
                nearest = distance;
                closest = candidate;
            }
        }
        if (closest == null) return null;
        BlockHitResult wall = owner.serverLevel().clip(new ClipContext(eye, far,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host));
        if (wall.getType() != HitResult.Type.MISS
                && eye.distanceToSqr(wall.getLocation()) < nearest) return null;
        return closest;
    }
}
