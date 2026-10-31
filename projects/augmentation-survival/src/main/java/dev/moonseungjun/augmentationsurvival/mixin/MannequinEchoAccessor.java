package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow presentation bridge for short-lived Echo Strike player doubles. */
@Mixin(Mannequin.class)
public interface MannequinEchoAccessor {
    @Invoker("setProfile")
    void augmentationSurvival$setProfile(ResolvableProfile profile);

    @Invoker("setImmovable")
    void augmentationSurvival$setImmovable(boolean immovable);

    @Invoker("setHideDescription")
    void augmentationSurvival$setHideDescription(boolean hideDescription);
}
