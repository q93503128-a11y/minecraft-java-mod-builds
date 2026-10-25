package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.TamingAugmentService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RunAroundLikeCrazyGoal.class)
abstract class HorseTamingAugmentMixin {
    @Shadow @Final private AbstractHorse horse;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;getTemper()I"
            )
    )
    private int augmentationSurvival$boostTemperThreshold(AbstractHorse target) {
        int original = target.getTemper();
        Entity passenger = target.getFirstPassenger();
        if (!(passenger instanceof Player player)) {
            return original;
        }
        return TamingAugmentService.boostThreshold(
                player,
                target.getRandom(),
                original,
                target.getMaxTemper()
        );
    }
}
