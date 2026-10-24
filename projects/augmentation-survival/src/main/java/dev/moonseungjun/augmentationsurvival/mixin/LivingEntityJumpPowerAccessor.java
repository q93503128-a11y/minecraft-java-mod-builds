package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow accessor for the vanilla jump-power calculation used by server-authoritative air jumps. */
@Mixin(LivingEntity.class)
public interface LivingEntityJumpPowerAccessor {
    @Invoker("getJumpPower")
    float augmentationSurvival$invokeGetJumpPower();
}
