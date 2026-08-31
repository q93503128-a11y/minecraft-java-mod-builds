package kr.moonseungjun.titanbreak.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived semantic tags for augmentation damage. They let defensive implants react to
 * armor-breaking or shockwave attacks without abusing vanilla damage-source message ids.
 */
public final class DamageChannelService {
    public enum Channel { ARMOR_BREAK, SHOCKWAVE, THERMAL, WEAKPOINT }

    private record Marker(UUID sourceId, long tick, EnumSet<Channel> channels) {}

    private static final Map<UUID, Marker> MARKERS = new ConcurrentHashMap<>();

    private DamageChannelService() {}

    public static void mark(Entity source, Entity target, Channel channel) {
        if (!(target instanceof LivingEntity living) || source == null || channel == null) return;
        long now = living.level().getGameTime();
        UUID targetId = living.getUUID();
        UUID sourceId = source.getUUID();
        MARKERS.compute(targetId, (ignored, existing) -> {
            EnumSet<Channel> channels = EnumSet.noneOf(Channel.class);
            if (existing != null && existing.tick() == now && existing.sourceId().equals(sourceId)) {
                channels.addAll(existing.channels());
            }
            channels.add(channel);
            return new Marker(sourceId, now, channels);
        });
    }

    public static Set<Channel> consume(LivingEntity target, Entity source) {
        if (target == null || source == null) return Set.of();
        Marker marker = MARKERS.get(target.getUUID());
        if (marker == null) return Set.of();
        long now = target.level().getGameTime();
        if (now - marker.tick() > 2L || !marker.sourceId().equals(source.getUUID())) {
            if (now - marker.tick() > 2L) MARKERS.remove(target.getUUID(), marker);
            return Set.of();
        }
        MARKERS.remove(target.getUUID(), marker);
        return Set.copyOf(marker.channels());
    }

    public static void clear(UUID entityId) {
        MARKERS.remove(entityId);
    }

    public static void clearAll() {
        MARKERS.clear();
    }
}
