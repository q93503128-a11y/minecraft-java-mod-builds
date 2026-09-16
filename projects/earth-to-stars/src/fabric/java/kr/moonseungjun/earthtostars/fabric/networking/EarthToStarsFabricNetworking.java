package kr.moonseungjun.earthtostars.fabric.networking;

import kr.moonseungjun.earthtostars.fabric.ship.EarthToStarsFabricShipAuthority;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class EarthToStarsFabricNetworking {
    private EarthToStarsFabricNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ShipControlInputPayload.TYPE, ShipControlInputPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShipControlSessionPayload.TYPE, ShipControlSessionPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                ShipControlInputPayload.TYPE,
                (payload, context) -> EarthToStarsFabricShipAuthority.acceptControlInput(context.player(), payload)
        );
    }
}
