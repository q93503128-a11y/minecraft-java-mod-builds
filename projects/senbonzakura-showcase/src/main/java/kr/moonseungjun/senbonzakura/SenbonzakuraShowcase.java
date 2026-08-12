package kr.moonseungjun.senbonzakura;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.senbonzakura.bankai.BankaiService;
import kr.moonseungjun.senbonzakura.network.BankaiNetwork;
import kr.moonseungjun.senbonzakura.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(SenbonzakuraShowcase.MOD_ID)
public final class SenbonzakuraShowcase {
    public static final String MOD_ID = "senbonzakura";
    public static final String VERSION = "0.1.0-alpha.1";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SenbonzakuraShowcase(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        modEventBus.addListener(BankaiNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("Senbonzakura Bankai Showcase {} loaded", VERSION);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) BankaiService.tick(player);
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BankaiService.clear(player, true);
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BankaiService.clear(player, true);
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BankaiService.clear(player, true);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        BankaiService.clearAll();
    }
}
