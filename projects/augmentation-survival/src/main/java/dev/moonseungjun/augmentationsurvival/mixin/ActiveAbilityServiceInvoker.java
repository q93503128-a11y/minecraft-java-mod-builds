package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exact-id bridge so the 10-slot router never reinterprets a reordered legacy active as another slot. */
@Mixin(value = ActiveAbilityService.class, remap = false)
public interface ActiveAbilityServiceInvoker {
    @Invoker("tryUse")
    static boolean augmentationSurvival$tryUse(
            ServerPlayer player,
            AugmentationPlayerState state,
            String abilityId
    ) {
        throw new AssertionError();
    }
}
