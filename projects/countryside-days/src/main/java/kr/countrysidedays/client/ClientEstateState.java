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
    private static boolean restaurantOpen;
    private static int customersToday;
    private static int customerCap;
    private static int totalCustomers;
    private static int progressionStage;
    private static int pendingRanchProducts;

    private ClientEstateState() {
    }

    @SubscribeEvent
    private static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(EstateHudPayload.TYPE, (payload, context) -> {
            home = new BlockPos(payload.homeX(), payload.homeY(), payload.homeZ());
            restaurant = new BlockPos(
                    payload.restaurantX(), payload.restaurantY(), payload.restaurantZ()
            );
            restaurantOpen = payload.restaurantOpen();
            customersToday = payload.customersToday();
            customerCap = payload.customerCap();
            totalCustomers = payload.totalCustomers();
            progressionStage = payload.progressionStage();
            pendingRanchProducts = payload.pendingRanchProducts();
        });
    }

    public static BlockPos home() {
        return home;
    }

    public static BlockPos restaurant() {
        return restaurant;
    }

    public static boolean restaurantOpen() {
        return restaurantOpen;
    }

    public static int customersToday() {
        return customersToday;
    }

    public static int customerCap() {
        return customerCap;
    }

    public static int totalCustomers() {
        return totalCustomers;
    }

    public static int progressionStage() {
        return progressionStage;
    }

    public static int pendingRanchProducts() {
        return pendingRanchProducts;
    }
}
