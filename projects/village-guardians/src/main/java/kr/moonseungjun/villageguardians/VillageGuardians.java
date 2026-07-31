package kr.moonseungjun.villageguardians;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(VillageGuardians.MOD_ID)
public final class VillageGuardians {
    public static final String MOD_ID = "villageguardians";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VillageGuardians(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Village Guardians governance core loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        VillageCouncilState.initializeServer(event.getServer());
        LOGGER.info("Village time initialized and natural daylight cycle disabled");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VillageCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            VillageCouncilState.registerPlayer(serverPlayer);
            VillageCouncilState.enforceFrozenTime(serverPlayer.getServer());
        }
    }
}
