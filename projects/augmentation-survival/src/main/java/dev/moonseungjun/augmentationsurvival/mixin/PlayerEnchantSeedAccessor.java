package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface PlayerEnchantSeedAccessor {
    @Accessor("enchantmentSeed")
    void augmentationSurvival$setEnchantmentSeed(int seed);
}
