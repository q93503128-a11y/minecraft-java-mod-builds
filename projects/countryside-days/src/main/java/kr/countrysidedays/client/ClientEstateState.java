package kr.countrysidedays.client;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.network.EstateHudPayload;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = CountrysideDays.MOD_ID, value = Dist.CLIENT)
public final class ClientEstateState {
    private static BlockPos home;
    private static BlockPos restaurant;

    private ClientEstateState() {
    }

    @SubscribeEvent
    private static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(EstateHudPayload.TYPE, (payload, context) -> {
            home = new BlockPos(payload.homeX(), payload.homeY(), payload.homeZ());
            restaurant = new BlockPos(
                    payload.restaurantX(), payload.restaurantY(), payload.restaurantZ()
            );
        });
    }

    public static BlockPos home() {
        return home;
    }

    public static BlockPos restaurant() {
        return restaurant;
    }
}
