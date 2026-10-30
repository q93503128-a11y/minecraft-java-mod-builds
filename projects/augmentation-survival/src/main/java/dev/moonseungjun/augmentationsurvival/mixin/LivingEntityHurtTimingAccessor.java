package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses ordinary hurt-animation timing so delayed installments can restore the pre-installment visual state. */
@Mixin(LivingEntity.class)
public interface LivingEntityHurtTimingAccessor {
    @Accessor("hurtTime")
    int augmentationSurvival$getHurtTime();

    @Accessor("hurtTime")
    void augmentationSurvival$setHurtTime(int hurtTime);

    @Accessor("hurtDuration")
    int augmentationSurvival$getHurtDuration();

    @Accessor("hurtDuration")
    void augmentationSurvival$setHurtDuration(int hurtDuration);
}
