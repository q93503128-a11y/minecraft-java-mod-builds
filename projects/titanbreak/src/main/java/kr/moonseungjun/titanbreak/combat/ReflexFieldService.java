package kr.moonseungjun.titanbreak.combat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflexFieldService {
    public static final double P0_RADIUS = 64.0D;

    private static final double MIN_RELATIVE_RATE = ReflexDriveService.P0_WORLD_RELATIVE_RATE;
    private static final Map<UUID, Field> FIELDS = new ConcurrentHashMap<>();

    private ReflexFieldService() {}

    public static void update(Player player, boolean active, int rating, double radius) {
        if (!active) {
            FIELDS.remove(player.getUUID());
            return;
        }
        FIELDS.put(player.getUUID(), new Field(player.getUUID(), player.level().dimension(),
                player.position(), Math.max(1, rating), Math.max(8.0, radius)));
    }

    public static void clear(UUID owner) {
        FIELDS.remove(owner);
    }

    public static void clearAll() {
        FIELDS.clear();
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
        return field == null ? 0.0 : field.radius();
    }

    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (FIELDS.isEmpty() || entity.isRemoved()) return;

        Field strongest = null;
        double strongestDistance = Double.MAX_VALUE;
        ResourceKey<Level> entityDimension = entity.level().dimension();
        for (Field field : FIELDS.values()) {
            if (!field.dimension().equals(entityDimension)) continue;
            double distance = entity.position().distanceToSqr(field.center());
            if (distance > field.radius() * field.radius()) continue;
            if (strongest == null || field.rating() > strongest.rating()
                    || (field.rating() == strongest.rating() && distance < strongestDistance)) {
                strongest = field;
                strongestDistance = distance;
            }
        }
        if (strongest == null) return;

        int localRating = localRating(entity, entityDimension);
        double relativeRate = relativeRate(localRating, strongest.rating());
        if (relativeRate >= 0.999D) return;

        // Spread allowed ticks across time instead of running a burst at the start of a window.
        // At the current P0 rate of 0.40 this produces an even 2-of-5 cadence per entity.
        long phase = entity.level().getGameTime() + entity.getId() * 31L;
        long previousStep = (long) Math.floor(phase * relativeRate);
        long nextStep = (long) Math.floor((phase + 1L) * relativeRate);
        if (nextStep <= previousStep) event.setCanceled(true);
    }

    private static int localRating(Entity entity, ResourceKey<Level> dimension) {
        if (entity instanceof Player player) {
            return ratingForOwner(player.getUUID(), dimension);
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof Player owner) {
            return ratingForOwner(owner.getUUID(), dimension);
        }
        return 0;
    }

    private static int ratingForOwner(UUID owner, ResourceKey<Level> dimension) {
        Field own = FIELDS.get(owner);
        return own != null && own.dimension().equals(dimension) ? own.rating() : 0;
    }

    public static double relativeRate(int localRating, int fieldRating) {
        if (fieldRating <= 0 || localRating >= fieldRating) return 1.0D;
        if (localRating <= 0) return MIN_RELATIVE_RATE;
        double ratio = Math.max(0.0D, Math.min(1.0D, localRating / (double) fieldRating));
        return MIN_RELATIVE_RATE + (1.0D - MIN_RELATIVE_RATE) * Math.pow(ratio, 1.6D);
    }

    private record Field(UUID owner, ResourceKey<Level> dimension, Vec3 center, int rating, double radius) {}
}
