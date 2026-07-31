package kr.moonseungjun.livingkingdoms;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LivingKingdoms.MOD_ID)
public final class LivingKingdoms {
    public static final String MOD_ID = "livingkingdoms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingKingdoms(IEventBus modEventBus) {
        FoundationCatalog.bootstrap();
        LOGGER.info(
                "Living Kingdoms foundation loaded: {} species, {} homelands, {} backgrounds, {} residences",
                FoundationCatalog.species().size(),
                FoundationCatalog.homelands().size(),
                FoundationCatalog.backgrounds().size(),
                FoundationCatalog.residences().size()
        );
    }
}
