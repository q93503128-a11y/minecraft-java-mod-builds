package kr.moonseungjun.titanbreak.combat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflexFieldService {
    private static final int WINDOW_TICKS = 20;
    private static final double MIN_RELATIVE_RATE = 0.08;
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
        for (Field field : FIELDS.values()) {
            if (field.dimension() != entity.level().dimension()) continue;
            double distance = entity.position().distanceToSqr(field.center());
            if (distance > field.radius() * field.radius()) continue;
            if (strongest == null || field.rating() > strongest.rating()
                    || (field.rating() == strongest.rating() && distance < strongestDistance)) {
                strongest = field;
                strongestDistance = distance;
            }
        }
        if (strongest == null) return;

        int localRating = 0;
        if (entity instanceof Player player) {
            Field own = FIELDS.get(player.getUUID());
            if (own != null && own.dimension() == entity.level().dimension()) localRating = own.rating();
        }

        double relativeRate = relativeRate(localRating, strongest.rating());
        if (relativeRate >= 0.999) return;

        int allowedTicks = Math.max(1, Math.min(WINDOW_TICKS,
                (int) Math.round(relativeRate * WINDOW_TICKS)));
        long phase = entity.level().getGameTime() + entity.getId() * 7L;
        int slot = Math.floorMod((int) (phase % WINDOW_TICKS), WINDOW_TICKS);
        if (slot >= allowedTicks) event.setCanceled(true);
    }

    public static double relativeRate(int localRating, int fieldRating) {
        if (fieldRating <= 0 || localRating >= fieldRating) return 1.0;
        if (localRating <= 0) return MIN_RELATIVE_RATE;
        double ratio = Math.max(0.0, Math.min(1.0, localRating / (double) fieldRating));
        return MIN_RELATIVE_RATE + (1.0 - MIN_RELATIVE_RATE) * Math.pow(ratio, 1.35);
    }

    private record Field(UUID owner, ResourceKey<Level> dimension, Vec3 center, int rating, double radius) {}
}
