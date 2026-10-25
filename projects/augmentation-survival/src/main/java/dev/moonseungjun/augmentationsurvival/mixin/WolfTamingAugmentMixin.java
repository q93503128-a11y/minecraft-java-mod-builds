package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.TamingAugmentService;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Wolf.class)
abstract class WolfTamingAugmentMixin {
    @Redirect(
            method = "tryToTame",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I")
    )
    private int augmentationSurvival$boostTamingRoll(RandomSource random, int bound, Player player) {
        return TamingAugmentService.boostZeroRoll(player, random, bound);
    }
}
