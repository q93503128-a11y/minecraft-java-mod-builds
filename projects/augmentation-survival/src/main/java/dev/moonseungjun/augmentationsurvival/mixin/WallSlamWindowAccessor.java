package dev.moonseungjun.augmentationsurvival.mixin;

import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService$WallSlamWindow", remap = false)
public interface WallSlamWindowAccessor {
    @Accessor("target") LivingEntity augmentationSurvival$getTarget();
    @Accessor("ownerId") UUID augmentationSurvival$getOwnerId();
    @Accessor("level") int augmentationSurvival$getLevel();
    @Accessor("referenceDamage") float augmentationSurvival$getReferenceDamage();
    @Accessor("expiresAtTick") long augmentationSurvival$getExpiresAtTick();
}
