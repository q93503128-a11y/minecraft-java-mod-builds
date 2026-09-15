package kr.moonseungjun.earthtostars;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.List;

@Mod(EarthToStars.MOD_ID)
public final class EarthToStars {
    public static final String MOD_ID = "earth_to_stars";
    public static final String VERSION = "0.2.0-alpha.2";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final List<String> REQUIRED_RUNTIME_MODS = List.of(
            "valkyrienskies",
            "genesis",
            "vlib",
            "zps",
            "zpl"
    );

    public EarthToStars() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EarthToStarsContent.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(StarterCraftControlManager::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        LOGGER.info("EARTH TO STARS {} physical VS starter-craft bootstrap loaded", VERSION);
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
            ServerLevel overworld = event.getServer().overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            int x = spawn.getX() + 32;
            int z = spawn.getZ();
            int surface = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            int y = Math.min(overworld.getMaxBuildHeight() - 8, surface + 4);
            StarterCraftDeploymentService.deploy(overworld, new BlockPos(x, y, z));
            LOGGER.info("EARTH_TO_STARS_STARTER_CRAFT_ASSEMBLY_PASS physical_vs_ship=true octo=true zpl_thrusters=true zpl_gyros=true finite_battery=true");
            event.getServer().halt(false);
        }
    }
}
