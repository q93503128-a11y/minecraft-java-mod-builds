package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.EnchantmentResonanceService;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityEnchantmentResonanceMixin {
    @Inject(method = "collectEquipmentChanges", at = @At("HEAD"))
    private void augmentationSurvival$pushEquipmentOwner(
            Map<EquipmentSlot, ItemStack> previous,
            CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir
    ) {
        EnchantmentResonanceService.pushActor((LivingEntity) (Object) this);
    }

    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void augmentationSurvival$popEquipmentOwner(
            Map<EquipmentSlot, ItemStack> previous,
            CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir
    ) {
        EnchantmentResonanceService.popActor();
    }
}
