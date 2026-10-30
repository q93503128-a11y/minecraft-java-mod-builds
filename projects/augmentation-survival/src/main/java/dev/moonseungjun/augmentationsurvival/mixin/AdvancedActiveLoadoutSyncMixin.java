package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActivePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveTuning;
import dev.moonseungjun.augmentationsurvival.network.ActiveAbilityNetworking;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Time Acceleration charge state and its real sustained timer aligned with the client HUD. */
@Mixin(AdvancedActiveAugmentService.class)
abstract class AdvancedActiveLoadoutSyncMixin {
    private static final String TIME_ACCELERATION_ID =
            AugmentationSurvival.MOD_ID + ":time_acceleration";
    private static final Map<UUID, PresentationWindow> TIME_WINDOWS = new HashMap<>();

    @Inject(method = "syncTemporalState", at = @At("TAIL"))
    private static void augmentationSurvival$syncActiveLoadoutAfterTemporalState(
            ServerPlayer player,
            boolean active,
            CallbackInfo ci
    ) {
        if (active) {
            int duration = AdvancedActiveTuning.current().timeAccelerationDurationTicks();
            long now = player.level().getGameTime();
            long end = now + duration;
            long maxEnd = end + Math.round(
                    duration * AdvancedActiveTuning.current().overtimeMaximumExtensionFraction());
            TIME_WINDOWS.put(player.getUUID(), new PresentationWindow(end, maxEnd));
            ActivePresentationService.notifyState(
                    player,
                    TIME_ACCELERATION_ID,
                    ActivePresentationPayload.SUSTAINED,
                    duration,
                    0.0F
            );
        } else {
            TIME_WINDOWS.remove(player.getUUID());
            ActivePresentationService.notifyState(
                    player,
                    TIME_ACCELERATION_ID,
                    ActivePresentationPayload.END,
                    0,
                    0.0F
            );
        }
        ActiveAbilityNetworking.syncNow(player);
    }

    @Inject(method = "extendSustainedActives", at = @At("TAIL"))
    private static void augmentationSurvival$syncOvertimeExtension(
            ServerPlayer player,
            int ticks,
            CallbackInfo ci
    ) {
        if (ticks <= 0 || !AdvancedActiveAugmentService.isTimeAccelerationActive(player)) return;
        PresentationWindow window = TIME_WINDOWS.get(player.getUUID());
        if (window == null) return;
        long now = player.level().getGameTime();
        if (window.endTick <= now) return;
        window.endTick = Math.min(window.maxEndTick, window.endTick + ticks);
        int remaining = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, window.endTick - now));
        ActivePresentationService.notifyState(
                player,
                TIME_ACCELERATION_ID,
                ActivePresentationPayload.SUSTAINED,
                remaining,
                0.0F
        );
    }

    private static final class PresentationWindow {
        private long endTick;
        private final long maxEndTick;

        private PresentationWindow(long endTick, long maxEndTick) {
            this.endTick = endTick;
            this.maxEndTick = maxEndTick;
        }
    }
}
