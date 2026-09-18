package dev.moonseungjun.openworldrpg.integration.bootstrap;

import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public final class DependencyValidator {
    private DependencyValidator() {
    }

    public static ValidationReport validate(DependencyManifest manifest, RuntimeProfile profile) {
        ValidationReport report = new ValidationReport();
        FabricLoader loader = FabricLoader.getInstance();

        for (DependencyContract dependency : manifest.dependencies()) {
            boolean required = dependency.requiredFor(profile);

            if (!dependency.registryIdResolved()) {
                if (required) {
                    report.add(
                            ValidationSeverity.ERROR,
                            "dependency.unresolved_mod_id",
                            dependency.logicalId() + " is required by profile " + profile.id()
                                    + " but its runtime mod id is not yet verified."
                    );
                }
                continue;
            }

            Optional<ModContainer> container = loader.getModContainer(dependency.modId());
            if (container.isEmpty()) {
                if (required) {
                    report.add(
                            ValidationSeverity.ERROR,
                            "dependency.missing",
                            dependency.logicalId() + " (" + dependency.modId() + ") is required by profile "
                                    + profile.id() + " but is not loaded."
                    );
                }
                continue;
            }

            String actualVersion = container.get().getMetadata().getVersion().getFriendlyString();
            if (dependency.enforceVersion()
                    && dependency.expectedVersion() != null
                    && !dependency.expectedVersion().isBlank()
                    && !dependency.expectedVersion().equals(actualVersion)) {
                report.add(
                        required ? ValidationSeverity.ERROR : ValidationSeverity.WARN,
                        "dependency.version",
                        dependency.logicalId() + " expected " + dependency.expectedVersion()
                                + " but loaded " + actualVersion + "."
                );
            }
        }

        return report;
    }
}
