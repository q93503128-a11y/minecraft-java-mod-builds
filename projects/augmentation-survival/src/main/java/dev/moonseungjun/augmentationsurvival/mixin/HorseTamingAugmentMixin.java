package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.TamingAugmentService;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractHorse.class)
abstract class HorseTamingAugmentMixin {
    @Redirect(
            method = "tickRidden",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;getTemper()I"
            )
    )
    private int augmentationSurvival$boostTemperThreshold(
            AbstractHorse horse,
            Player controller,
            Vec3 riddenInput
    ) {
        int original = horse.getTemper();
        return TamingAugmentService.boostThreshold(
                controller,
                horse.getRandom(),
                original,
                horse.getMaxTemper()
        );
    }
}
