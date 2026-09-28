package dev.moonseungjun.openworldrpg.equipment;

import java.util.Objects;
import org.slf4j.Logger;

/** Cached validated ordinary-affix catalog shared by loot, merchants and forge. */
public final class OrdinaryEquipmentAffixCatalogRegistry {
    private static volatile OrdinaryEquipmentAffixCatalogData data;

    private OrdinaryEquipmentAffixCatalogRegistry() {
    }

    public static synchronized void initialize(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (data != null) {
            return;
        }
        data = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        logger.info(
                "Openworld RPG ordinary affix catalog loaded: staticAffixes={}, runtimeImplemented={}.",
                data.affixes().size(),
                data.implementedDefinitions().size()
        );
    }

    public static OrdinaryEquipmentAffixCatalogData data() {
        OrdinaryEquipmentAffixCatalogData current = data;
        if (current == null) {
            current = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
            data = current;
        }
        return current;
    }
}
