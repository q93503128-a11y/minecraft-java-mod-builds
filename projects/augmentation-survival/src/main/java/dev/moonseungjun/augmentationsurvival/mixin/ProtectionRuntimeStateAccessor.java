package dev.moonseungjun.augmentationsurvival.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only accessor for ProtectionAugmentService.RuntimeState. */
@Mixin(targets = "dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService$RuntimeState")
public interface ProtectionRuntimeStateAccessor {
    @Accessor("pools")
    Map<String, Object> augmentationSurvival$getPools();
}
