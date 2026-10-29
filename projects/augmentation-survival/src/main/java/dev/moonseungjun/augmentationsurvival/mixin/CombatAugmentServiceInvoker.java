package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow bridge for canonical combat protection ordering used by mixin-owned boundaries. */
@Mixin(value = CombatAugmentService.class, remap = false)
public interface CombatAugmentServiceInvoker {
    @Invoker("allowDamage")
    static boolean augmentationSurvival$allowDamage(
            LivingEntity entity,
            DamageSource source,
            float amount
    ) {
        throw new AssertionError();
    }

    @Invoker("allowDeath")
    static boolean augmentationSurvival$allowDeath(
            LivingEntity entity,
            DamageSource source,
            float amount
    ) {
        throw new AssertionError();
    }
}
