package kr.moonseungjun.titanbreak.combat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflexFieldService {
    public static final double P0_RADIUS = 64.0D;

    private static final double MIN_RELATIVE_RATE = ReflexDriveService.P0_WORLD_RELATIVE_RATE;
    private static final double EPSILON = 1.0E-6D;
    private static final Map<UUID, Field> FIELDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> MOB_AI_BUDGET = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> PROJECTILE_SCALE = new ConcurrentHashMap<>();

    private ReflexFieldService() {}

    public static void update(Player player, boolean active, int rating, double radius) {
        if (!active) {
            FIELDS.remove(player.getUUID());
            return;
        }
        FIELDS.put(player.getUUID(), new Field(player.getUUID(), player.level().dimension(),
                player.position(), Math.max(1, rating), Math.max(8.0D, radius)));
    }

    public static void clear(UUID owner) {
        FIELDS.remove(owner);
    }

    public static void clearAll() {
        FIELDS.clear();
        MOB_AI_BUDGET.clear();
        PROJECTILE_SCALE.clear();
    }

    public static boolean active(UUID owner) {
        return FIELDS.containsKey(owner);
    }

    public static int rating(UUID owner) {
        Field field = FIELDS.get(owner);
        return field == null ? 0 : field.rating();
    }

    public static double radius(UUID owner) {
        Field field = FIELDS.get(owner);
        return field == null ? 0.0D : field.radius();
    }

    public static double timeScale(Entity entity) {
        if (entity == null || entity.isRemoved() || entity.level().isClientSide() || FIELDS.isEmpty()) return 1.0D;
        Field strongest = strongestField(entity);
        if (strongest == null) return 1.0D;
        int localRating = localRating(entity, entity.level().dimension());
        return relativeRate(localRating, strongest.rating());
    }

    public static double movementScale(Entity entity) {
        if (entity instanceof Projectile) return 1.0D;
        return timeScale(entity);
    }

    public static boolean shouldAdvanceMobAi(Mob mob) {
        double scale = timeScale(mob);
        UUID id = mob.getUUID();
        if (scale >= 0.999D) {
            MOB_AI_BUDGET.remove(id);
            return true;
        }

        double budget = MOB_AI_BUDGET.getOrDefault(id, initialBudget(id));
        budget += Math.max(0.0D, Math.min(1.0D, scale));
        if (budget + EPSILON >= 1.0D) {
            MOB_AI_BUDGET.put(id, budget - 1.0D);
            return true;
        }
        MOB_AI_BUDGET.put(id, budget);
        return false;
    }

    public static void applyProjectileTimeScale(Projectile projectile) {
        if (projectile == null || projectile.level().isClientSide()) return;
        UUID id = projectile.getUUID();
        if (projectile.isRemoved()) {
            PROJECTILE_SCALE.remove(id);
            return;
        }

        double desired = timeScale(projectile);
        double previous = PROJECTILE_SCALE.getOrDefault(id, 1.0D);
        if (Math.abs(desired - previous) > 1.0E-4D) {
            Vec3 motion = projectile.getDeltaMovement();
            double ratio = desired / Math.max(0.05D, previous);
            projectile.setDeltaMovement(motion.scale(ratio));
        }

        if (desired >= 0.999D) PROJECTILE_SCALE.remove(id);
        else PROJECTILE_SCALE.put(id, desired);
    }

    private static Field strongestField(Entity entity) {
        Field strongest = null;
        double strongestDistance = Double.MAX_VALUE;
        ResourceKey<Level> dimension = entity.level().dimension();
        Vec3 position = entity.position();

        for (Field field : FIELDS.values()) {
            if (!field.dimension().equals(dimension)) continue;
            double distance = position.distanceToSqr(field.center());
            if (distance > field.radius() * field.radius()) continue;
            if (strongest == null || field.rating() > strongest.rating()
                    || (field.rating() == strongest.rating() && distance < strongestDistance)) {
                strongest = field;
                strongestDistance = distance;
            }
        }
        return strongest;
    }

    private static int localRating(Entity entity, ResourceKey<Level> dimension) {
        if (entity instanceof Player player) {
            return ratingForOwner(player.getUUID(), dimension);
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof Player owner) {
            return ratingForOwner(owner.getUUID(), dimension);
        }
        if (entity instanceof TemporalRated temporalRated) {
            return Math.max(0, temporalRated.temporalRating());
        }
        return 0;
    }

    private static int ratingForOwner(UUID owner, ResourceKey<Level> dimension) {
        Field own = FIELDS.get(owner);
        return own != null && own.dimension().equals(dimension) ? own.rating() : 0;
    }

    private static double initialBudget(UUID id) {
        return Math.floorMod(id.hashCode(), 1000) / 1000.0D;
    }

    public static double relativeRate(int localRating, int fieldRating) {
        if (fieldRating <= 0 || localRating >= fieldRating) return 1.0D;
        if (localRating <= 0) return MIN_RELATIVE_RATE;
        double ratio = Math.max(0.0D, Math.min(1.0D, localRating / (double) fieldRating));
        return MIN_RELATIVE_RATE + (1.0D - MIN_RELATIVE_RATE) * Math.pow(ratio, 1.6D);
    }

    private record Field(UUID owner, ResourceKey<Level> dimension, Vec3 center, int rating, double radius) {}
}
