package kr.moonseungjun.turnboundre.client.camera;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.camera.external.FreeCameraRotationSmoother;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Battle-only camera bridge.
 *
 * <p>No TURNBOUND-authored cinematic angle or motion profile is invented here. While a server-authored battle
 * snapshot is active, normal Minecraft camera yaw/pitch becomes the target of the externally derived Free Camera
 * cinematic smoother. When the battle disappears, the bridge clears immediately and vanilla camera angles pass
 * through untouched.</p>
 */
@EventBusSubscriber(modid = TurnboundRe.MOD_ID, value = Dist.CLIENT)
public final class BattleCameraHooks {
    private static final FreeCameraRotationSmoother ROTATION = new FreeCameraRotationSmoother();
    private static boolean activeLastFrame;

    private BattleCameraHooks() {}

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        boolean active = BattleClientState.presentation().isPresent();
        if (!active) {
            if (activeLastFrame) ROTATION.clear();
            activeLastFrame = false;
            return;
        }

        activeLastFrame = true;
        ROTATION.setTarget(event.getYaw(), event.getPitch());
        double elapsedSeconds = ROTATION.elapsedSeconds(System.nanoTime());
        if (!ROTATION.advance(elapsedSeconds)) return;

        event.setYaw(ROTATION.outputYaw());
        event.setPitch(ROTATION.outputPitch());
    }
}
