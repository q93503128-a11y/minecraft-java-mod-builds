package dev.moonseungjun.openworldrpg.integration.verify;

import dev.moonseungjun.openworldrpg.equipment.OrdinaryEquipmentAffixCatalogRegistry;
import dev.moonseungjun.openworldrpg.fishing.R01FishingSpatialRegistry;
import dev.moonseungjun.openworldrpg.world.spatial.R01QuarrySpatialBindingRegistry;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingRegistry;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import dev.moonseungjun.openworldrpg.world.structure.R01StructureBindingLoader;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Non-mutating R01 integration-playtest bootstrap.
 *
 * <p>Unlike the M0 fixture, this mode never replaces normal opening/progression state on join.
 * Its purpose is to run the real R01 flow while retaining the manual M0 combat probes for focused
 * diagnosis when explicitly requested. The marker exists only in a dedicated playtest artifact.</p>
 */
public final class R01PlayerVerificationBootstrap {
    public static final String ENABLE_PROPERTY =
            "openworld_rpg.r01PlayerVerification";
    public static final String EMBEDDED_MARKER =
            "data/openworld_rpg/integration/r01_player_verification.enabled";

    private R01PlayerVerificationBootstrap() {
    }

    public static boolean enabled() {
        if (Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"))) {
            return true;
        }
        return R01PlayerVerificationBootstrap.class.getResource(
                "/" + EMBEDDED_MARKER
        ) != null;
    }

    public static void verifyStaticContracts(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (!enabled()) {
            return;
        }
        if (M0PlayerVerificationBootstrap.enabled()) {
            throw new IllegalStateException(
                    "R01 integration verification cannot share the M0 auto-profile marker."
            );
        }

        var spatial = R01SpatialBindingRegistry.data();
        var structures = R01StructureBindingLoader.loadBundled();
        var affixes = OrdinaryEquipmentAffixCatalogRegistry.data();
        logger.info(
                "OPENWORLD_RPG_R01_VERIFICATION_CONTRACT_PASS mapBuild={} anchors={} areas={} volumes={} routes={} structures={} services={} properties={} fishingSpots={} staticAffixes={} runtimeAffixes={} resourceAffixAuthorityReady={} spatialProductionReady={} structureProductionReady={} alderfordRuntimeProductionReady={} quarryRuntimeProductionReady={} fishingProductionReady={}",
                spatial.mapBuild(),
                spatial.anchors().size(),
                spatial.areas().size(),
                spatial.volumes().size(),
                spatial.routes().size(),
                structures.structures().size(),
                structures.services().size(),
                structures.properties().size(),
                R01FishingSpatialRegistry.allAuthoredSpots().size(),
                affixes.affixes().size(),
                affixes.implementedDefinitions().size(),
                affixes.resourceAuthorityReady(),
                spatial.productionReady(),
                structures.productionReady(),
                R01AlderfordRuntimeBindingRegistry.productionReady(),
                R01QuarrySpatialBindingRegistry.productionReady(),
                R01FishingSpatialRegistry.productionReady()
        );
    }

    public static void prepare(ServerPlayer player, Logger logger) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(logger, "logger");
        if (!enabled()) {
            return;
        }
        if (M0PlayerVerificationBootstrap.enabled()) {
            throw new IllegalStateException(
                    "R01 integration verification cannot mutate the player through M0 auto-profile setup."
            );
        }

        logger.info(
                "OPENWORLD_RPG_R01_PLAYER_READY player={} naturalOpening=true autoProfileMutation=false manualM0Probes=true",
                player.getGameProfile().name()
        );
    }
}
