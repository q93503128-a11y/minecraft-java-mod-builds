package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.RewindSafetyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Routes Rewind's historical teleport through collision-safe destination reconciliation. */
@Mixin(AdvancedActiveAugmentService.class)
abstract class RewindSafeDestinationMixin {
    @Redirect(
            method = "tryRewind",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;"
            )
    )
    private static ServerPlayer augmentationSurvival$safeRewindTeleport(
            ServerPlayer player,
            TeleportTransition transition
    ) {
        return player.teleport(RewindSafetyService.adjust(player, transition));
    }
}
