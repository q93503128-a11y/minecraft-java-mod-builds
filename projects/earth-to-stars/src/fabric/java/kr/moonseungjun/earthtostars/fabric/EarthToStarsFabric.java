package kr.moonseungjun.earthtostars.fabric;

import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.persistence.ShipBootstrapCatalog;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EarthToStarsFabric implements ModInitializer {
    public static final String MOD_ID = "earth_to_stars";
    public static final String VERSION = "0.3.0-alpha.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ModuleCatalog BOOTSTRAP_CATALOG = ShipBootstrapCatalog.create();

    @Override
    public void onInitialize() {
        LOGGER.info(
                "EARTH TO STARS {} Fabric 26.2 standalone kernel loaded modules={}",
                VERSION,
                BOOTSTRAP_CATALOG.definitions().size()
        );
    }

    public static ModuleCatalog bootstrapCatalog() {
        return BOOTSTRAP_CATALOG;
    }
}
