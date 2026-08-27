package kr.moonseungjun.titanbreak;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.titanbreak.combat.AugmentedMobilityService;
import kr.moonseungjun.titanbreak.combat.ReflexFieldService;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(Titanbreak.MOD_ID)
public final class Titanbreak {
    public static final String MOD_ID = "titanbreak";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final int REFLEX_RATING = 80;
    private static final double REFLEX_RADIUS = 96.0;
    private static final double OVERHEAT_LOCK = 95.0;

    public Titanbreak(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        modEventBus.addListener(TitanbreakNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ReflexFieldService::onEntityTickPre);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("TITANBREAK {} loaded", VERSION);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TitanPlayerData.get(((ServerLevel) player.level()).getServer()).ensureProfile(player);
        TitanbreakNetwork.sync(player);
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ReflexFieldService.clear(event.getEntity().getUUID());
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ReflexFieldService.clear(player.getUUID());
        TitanPlayerData.get(((ServerLevel) player.level()).getServer()).ensureProfile(player);
        TitanbreakNetwork.sync(player);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);

        boolean installed = player.getOffhandItem().is(ModItems.REFLEX_DRIVE_I.get());
        boolean requested = installed && player.isCrouching();
        boolean active = requested && state.heat() < OVERHEAT_LOCK;

        if (active) {
            data.setHeat(player, state.heat() + 0.65);
            data.setSanity(player, state.sanity() - 0.002);
        } else if (!requested) {
            data.setHeat(player, state.heat() - 0.45);
        }

        ReflexFieldService.update(player, active, REFLEX_RATING, REFLEX_RADIUS);
        AugmentedMobilityService.tick(player, active);

        if (player.tickCount % 4 == 0) TitanbreakNetwork.sync(player);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        ReflexFieldService.clearAll();
    }
}
