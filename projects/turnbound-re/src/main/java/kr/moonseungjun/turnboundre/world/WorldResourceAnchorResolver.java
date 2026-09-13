package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.RegionDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves authored resource hotspot metadata without granting loot or replacing Minecraft gathering mechanics.
 * Production geometry owns placement; the locator/dimension pair is the stable gameplay identity.
 */
public final class WorldResourceAnchorResolver {
    public static final String TAG_PREFIX = "turnbound_re:resource=";
    private static final double MAX_CONFIRM_DISTANCE_SQUARED = 36.0D;

    public record Resolved(RegionDefinition region, RegionDefinition.ResourceAnchor anchor) {}

    private WorldResourceAnchorResolver() {}

    public static Optional<Resolved> resolve(
            DefinitionRegistry definitions,
            String locator,
            String dimension
    ) {
        if (definitions == null) return Optional.empty();
        return resolve(definitions.regions().values(), locator, dimension);
    }

    static Optional<Resolved> resolve(
            Collection<RegionDefinition> regions,
            String locator,
            String dimension
    ) {
        if (regions == null || locator == null || locator.isBlank() || dimension == null || dimension.isBlank()) {
            return Optional.empty();
        }
        Resolved match = null;
        for (RegionDefinition region : regions) {
            if (region == null || !dimension.equals(region.dimension())) continue;
            for (RegionDefinition.ResourceAnchor anchor : region.resourceAnchors()) {
                if (anchor == null || !locator.equals(anchor.locator())) continue;
                if (match != null) return Optional.empty();
                match = new Resolved(region, anchor);
            }
        }
        return Optional.ofNullable(match);
    }

    public static String tagFor(String locator) {
        if (locator == null || locator.isBlank()) throw new IllegalArgumentException("resource locator required");
        return TAG_PREFIX + locator;
    }

    public static Optional<String> locatorFromTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return Optional.empty();
        List<String> matches = new ArrayList<>();
        for (String tag : tags) {
            if (tag == null || !tag.startsWith(TAG_PREFIX) || tag.length() <= TAG_PREFIX.length()) continue;
            matches.add(tag.substring(TAG_PREFIX.length()));
        }
        if (matches.size() != 1) return Optional.empty();
        String locator = matches.getFirst();
        return locator.isBlank() ? Optional.empty() : Optional.of(locator);
    }

    public static boolean withinConfirmRange(double distanceSquared) {
        return Double.isFinite(distanceSquared)
                && distanceSquared >= 0.0D
                && distanceSquared <= MAX_CONFIRM_DISTANCE_SQUARED;
    }
}
