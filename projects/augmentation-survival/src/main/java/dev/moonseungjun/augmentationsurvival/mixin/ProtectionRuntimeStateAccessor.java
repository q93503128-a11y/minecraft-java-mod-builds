package dev.moonseungjun.augmentationsurvival.mixin;

import java.util.LinkedHashMap;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor for ProtectionAugmentService.RuntimeState player-owned protection queues. */
@Mixin(targets = "dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService$RuntimeState")
public interface ProtectionRuntimeStateAccessor {
    @Accessor("pools")
    LinkedHashMap<String, Object> augmentationSurvival$getPools();

    @Accessor("delayed")
    List<Object> augmentationSurvival$getDelayed();
}
