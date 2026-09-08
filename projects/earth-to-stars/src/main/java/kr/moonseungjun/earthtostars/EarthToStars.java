package kr.moonseungjun.earthtostars;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EarthToStars.MOD_ID)
public final class EarthToStars {
    public static final String MOD_ID = "earth_to_stars";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EarthToStars(IEventBus modEventBus) {
        LOGGER.info("EARTH TO STARS {} authoritative ship kernel bootstrap loaded", VERSION);
    }
}
