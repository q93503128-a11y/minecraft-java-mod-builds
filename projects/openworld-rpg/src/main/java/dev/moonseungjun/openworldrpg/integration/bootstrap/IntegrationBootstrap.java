package dev.moonseungjun.openworldrpg.integration.bootstrap;

import org.slf4j.Logger;

public final class IntegrationBootstrap {
    private IntegrationBootstrap() {
    }

    public static void bootstrap(RuntimeProfile profile, Logger logger) {
        DependencyManifest manifest = DependencyManifestLoader.loadDefault();

        ValidationReport report = manifest.validateStructure();
        report.addAll(DependencyValidator.validate(manifest, profile));
        report.logTo(logger);

        if (report.hasErrors()) {
            throw new IllegalStateException(
                    "Openworld RPG integration contract failed for profile " + profile.id()
                            + ". See the log for the exact dependency/data contract error."
            );
        }

        logger.info(
                "Openworld RPG integration manifest schema {} accepted ({} dependency contracts, profile {}).",
                manifest.schemaVersion(),
                manifest.dependencies().size(),
                profile.id()
        );
    }
}
