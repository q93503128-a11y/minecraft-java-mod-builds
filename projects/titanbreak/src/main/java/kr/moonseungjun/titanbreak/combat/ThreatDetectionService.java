package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.entity.PursuerEntity;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Passive warning layer for blind-side, projectile and boss-charge threats. */
public final class ThreatDetectionService {
    private static final Map<UUID, Long> LAST_WARNING = new ConcurrentHashMap<>();

    private record Threat(Vec3 position, int priority) {}

    private ThreatDetectionService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance node = state.firstInstalledInstance("threat_detection");
        if (node == null || !(player.level() instanceof ServerLevel level)) return;

        int enhancement = node.enhancement();
        Threat threat = detect(level, player, enhancement);
        if (threat == null) return;
        if (!AugmentationResourceService.trySpendContinuousPower(player, state, "threat_detection")) return;

        long now = level.getGameTime();
        long last = LAST_WARNING.getOrDefault(player.getUUID(), Long.MIN_VALUE / 4L);
        if (now - last < 8L) return;
        LAST_WARNING.put(player.getUUID(), now);

        Component warning = Component.translatable("hud.titanbreak.threat_warning");
        if (enhancement >= 10) warning = warning.copy().append(Component.literal("  " + directionHint(player, threat.position())));
        if (threat.priority() >= 3) warning = warning.copy().append(Component.literal("  ◆"));
        else if (threat.priority() == 2) warning = warning.copy().append(Component.literal("  ◇"));
        player.sendSystemMessage(warning, true);

        if (player.tickCount % 40 == 0) {
            TitanPlayerData.get(level.getServer()).addMasteryXp(player, "threat_detection", 1);
        }
    }

    private static Threat detect(ServerLevel level, ServerPlayer player, int enhancement) {
        if (enhancement >= 7) {
            PursuerEntity boss = level.getEntitiesOfClass(PursuerEntity.class, player.getBoundingBox().inflate(48.0D),
                            LivingEntity::isAlive).stream()
                    .filter(entity -> approaching(entity.position(), entity.getDeltaMovement(), player.position(), 0.55D)
                            && entity.getDeltaMovement().horizontalDistance() >= 0.75D)
                    .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
            if (boss != null) return new Threat(boss.position(), 3);
        }

        if (enhancement >= 5) {
            Projectile projectile = level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(28.0D),
                            entity -> entity.isAlive() && entity.getOwner() != player).stream()
                    .filter(entity -> approaching(entity.position(), entity.getDeltaMovement(), player.getEyePosition(), 0.72D))
                    .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
            if (projectile != null) return new Threat(projectile.position(), 2);
        }

        AABB area = player.getBoundingBox().inflate(20.0D);
        Vec3 forward = horizontal(player.getLookAngle());
        LivingEntity blind = level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity != player && entity.isAlive() && entity instanceof Enemy).stream()
                .filter(entity -> {
                    Vec3 to = horizontal(entity.position().subtract(player.position()));
                    return to.lengthSqr() > 1.0E-6D && forward.dot(to.normalize()) < 0.30D;
                })
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        return blind == null ? null : new Threat(blind.position(), 1);
    }

    private static boolean approaching(Vec3 origin, Vec3 velocity, Vec3 target, double threshold) {
        if (velocity.lengthSqr() <= 1.0E-6D) return false;
        Vec3 toTarget = target.subtract(origin);
        return toTarget.lengthSqr() > 1.0E-6D && velocity.normalize().dot(toTarget.normalize()) >= threshold;
    }

    private static String directionHint(ServerPlayer player, Vec3 threat) {
        Vec3 forward = horizontal(player.getLookAngle());
        Vec3 to = horizontal(threat.subtract(player.position()));
        if (to.lengthSqr() <= 1.0E-6D || forward.lengthSqr() <= 1.0E-6D) return "•";
        to = to.normalize();
        double dot = forward.dot(to);
        double cross = forward.x * to.z - forward.z * to.x;
        if (dot < -0.45D) return "▼";
        if (cross > 0.25D) return "▶";
        if (cross < -0.25D) return "◀";
        return "▲";
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 result = new Vec3(value.x, 0.0D, value.z);
        return result.lengthSqr() <= 1.0E-6D ? Vec3.ZERO : result.normalize();
    }

    public static void clear(UUID playerId) {
        LAST_WARNING.remove(playerId);
    }

    public static void clearAll() {
        LAST_WARNING.clear();
    }
}
