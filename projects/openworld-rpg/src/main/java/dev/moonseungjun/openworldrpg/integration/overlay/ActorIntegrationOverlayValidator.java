package dev.moonseungjun.openworldrpg.integration.overlay;

import dev.moonseungjun.openworldrpg.integration.bootstrap.ValidationReport;
import dev.moonseungjun.openworldrpg.integration.bootstrap.ValidationSeverity;
import java.util.Set;
import java.util.regex.Pattern;

public final class ActorIntegrationOverlayValidator {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> REQUIRED_POLICIES = Set.of(
            "presentation",
            "animation",
            "movement_ai",
            "combat_ai",
            "spawn",
            "stats",
            "damage",
            "loot",
            "recipes",
            "worldgen",
            "capture_or_duplication",
            "progression",
            "save_ownership"
    );

    private ActorIntegrationOverlayValidator() {
    }

    public static ValidationReport validate(ActorIntegrationOverlay overlay) {
        ValidationReport report = new ValidationReport();

        if (overlay.target() == null || !RESOURCE_ID.matcher(overlay.target()).matches()) {
            report.add(ValidationSeverity.ERROR, "overlay.target", "Invalid target resource id: " + overlay.target());
        }
        if (overlay.role() == null || overlay.role().isBlank()) {
            report.add(ValidationSeverity.ERROR, "overlay.role", "Actor overlay role must not be blank.");
        }
        if (overlay.policy() == null) {
            report.add(ValidationSeverity.ERROR, "overlay.policy", "Actor overlay policy map is missing.");
            return report;
        }

        for (String key : REQUIRED_POLICIES) {
            if (!overlay.policy().containsKey(key) || overlay.policy().get(key) == null) {
                report.add(ValidationSeverity.ERROR, "overlay.policy." + key, "Missing actor integration policy: " + key);
            }
        }

        if (overlay.tags() != null) {
            for (String tag : overlay.tags()) {
                if (tag == null || !RESOURCE_ID.matcher(tag).matches()) {
                    report.add(ValidationSeverity.ERROR, "overlay.tag", "Invalid actor integration tag: " + tag);
                }
            }
        }

        if (overlay.encounter() != null
                && !overlay.encounter().isBlank()
                && !RESOURCE_ID.matcher(overlay.encounter()).matches()) {
            report.add(ValidationSeverity.ERROR, "overlay.encounter", "Invalid encounter resource id: " + overlay.encounter());
        }

        return report;
    }
}
