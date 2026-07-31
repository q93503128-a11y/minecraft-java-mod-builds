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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(VillageGuardians.MOD_ID)
public final class VillageGuardians {
    public static final String MOD_ID = "villageguardians";
    public static final Logger LOGGER = LogUtils.getLogger();

    private int frozenTimeCheckTicks;

    public VillageGuardians(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Village Guardians governance core loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        VillageCouncilState.initializeServer(event.getServer());
        frozenTimeCheckTicks = 0;
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

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        frozenTimeCheckTicks++;
        if (frozenTimeCheckTicks >= 20) {
            frozenTimeCheckTicks = 0;
            VillageCouncilState.enforceFrozenTime(event.getServer());
        }
    }
}
