package kr.moonseungjun.livingkingdoms;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import kr.moonseungjun.livingkingdoms.network.LivingKingdomsNetwork;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.world.StarterNpcManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(LivingKingdoms.MOD_ID)
public final class LivingKingdoms {
    public static final String MOD_ID = "livingkingdoms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingKingdoms(IEventBus modEventBus) {
        FoundationCatalog.bootstrap();
        modEventBus.addListener(LivingKingdomsNetwork::register);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info(
                "Living Kingdoms foundation loaded: {} species, {} homelands, {} backgrounds, {} residences",
                FoundationCatalog.species().size(),
                FoundationCatalog.homelands().size(),
                FoundationCatalog.backgrounds().size(),
                FoundationCatalog.residences().size()
        );
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        OriginProfileManager.initialize(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OriginProfileManager.requestSelection(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && OriginProfileManager.requiresSelection(player.getUUID())
                && player.level().getGameTime() % 40L == 0L) {
            OriginProfileManager.requestSelection(player);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && OriginProfileManager.requiresSelection(player.getUUID())) {
            event.setAmount(0.0F);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        StarterNpcManager.handleInteraction(event);
    }
}
