package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.RegionDefinition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;

import java.util.Collection;
import java.util.Optional;

/** Resolves physical travel-marker tags back to current authored fast-travel definitions. */
public final class WorldFastTravelResolver {
    public static final String TAG_PREFIX = "turnbound_re:travel=";
    public static final double MAX_USE_DISTANCE_SQR = 36.0D;

    public record Resolved(RegionDefinition region, RegionDefinition.FastTravelAnchor anchor) {}

    private WorldFastTravelResolver() {}

    public static String tagFor(String locator) {
        if (locator == null || locator.isBlank()) throw new IllegalArgumentException("locator required");
        return TAG_PREFIX + locator;
    }

    public static Optional<String> locatorFromTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) return Optional.empty();
        String found = null;
        for (String tag : tags) {
            if (tag == null || !tag.startsWith(TAG_PREFIX)) continue;
            String locator = tag.substring(TAG_PREFIX.length());
            if (locator.isBlank()) return Optional.empty();
            if (found != null && !found.equals(locator)) return Optional.empty();
            found = locator;
        }
        return Optional.ofNullable(found);
    }

    public static Optional<Resolved> resolveEntity(DefinitionRegistry registry, Entity entity, String dimensionId) {
        if (entity == null || entity.getType() != EntityTypes.INTERACTION) return Optional.empty();
        return locatorFromTags(entity.entityTags()).flatMap(locator -> resolve(registry, locator, dimensionId));
    }

    public static Optional<Resolved> resolve(DefinitionRegistry registry, String locator, String dimensionId) {
        if (registry == null || locator == null || locator.isBlank() || dimensionId == null || dimensionId.isBlank()) {
            return Optional.empty();
        }
        for (RegionDefinition region : registry.regions().values()) {
            if (region == null || !dimensionId.equals(region.dimension())) continue;
            for (RegionDefinition.FastTravelAnchor anchor : region.fastTravelAnchors()) {
                if (anchor != null && locator.equals(anchor.locator())) return Optional.of(new Resolved(region, anchor));
            }
        }
        return Optional.empty();
    }

    public static boolean withinUseRange(double distanceSqr) {
        return Double.isFinite(distanceSqr) && distanceSqr >= 0.0D && distanceSqr <= MAX_USE_DISTANCE_SQR;
    }
}
