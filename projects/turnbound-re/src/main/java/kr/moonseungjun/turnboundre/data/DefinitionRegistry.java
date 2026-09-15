package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Immutable, atomically validated registry for all server-authoritative content definitions. */
public final class DefinitionRegistry {
    private final Map<String, ActionDefinition> actions;
    private final Map<String, CharacterDefinition> characters;
    private final Map<String, StatusDefinition> statuses;
    private final Map<String, EncounterDefinition> encounters;
    private final Map<String, RewardTableDefinition> rewards;
    private final Map<String, ProgressionDefinition> progressions;
    private final Map<String, EquipmentDefinition> equipment;
    private final Map<String, RegionDefinition> regions;
    private final Map<String, ExternalWorldProfileDefinition> externalWorldProfiles;

    private DefinitionRegistry(
            Map<String, ActionDefinition> actions,
            Map<String, CharacterDefinition> characters,
            Map<String, StatusDefinition> statuses,
            Map<String, EncounterDefinition> encounters,
            Map<String, RewardTableDefinition> rewards,
            Map<String, ProgressionDefinition> progressions,
            Map<String, EquipmentDefinition> equipment,
            Map<String, RegionDefinition> regions,
            Map<String, ExternalWorldProfileDefinition> externalWorldProfiles
    ) {
        this.actions = Collections.unmodifiableMap(actions);
        this.characters = Collections.unmodifiableMap(characters);
        this.statuses = Collections.unmodifiableMap(statuses);
        this.encounters = Collections.unmodifiableMap(encounters);
        this.rewards = Collections.unmodifiableMap(rewards);
        this.progressions = Collections.unmodifiableMap(progressions);
        this.equipment = Collections.unmodifiableMap(equipment);
        this.regions = Collections.unmodifiableMap(regions);
        this.externalWorldProfiles = Collections.unmodifiableMap(externalWorldProfiles);
    }

    public static DefinitionRegistry create(List<ActionDefinition> actions, List<CharacterDefinition> characters) {
        return create(actions, characters, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses
    ) {
        return create(actions, characters, statuses, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public static DefinitionRegistry create(DefinitionBundle bundle) {
        if (bundle == null) throw new IllegalArgumentException("bundle must not be null");
        return create(
                bundle.actions(), bundle.characters(), bundle.statuses(), bundle.encounters(),
                bundle.rewards(), bundle.progressions(), bundle.equipment(), bundle.regions(), bundle.externalWorldProfiles());
    }

    /** Compatibility overload for compact M0-M3 registries that have no progression tuning. */
    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses,
            List<EncounterDefinition> encounters,
            List<RewardTableDefinition> rewards
    ) {
        return create(actions, characters, statuses, encounters, rewards, List.of(), List.of(), List.of(), List.of());
    }

    /** Compatibility overload for M4-M5 registries created before region/equipment definitions existed. */
    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses,
            List<EncounterDefinition> encounters,
            List<RewardTableDefinition> rewards,
            List<ProgressionDefinition> progressions
    ) {
        return create(actions, characters, statuses, encounters, rewards, progressions, List.of(), List.of(), List.of());
    }

    /** Compatibility overload for callers created after region definitions but before equipment definitions. */
    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses,
            List<EncounterDefinition> encounters,
            List<RewardTableDefinition> rewards,
            List<ProgressionDefinition> progressions,
            List<RegionDefinition> regions
    ) {
        return create(actions, characters, statuses, encounters, rewards, progressions, List.of(), regions, List.of());
    }

