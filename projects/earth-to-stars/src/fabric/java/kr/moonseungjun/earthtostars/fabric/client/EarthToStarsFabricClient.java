package kr.moonseungjun.earthtostars.fabric.client;

import kr.moonseungjun.earthtostars.fabric.content.EarthToStarsFabricEntities;
import kr.moonseungjun.earthtostars.fabric.entity.LaunchCraftEntity;
import kr.moonseungjun.earthtostars.fabric.networking.ShipControlInputPayload;
import kr.moonseungjun.earthtostars.fabric.networking.ShipControlSessionPayload;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;

import java.util.UUID;

public final class EarthToStarsFabricClient implements ClientModInitializer {
    private static ShipId controlledShipId;
    private static UUID sessionId;
    private static long sequence;

    @Override
    public void onInitializeClient() {
        EntityRenderers.register(EarthToStarsFabricEntities.LAUNCH_CRAFT, LaunchCraftEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(
                ShipControlSessionPayload.TYPE,
                (payload, context) -> handleSession(payload)
        );
        ClientTickEvents.END_CLIENT_TICK.register(EarthToStarsFabricClient::tickControl);
    }

    private static void tickControl(Minecraft minecraft) {
        if (controlledShipId == null || sessionId == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof LaunchCraftEntity craft)
                || craft.shipId().filter(controlledShipId::equals).isEmpty()) {
            clearSession();
            return;
        }

        float throttle = axis(minecraft.options.keyUp.isDown(), minecraft.options.keyDown.isDown());
        float yaw = axis(minecraft.options.keyLeft.isDown(), minecraft.options.keyRight.isDown());
        float lift = axis(minecraft.options.keyJump.isDown(), minecraft.options.keySprint.isDown());
        if (!minecraft.mouseHandler.isMouseGrabbed()) {
            throttle = 0.0F;
            yaw = 0.0F;
            lift = 0.0F;
        }

        UUID ship = controlledShipId.value();
        ClientPlayNetworking.send(new ShipControlInputPayload(
                ship.getMostSignificantBits(),
                ship.getLeastSignificantBits(),
                sessionId.getMostSignificantBits(),
                sessionId.getLeastSignificantBits(),
                ++sequence,
                throttle,
                yaw,
                lift
        ));
    }

    private static void handleSession(ShipControlSessionPayload payload) {
        if (!payload.active()) {
            if (controlledShipId != null && controlledShipId.equals(payload.shipId())) {
                clearSession();
            }
            return;
        }
        controlledShipId = payload.shipId();
        sessionId = payload.sessionId();
        sequence = 0L;
    }

    private static float axis(boolean positive, boolean negative) {
        return (positive ? 1.0F : 0.0F) - (negative ? 1.0F : 0.0F);
    }

    private static void clearSession() {
        controlledShipId = null;
        sessionId = null;
        sequence = 0L;
    }
}
