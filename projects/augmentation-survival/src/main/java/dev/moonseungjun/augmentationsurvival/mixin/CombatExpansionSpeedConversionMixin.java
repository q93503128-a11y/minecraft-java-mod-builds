package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.AugmentMovementBonusService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps Speed Conversion on the augmentation-owned positive movement-speed axis only. */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class CombatExpansionSpeedConversionMixin {
    @Redirect(
            method = "modifyOutgoingDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D",
                    remap = true
            ),
            remap = false
    )
    private static double augmentationSurvival$useAugmentMovementBonus(
            AttributeInstance movement,
            LivingEntity target,
            DamageSource source,
            float damage
    ) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return movement.getValue();
        }
        double base = movement.getBaseValue();
        return base * (1.0D + AugmentMovementBonusService.positiveMovementBonus(attacker));
    }

    @Redirect(
            method = "modifyOutgoingDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;isSprinting()Z",
                    remap = true
            ),
            remap = false
    )
    private static boolean augmentationSurvival$excludeVanillaSprint(ServerPlayer player) {
        return false;
    }
}
