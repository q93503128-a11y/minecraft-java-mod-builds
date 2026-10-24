package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAugmentAccessor {
    @Invoker("setPierceLevel")
    void augmentationSurvival$invokeSetPierceLevel(byte level);

    @Invoker("resetPiercedEntities")
    void augmentationSurvival$invokeResetPiercedEntities();

    @Invoker("hitBlockEnchantmentEffects")
    void augmentationSurvival$invokeHitBlockEnchantmentEffects(
            ServerLevel level,
            BlockHitResult hitResult,
            ItemStack weapon
    );

    @Invoker("setInGround")
    void augmentationSurvival$invokeSetInGround(boolean inGround);
}
