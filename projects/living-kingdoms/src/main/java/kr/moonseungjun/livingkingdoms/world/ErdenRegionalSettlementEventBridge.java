package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Keeps far-regional settlement, road, society, economy, government, security and freight streaming isolated from the central dispatcher. */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenRegionalSettlementEventBridge {
    private ErdenRegionalSettlementEventBridge() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        ErdenRegionalSettlementManager.onChunkLoad(event);
        ErdenRegionalRoadManager.onChunkLoad(event);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ErdenRegionalRoadManager.onServerTick(event);
        ErdenRegionalSettlementManager.onServerTick(event);
        ErdenRegionalSocietyManager.onServerTick(event);
        ErdenRegionalShipmentClockGuard.onServerTick(event);
        ErdenRegionalEconomyManager.onServerTick(event);
        ErdenRegionalGovernanceManager.onServerTick(event);
        ErdenRegionalRoadSecurityManager.onServerTick(event);
        ErdenRegionalTransportManager.onServerTick(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ErdenRegionalGovernanceManager.handleOfficialInteraction(event);
        ErdenRegionalSocietyManager.handleInteraction(event);
    }

    @SubscribeEvent
    public static void onMarketInteract(PlayerInteractEvent.RightClickBlock event) {
        ErdenRegionalGovernanceManager.handleLedgerInteraction(event);
        ErdenRegionalEconomyManager.handleInteraction(event);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Villager villager
                && villager.level() instanceof ServerLevel level) {
            ErdenRegionalGovernanceManager.markDeadIfGuard(level, villager);
            ErdenRegionalSocietyManager.markDeadIfResident(level, villager);
        }
    }
}
