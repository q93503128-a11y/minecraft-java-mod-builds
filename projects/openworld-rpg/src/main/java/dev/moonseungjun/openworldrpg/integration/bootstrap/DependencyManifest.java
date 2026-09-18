package dev.moonseungjun.openworldrpg.integration.bootstrap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record DependencyManifest(int schemaVersion, List<DependencyContract> dependencies) {
    public ValidationReport validateStructure() {
        ValidationReport report = new ValidationReport();
        if (schemaVersion != 1) {
            report.add(ValidationSeverity.ERROR, "manifest.schema", "Unsupported dependency manifest schema: " + schemaVersion);
        }
        if (dependencies == null) {
            report.add(ValidationSeverity.ERROR, "manifest.dependencies", "Dependency list is missing.");
            return report;
        }

        Set<String> logicalIds = new HashSet<>();
        Set<String> resolvedModIds = new HashSet<>();

        for (DependencyContract dependency : dependencies) {
            if (dependency == null || dependency.logicalId() == null || dependency.logicalId().isBlank()) {
                report.add(ValidationSeverity.ERROR, "manifest.logical_id", "A dependency has no logicalId.");
                continue;
            }
            if (!logicalIds.add(dependency.logicalId())) {
                report.add(ValidationSeverity.ERROR, "manifest.logical_id_duplicate", "Duplicate logicalId: " + dependency.logicalId());
            }
            if (dependency.integrationModule() == null || dependency.integrationModule().isBlank()) {
                report.add(ValidationSeverity.ERROR, "manifest.integration_module", "Missing integrationModule for " + dependency.logicalId());
            }
            if (dependency.enforceVersion()
                    && (dependency.expectedVersion() == null || dependency.expectedVersion().isBlank())) {
                report.add(
                        ValidationSeverity.ERROR,
                        "manifest.expected_version",
                        "Version enforcement requires expectedVersion for " + dependency.logicalId()
                );
            }
            if (dependency.registryIdResolved() && !resolvedModIds.add(dependency.modId())) {
                report.add(ValidationSeverity.ERROR, "manifest.mod_id_duplicate", "Duplicate resolved modId: " + dependency.modId());
            }
        }
        return report;
    }
}
