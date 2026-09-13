package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Semantic/cross-reference validation for authored region definitions. */
public final class RegionDefinitionValidator {
    private static final Set<String> KINDS = Set.of("HUB", "REGION", "DUNGEON");
    private static final Set<String> RESOURCE_ACTIVITIES = Set.of("MINING", "FARMING", "FISHING");

    private RegionDefinitionValidator() {}

    public static List<String> validate(List<RegionDefinition> regions, Set<String> encounterIds) {
        if (regions == null) return List.of("regions must not be null");
        if (encounterIds == null) return List.of("region encounterIds must not be null");

        List<String> errors = new ArrayList<>();
        Set<String> regionIds = new HashSet<>();
        Set<String> anchorIds = new HashSet<>();
        Set<String> resourceAnchorIds = new HashSet<>();
        Set<String> locators = new HashSet<>();

        for (RegionDefinition region : regions) {
            if (region == null) {
                errors.add("region must not be null");
                continue;
            }
            String id = region.id();
            if (!validId(id)) errors.add("invalid region id: " + id);
            else if (!regionIds.add(id)) errors.add("duplicate region id: " + id);
            if (!KINDS.contains(region.kind())) errors.add(id + ": unknown region kind " + region.kind());
            if (!validId(region.dimension())) errors.add(id + ": invalid dimension " + region.dimension());

            Set<String> localExits = new HashSet<>();
            for (String exit : region.exits()) {
                if (!validId(exit)) errors.add(id + ": invalid exit id " + exit);
                else if (!localExits.add(exit)) errors.add(id + ": duplicate exit " + exit);
                if (id != null && id.equals(exit)) errors.add(id + ": region cannot exit to itself");
            }

            for (RegionDefinition.EncounterAnchor anchor : region.encounterAnchors()) {
                if (anchor == null) {
                    errors.add(id + ": encounter anchor must not be null");
                    continue;
                }
                if (!validId(anchor.id())) errors.add(id + ": invalid encounter anchor id " + anchor.id());
                else if (!anchorIds.add(anchor.id())) errors.add("duplicate encounter anchor id: " + anchor.id());
                if (!validId(anchor.encounter())) errors.add(anchor.id() + ": invalid encounter id " + anchor.encounter());
                else if (!encounterIds.contains(anchor.encounter())) errors.add(anchor.id() + ": unresolved encounter " + anchor.encounter());
                validateLocator(anchor.id(), anchor.locator(), locators, errors);
            }

            for (RegionDefinition.ResourceAnchor anchor : region.resourceAnchors()) {
                if (anchor == null) {
                    errors.add(id + ": resource anchor must not be null");
                    continue;
                }
                if (!validId(anchor.id())) errors.add(id + ": invalid resource anchor id " + anchor.id());
                else if (!resourceAnchorIds.add(anchor.id())) errors.add("duplicate resource anchor id: " + anchor.id());
                if (!RESOURCE_ACTIVITIES.contains(anchor.activity())) {
                    errors.add(anchor.id() + ": unknown resource activity " + anchor.activity());
                }
                validateLocator(anchor.id(), anchor.locator(), locators, errors);
            }
        }

        for (RegionDefinition region : regions) {
            if (region == null) continue;
            for (String exit : region.exits()) {
                if (validId(exit) && !regionIds.contains(exit)) {
                    errors.add(region.id() + ": unresolved exit region " + exit);
                }
            }
        }
        return List.copyOf(errors);
    }

    private static void validateLocator(String anchorId, String locator, Set<String> locators, List<String> errors) {
        if (!validId(locator)) errors.add(anchorId + ": invalid locator " + locator);
        else if (!locators.add(locator)) errors.add("duplicate authored locator: " + locator);
    }

    private static boolean validId(String value) {
        if (value == null || value.isBlank()) return false;
        int colon = value.indexOf(':');
        if (colon <= 0 || colon == value.length() - 1 || colon != value.lastIndexOf(':')) return false;
        return validPart(value.substring(0, colon), false) && validPart(value.substring(colon + 1), true);
    }

    private static boolean validPart(String part, boolean path) {
        if (part.isBlank()) return false;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            boolean ok = c >= 'a' && c <= 'z'
                    || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || (path && c == '/');
            if (!ok) return false;
        }
        return true;
    }
}
