package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import dev.moonseungjun.augmentationsurvival.augment.WallSlamMeleeRuntime;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the legacy exact-knockback Wall Slam tick with the forgiving authoritative melee bridge. */
@Mixin(value = FinalAugmentRuntimeService.class, remap = false)
abstract class WallSlamRuntimeOverrideMixin {
    @Shadow @Final private static Map<UUID, Object> WALL_SLAMS;

    @Inject(method = "tickWallSlams", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$tickCanonicalWallSlams(MinecraftServer server, CallbackInfo ci) {
        // Legacy AFTER_DAMAGE windows are no longer authoritative; keeping them would permit a
        // second impact after the real-melee bridge has already resolved the same attack.
        WALL_SLAMS.clear();
        WallSlamMeleeRuntime.tick(server);
        ci.cancel();
    }
}
