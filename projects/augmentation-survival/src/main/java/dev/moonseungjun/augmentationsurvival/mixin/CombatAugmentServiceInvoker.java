package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow bridge for the canonical pre-totem Last Stand ordering. */
@Mixin(value = CombatAugmentService.class, remap = false)
public interface CombatAugmentServiceInvoker {
    @Invoker("allowDeath")
    static boolean augmentationSurvival$allowDeath(
            LivingEntity entity,
            DamageSource source,
            float amount
    ) {
        throw new AssertionError();
    }
}
