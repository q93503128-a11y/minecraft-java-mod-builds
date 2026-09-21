package dev.moonseungjun.openworldrpg.integration.bootstrap;

import dev.moonseungjun.openworldrpg.integration.ExternalRuntimeContainment;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.bettercombat.BetterCombatAuthorityAdapter;
import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineAuthorityAdapter;
import dev.moonseungjun.openworldrpg.integration.verify.M0RuntimeVerificationHarness;
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

        ExternalRuntimeContainment.initialize(profile, logger);
        ExternalActorBindingRuntime.initialize(profile, logger);
        BetterCombatAuthorityAdapter.initialize(profile, logger);
        SpellEngineAuthorityAdapter.initialize(profile, logger);
        M0RuntimeVerificationHarness.initialize(profile, logger);

        logger.info(
                "Openworld RPG integration manifest schema {} accepted ({} dependency contracts, profile {}).",
                manifest.schemaVersion(),
                manifest.dependencies().size(),
                profile.id()
        );
    }
}
