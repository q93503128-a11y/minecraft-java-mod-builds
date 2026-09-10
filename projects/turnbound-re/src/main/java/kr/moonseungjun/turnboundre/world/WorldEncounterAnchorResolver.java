package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.EncounterDefinition;
import kr.moonseungjun.turnboundre.data.RegionDefinition;
import kr.moonseungjun.turnboundre.data.RewardTableDefinition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves authored world marker tags to validated RegionDefinition encounter anchors.
 * Coordinates are intentionally absent: fixed-world structures own placement, while data owns meaning.
 */
public final class WorldEncounterAnchorResolver {
    public static final String TAG_PREFIX = "turnbound_re:anchor=";
    public static final double MAX_CONFIRM_DISTANCE_SQR = 36.0D;

    public record Resolved(
            RegionDefinition region,
            RegionDefinition.EncounterAnchor anchor,
            EncounterDefinition encounter
    ) {}

    private WorldEncounterAnchorResolver() {}

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

    public static Optional<Resolved> resolveEntity(
            DefinitionRegistry registry,
            Entity entity,
            String dimensionId
    ) {
        if (entity == null || entity.getType() != EntityType.INTERACTION) return Optional.empty();
        return locatorFromTags(entity.getTags()).flatMap(locator -> resolve(registry, locator, dimensionId));
    }

    public static Optional<Resolved> resolve(
            DefinitionRegistry registry,
            String locator,
            String dimensionId
    ) {
        if (registry == null) return Optional.empty();
        return resolve(registry.regions().values(), registry.encounters(), locator, dimensionId);
    }

    static Optional<Resolved> resolve(
            Collection<RegionDefinition> regions,
            Map<String, EncounterDefinition> encounters,
            String locator,
            String dimensionId
    ) {
        if (regions == null || encounters == null || locator == null || locator.isBlank()
                || dimensionId == null || dimensionId.isBlank()) return Optional.empty();
        for (RegionDefinition region : regions) {
            if (region == null || !dimensionId.equals(region.dimension())) continue;
            for (RegionDefinition.EncounterAnchor anchor : region.encounterAnchors()) {
                if (anchor == null || !locator.equals(anchor.locator())) continue;
                EncounterDefinition encounter = encounters.get(anchor.encounter());
                if (encounter == null) return Optional.empty();
                return Optional.of(new Resolved(region, anchor, encounter));
            }
        }
        return Optional.empty();
    }

    public static boolean matchesEntity(Entity entity, java.util.UUID entityId, String locator) {
        if (entity == null || entityId == null || locator == null || locator.isBlank()) return false;
        if (!entityId.equals(entity.getUUID()) || entity.getType() != EntityType.INTERACTION) return false;
        return locatorFromTags(entity.getTags()).filter(locator::equals).isPresent();
    }

    public static boolean withinConfirmRange(double distanceSqr) {
        return Double.isFinite(distanceSqr) && distanceSqr >= 0.0D && distanceSqr <= MAX_CONFIRM_DISTANCE_SQR;
    }

    public static List<String> enemySourceEntities(DefinitionRegistry registry, EncounterDefinition encounter) {
        if (registry == null || encounter == null) return List.of();
        List<String> out = new ArrayList<>();
        for (EncounterDefinition.EnemySlot slot : encounter.enemies()) {
            CharacterDefinition character = registry.characters().get(slot.character());
            if (character == null) continue;
            String source = character.sourceEntity();
            out.add(source == null || source.isBlank() ? character.id() : source);
        }
        return List.copyOf(out);
    }

    public static List<String> rewardKinds(DefinitionRegistry registry, EncounterDefinition encounter) {
        if (registry == null || encounter == null) return List.of();
        RewardTableDefinition rewards = registry.rewards().get(encounter.rewardTable());
        if (rewards == null) return List.of();
        Set<String> kinds = new LinkedHashSet<>();
        for (RewardTableDefinition.Roll roll : rewards.rolls()) {
            if (roll != null && roll.type() != null && !roll.type().isBlank()) kinds.add(roll.type());
        }
        return List.copyOf(kinds);
    }
}
