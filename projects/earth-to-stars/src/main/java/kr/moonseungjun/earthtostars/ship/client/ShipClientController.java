package kr.moonseungjun.earthtostars.ship.client;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlInputPayload;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlSessionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import java.util.UUID;

@EventBusSubscriber(value = Dist.CLIENT, modid = EarthToStars.MOD_ID)
public final class ShipClientController {
    private static int controlledEntityId = -1;
    private static UUID sessionId;
    private static long sequence;

    private ShipClientController() {
    }

    @SubscribeEvent
    private static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(ShipControlSessionPayload.TYPE, (payload, context) -> handleSession(payload));
    }

    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        if (sessionId == null || controlledEntityId < 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clearSession();
            return;
        }

        float throttle = axis(minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown());
        float yaw = axis(minecraft.options.keyRight.isDown(), minecraft.options.keyLeft.isDown());
        float pitch = axis(minecraft.options.keyShift.isDown(), minecraft.options.keyJump.isDown());
        if (minecraft.screen != null) {
            throttle = 0.0F;
            yaw = 0.0F;
            pitch = 0.0F;
        }

        ClientPacketDistributor.sendToServer(new ShipControlInputPayload(
                controlledEntityId,
                sessionId.getMostSignificantBits(),
                sessionId.getLeastSignificantBits(),
                ++sequence,
                throttle,
                yaw,
                pitch
        ));
    }

    @SubscribeEvent
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clearSession();
    }

    private static void handleSession(ShipControlSessionPayload payload) {
        if (!payload.active()) {
            if (controlledEntityId == payload.entityId()) {
                clearSession();
            }
            return;
        }
        controlledEntityId = payload.entityId();
        sessionId = payload.sessionId();
        sequence = 0L;
    }

    private static float axis(boolean positive, boolean negative) {
        return (positive ? 1.0F : 0.0F) - (negative ? 1.0F : 0.0F);
    }

    private static void clearSession() {
        controlledEntityId = -1;
        sessionId = null;
        sequence = 0L;
    }
}
