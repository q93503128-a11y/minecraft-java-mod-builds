package kr.moonseungjun.earthtostars.ship.client;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlInputPayload;
import kr.moonseungjun.earthtostars.ship.networking.ShipControlSessionPayload;
import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipExteriorEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import java.util.UUID;

@EventBusSubscriber(value = Dist.CLIENT, modid = EarthToStars.MOD_ID)
public final class ShipClientController {
    private static final float THIRD_PERSON_SHIP_DISTANCE = 8.5F;
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
        if (minecraft.player.getVehicle() == null || minecraft.player.getVehicle().getId() != controlledEntityId) {
            clearSession();
            return;
        }

        float throttle = axis(minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown());
        // Positive yaw means left turn in the server simulation.
        float yaw = axis(minecraft.options.keyLeft.isDown(), minecraft.options.keyRight.isDown());
        // Jump is always upward thrust; Sprint (Ctrl by default) is downward thrust. Shift remains dismount.
        float lift = axis(minecraft.options.keyJump.isDown(), minecraft.options.keySprint.isDown());
        if (!minecraft.mouseHandler.isMouseGrabbed()) {
            throttle = 0.0F;
            yaw = 0.0F;
            lift = 0.0F;
        }

        ClientPacketDistributor.sendToServer(new ShipControlInputPayload(
                controlledEntityId,
                sessionId.getMostSignificantBits(),
                sessionId.getLeastSignificantBits(),
                ++sequence,
                throttle,
                yaw,
                lift
        ));
    }

    @SubscribeEvent
    private static void onDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        var cameraEntity = event.getCamera().getEntity();
        if (cameraEntity != null && cameraEntity.getVehicle() instanceof ShipExteriorEntity) {
            event.setDistance(Math.max(event.getDistance(), THIRD_PERSON_SHIP_DISTANCE));
        }
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
