package kr.moonseungjun.earthtostars;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;

@Mod(EarthToStars.MOD_ID)
public final class EarthToStars {
    public static final String MOD_ID = "earth_to_stars";
    public static final String VERSION = "0.2.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final List<String> REQUIRED_RUNTIME_MODS = List.of(
            "valkyrienskies",
            "genesis",
            "vlib",
            "zps",
            "zpl"
    );

    public EarthToStars() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        LOGGER.info("EARTH TO STARS {} VS/Genesis reboot bootstrap loaded", VERSION);
    }

    private void onServerStarted(ServerStartedEvent event) {
        List<String> missing = REQUIRED_RUNTIME_MODS.stream()
                .filter(modId -> !ModList.get().isLoaded(modId))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("EARTH TO STARS reboot stack missing required mods: " + String.join(", ", missing));
        }

        LOGGER.info(
                "EARTH_TO_STARS_REBOOT_STACK_BOOT_PASS version={} valkyrienskies=true genesis=true vlib=true zps=true zpl=true",
                VERSION
        );

        if ("1".equals(System.getenv("EARTH_TO_STARS_STACK_PROBE"))) {
            event.getServer().halt(false);
        }
    }
}