    /** Compatibility overload for callers authored before external-world profiles existed. */
    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses,
            List<EncounterDefinition> encounters,
            List<RewardTableDefinition> rewards,
            List<ProgressionDefinition> progressions,
            List<EquipmentDefinition> equipment,
            List<RegionDefinition> regions
    ) {
        return create(actions, characters, statuses, encounters, rewards, progressions, equipment, regions, List.of());
    }

    public static DefinitionRegistry create(
            List<ActionDefinition> actions,
            List<CharacterDefinition> characters,
            List<StatusDefinition> statuses,
            List<EncounterDefinition> encounters,
            List<RewardTableDefinition> rewards,
            List<ProgressionDefinition> progressions,
            List<EquipmentDefinition> equipment,
            List<RegionDefinition> regions,
            List<ExternalWorldProfileDefinition> externalWorldProfiles
    ) {
        if (actions == null || characters == null || statuses == null || encounters == null
                || rewards == null || progressions == null || equipment == null || regions == null
                || externalWorldProfiles == null) {
            throw new IllegalArgumentException("definition lists must not be null");
        }

        List<String> errors = new ArrayList<>();
        errors.addAll(DefinitionValidator.validateActions(actions));
        errors.addAll(DefinitionValidator.validateStatuses(statuses));
        errors.addAll(ProgressionDefinitionValidator.validate(progressions));
        errors.addAll(EquipmentDefinitionValidator.validate(equipment));

        Set<String> actionIds = ids(actions.stream().map(ActionDefinition::id).toList());
        errors.addAll(DefinitionValidator.validateCharacters(characters, actionIds));
        errors.addAll(DefinitionCrossReferenceValidator.validate(actions, characters, statuses));

        Map<String, CharacterDefinition> characterMap = mapCharacters(characters);
        errors.addAll(ProgressionDefinitionValidator.validateStarterParties(progressions, characterMap));
        Set<String> characterIds = Set.copyOf(characterMap.keySet());
        errors.addAll(DefinitionValidator.validateRewards(rewards, characterIds));

        Set<String> rewardIds = ids(rewards.stream().map(RewardTableDefinition::id).toList());
        errors.addAll(DefinitionValidator.validateEncounters(encounters, characterMap, rewardIds));
        Set<String> encounterIds = ids(encounters.stream().map(EncounterDefinition::id).toList());
        errors.addAll(RegionDefinitionValidator.validate(regions, encounterIds));
        errors.addAll(validateExternalWorldProfiles(externalWorldProfiles, regions));

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid TURNBOUND definitions: " + String.join("; ", errors));
        }

        return new DefinitionRegistry(
                mapActions(actions), characterMap, mapStatuses(statuses), mapEncounters(encounters),
                mapRewards(rewards), mapProgressions(progressions), mapEquipment(equipment), mapRegions(regions),
                mapExternalWorldProfiles(externalWorldProfiles));
    }

    private static List<String> validateExternalWorldProfiles(
            List<ExternalWorldProfileDefinition> profiles,
            List<RegionDefinition> regions
    ) {
        List<String> errors = new ArrayList<>();
        Set<String> profileIds = new HashSet<>();
        Map<String, String> fastTravelDimensions = new LinkedHashMap<>();
        Map<String, String> resourceDimensions = new LinkedHashMap<>();
        Map<String, String> encounterDimensions = new LinkedHashMap<>();

        for (RegionDefinition region : regions) {
            if (region == null) continue;
            for (RegionDefinition.FastTravelAnchor anchor : region.fastTravelAnchors()) {
                if (anchor != null) fastTravelDimensions.put(anchor.locator(), region.dimension());
            }
            for (RegionDefinition.ResourceAnchor anchor : region.resourceAnchors()) {
                if (anchor != null) resourceDimensions.put(anchor.locator(), region.dimension());
            }
            for (RegionDefinition.EncounterAnchor anchor : region.encounterAnchors()) {
                if (anchor != null) encounterDimensions.put(anchor.locator(), region.dimension());
            }
        }

        for (ExternalWorldProfileDefinition profile : profiles) {
            if (profile == null) {
                errors.add("external world profile must not be null");
                continue;
            }
            if (profile.id() == null || profile.id().isBlank()) {
                errors.add("external world profile id must not be blank");
                continue;
            }
            if (!profileIds.add(profile.id())) errors.add("duplicate external world profile id " + profile.id());
            if (profile.sourceVersion() == null || profile.sourceVersion().isBlank()) {
                errors.add(profile.id() + " sourceVersion must not be blank");
            }
            if (profile.dimension() == null || profile.dimension().isBlank()) {
                errors.add(profile.id() + " dimension must not be blank");
            }
            if (profile.anchors().isEmpty()) errors.add(profile.id() + " must contain at least one anchor");

            Set<String> profileLocators = new HashSet<>();
            for (ExternalWorldProfileDefinition.Anchor anchor : profile.anchors()) {
                if (anchor == null) {
                    errors.add(profile.id() + " contains null anchor");
                    continue;
                }
                String kind = anchor.kind();
                String locator = anchor.locator();
                if (kind == null || kind.isBlank()) {
                    errors.add(profile.id() + " anchor kind must not be blank");
                    continue;
                }
                if (locator == null || locator.isBlank()) {
                    errors.add(profile.id() + " anchor locator must not be blank");
                    continue;
                }
                if (!profileLocators.add(locator)) {
                    errors.add(profile.id() + " duplicate anchor locator " + locator);
                }
                if (anchor.y() < -128 || anchor.y() > 512) {
                    errors.add(profile.id() + " anchor y out of sanity range " + locator + "=" + anchor.y());
                }

                Map<String, String> expectedDimensions = switch (kind) {
                    case ExternalWorldProfileDefinition.FAST_TRAVEL -> fastTravelDimensions;
                    case ExternalWorldProfileDefinition.RESOURCE -> resourceDimensions;
                    case ExternalWorldProfileDefinition.ENCOUNTER -> encounterDimensions;
                    default -> null;
                };
                if (expectedDimensions == null) {
                    errors.add(profile.id() + " unknown anchor kind " + kind + " for " + locator);
                    continue;
                }
                String expectedDimension = expectedDimensions.get(locator);
                if (expectedDimension == null) {
                    errors.add(profile.id() + " unresolved " + kind + " locator " + locator);
                } else if (!expectedDimension.equals(profile.dimension())) {
                    errors.add(profile.id() + " locator dimension mismatch " + locator
                            + " expected=" + expectedDimension + " profile=" + profile.dimension());
                }
            }
        }
        return List.copyOf(errors);
    }

    private static Set<String> ids(List<String> values) {
        return values.stream().filter(id -> id != null && !id.isBlank()).collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, ActionDefinition> mapActions(List<ActionDefinition> values) {
        Map<String, ActionDefinition> out = new LinkedHashMap<>();
        for (ActionDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, CharacterDefinition> mapCharacters(List<CharacterDefinition> values) {
        Map<String, CharacterDefinition> out = new LinkedHashMap<>();
        for (CharacterDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, StatusDefinition> mapStatuses(List<StatusDefinition> values) {
        Map<String, StatusDefinition> out = new LinkedHashMap<>();
        for (StatusDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, EncounterDefinition> mapEncounters(List<EncounterDefinition> values) {
        Map<String, EncounterDefinition> out = new LinkedHashMap<>();
        for (EncounterDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, RewardTableDefinition> mapRewards(List<RewardTableDefinition> values) {
        Map<String, RewardTableDefinition> out = new LinkedHashMap<>();
        for (RewardTableDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, ProgressionDefinition> mapProgressions(List<ProgressionDefinition> values) {
        Map<String, ProgressionDefinition> out = new LinkedHashMap<>();
        for (ProgressionDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, EquipmentDefinition> mapEquipment(List<EquipmentDefinition> values) {
        Map<String, EquipmentDefinition> out = new LinkedHashMap<>();
        for (EquipmentDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, RegionDefinition> mapRegions(List<RegionDefinition> values) {
        Map<String, RegionDefinition> out = new LinkedHashMap<>();
        for (RegionDefinition value : values) out.put(value.id(), value);
        return out;
    }

    private static Map<String, ExternalWorldProfileDefinition> mapExternalWorldProfiles(
            List<ExternalWorldProfileDefinition> values
    ) {
        Map<String, ExternalWorldProfileDefinition> out = new LinkedHashMap<>();
        for (ExternalWorldProfileDefinition value : values) out.put(value.id(), value);
        return out;
    }

    public Map<String, ActionDefinition> actions() { return actions; }
    public Map<String, CharacterDefinition> characters() { return characters; }
    public Map<String, StatusDefinition> statuses() { return statuses; }
    public Map<String, EncounterDefinition> encounters() { return encounters; }
    public Map<String, RewardTableDefinition> rewards() { return rewards; }
    public Map<String, ProgressionDefinition> progressions() { return progressions; }
    public Map<String, EquipmentDefinition> equipment() { return equipment; }
    public Map<String, RegionDefinition> regions() { return regions; }
    public Map<String, ExternalWorldProfileDefinition> externalWorldProfiles() { return externalWorldProfiles; }
}
