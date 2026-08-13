package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Keeps the drainage vertical slice isolated from the already crowded central server-tick dispatcher. */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenDrainageEventBridge {
    private ErdenDrainageEventBridge() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ErdenDrainageSimulationManager.onServerTick(event);
    }
}
