package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Keeps far-regional settlement streaming isolated from the already crowded central dispatcher. */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenRegionalSettlementEventBridge {
    private ErdenRegionalSettlementEventBridge() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        ErdenRegionalSettlementManager.onChunkLoad(event);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ErdenRegionalSettlementManager.onServerTick(event);
    }
}
