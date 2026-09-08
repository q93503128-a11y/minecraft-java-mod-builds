package kr.moonseungjun.earthtostars.ship.networking;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipRuntimeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = EarthToStars.MOD_ID)
public final class ShipNetworking {
    private ShipNetworking() {
    }

    @SubscribeEvent
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                ShipControlInputPayload.TYPE,
                ShipControlInputPayload.STREAM_CODEC,
                (payload, context) -> ShipRuntimeManager.acceptControlInput(context.player(), payload)
        );
        registrar.playToClient(
                ShipControlSessionPayload.TYPE,
                ShipControlSessionPayload.STREAM_CODEC
        );
    }
}
