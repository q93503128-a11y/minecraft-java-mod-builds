package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.utility.EnchantmentResonanceService;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Raises runtime enchantment levels without rewriting the item's stored enchantments. */
@Mixin(EnchantmentHelper.class)
abstract class EnchantmentResonanceMixin {
    @ModifyArgs(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;accept(Lnet/minecraft/core/Holder;I)V"
            )
    )
    private static void augmentationSurvival$raiseItemIterationLevel(Args args) {
        @SuppressWarnings("unchecked")
        Holder<Enchantment> enchantment = (Holder<Enchantment>) args.get(0);
        int level = args.get(1);
        args.set(1, EnchantmentResonanceService.effectiveLevel(enchantment, level));
    }

    @ModifyArgs(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"
            )
    )
    private static void augmentationSurvival$raiseEquipmentIterationLevel(
            Args args,
            ItemStack stack,
            EquipmentSlot slot,
            LivingEntity owner,
            EnchantmentHelper.EnchantmentInSlotVisitor visitor
    ) {
        @SuppressWarnings("unchecked")
        Holder<Enchantment> enchantment = (Holder<Enchantment>) args.get(0);
        int level = args.get(1);
        args.set(1, EnchantmentResonanceService.effectiveLevel(owner, enchantment, level));
    }

    @Inject(
            method = "getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void augmentationSurvival$raiseDirectEquipmentLevel(
            Holder<Enchantment> enchantment,
            LivingEntity owner,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(EnchantmentResonanceService.effectiveLevel(owner, enchantment, cir.getReturnValueI()));
    }

    @Inject(
            method = {
                    "modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
                    "modifyFallBasedDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
                    "modifyArmorEffectiveness(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
                    "modifyKnockback(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"
            },
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushCombatItemOwner(
            ServerLevel level,
            ItemStack stack,
            Entity target,
            DamageSource source,
            float value,
            CallbackInfoReturnable<Float> cir
    ) {
        EnchantmentResonanceService.pushRelevantActor(level, stack, source.getEntity());
    }

    @Inject(
            method = {
                    "modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
                    "modifyFallBasedDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
                    "modifyArmorEffectiveness(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
                    "modifyKnockback(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"
            },
            at = @At("RETURN")
    )
    private static void augmentationSurvival$popCombatItemOwner(
            ServerLevel level,
            ItemStack stack,
            Entity target,
            DamageSource source,
            float value,
            CallbackInfoReturnable<Float> cir
    ) {
        EnchantmentResonanceService.popActor();
    }

    @Inject(
            method = {
                    "processProjectileCount(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;I)I",
                    "getFishingLuckBonus(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)I",
                    "getTridentReturnToOwnerAcceleration(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)I"
            },
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushEntityItemInt(
            ServerLevel level,
            ItemStack stack,
            Entity entity,
            int baseValue,
            CallbackInfoReturnable<Integer> cir
    ) {
        EnchantmentResonanceService.pushRelevantActor(level, stack, entity);
    }

    @Inject(
            method = {
                    "processProjectileCount(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;I)I",
                    "getFishingLuckBonus(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)I",
                    "getTridentReturnToOwnerAcceleration(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)I"
            },
            at = @At("RETURN")
    )
    private static void augmentationSurvival$popEntityItemInt(
            ServerLevel level,
            ItemStack stack,
            Entity entity,
            int baseValue,
            CallbackInfoReturnable<Integer> cir
    ) {
        EnchantmentResonanceService.popActor();
    }

    @Inject(
            method = {
                    "processProjectileSpread(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;F)F",
                    "getFishingTimeReduction(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)F"
            },
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushEntityItemFloat(
            ServerLevel level,
            ItemStack stack,
            Entity entity,
            float baseValue,
            CallbackInfoReturnable<Float> cir
    ) {
        EnchantmentResonanceService.pushRelevantActor(level, stack, entity);
    }

    @Inject(
            method = {
                    "processProjectileSpread(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;F)F",
                    "getFishingTimeReduction(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;)F"
            },
            at = @At("RETURN")
    )
    private static void augmentationSurvival$popEntityItemFloat(
            ServerLevel level,
            ItemStack stack,
            Entity entity,
            float baseValue,
            CallbackInfoReturnable<Float> cir
    ) {
        EnchantmentResonanceService.popActor();
    }

    @Inject(
            method = "modifyCrossbowChargingTime(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushCrossbowOwner(
            ItemStack stack,
            LivingEntity owner,
            float value,
            CallbackInfoReturnable<Float> cir
    ) {
        EnchantmentResonanceService.pushActor(owner);
    }

    @Inject(
            method = "modifyCrossbowChargingTime(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At("RETURN")
    )
    private static void augmentationSurvival$popCrossbowOwner(
            ItemStack stack,
            LivingEntity owner,
            float value,
            CallbackInfoReturnable<Float> cir
    ) {
        EnchantmentResonanceService.popActor();
    }

    @Inject(
            method = "getTridentSpinAttackStrength(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)F",
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushTridentSpinOwner(
            ItemStack stack,
            LivingEntity owner,
            CallbackInfoReturnable<Float> cir
    ) {
        EnchantmentResonanceService.pushActor(owner);
    }

    @Inject(
            method = "getTridentSpinAttackStrength(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)F",
            at = @At("RETURN")
    )
    private static void augmentationSurvival$popTridentSpinOwner(
            ItemStack stack,
            LivingEntity owner,
            CallbackInfoReturnable<Float> cir
    ) {
        EnchantmentResonanceService.popActor();
    }

    @Inject(
            method = {
                    "processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I",
                    "processBlockExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I",
                    "modifyDurabilityToRepairFromXp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"
            },
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushSingleItemOwner(
            ServerLevel level,
            ItemStack stack,
            int value,
            CallbackInfoReturnable<Integer> cir
    ) {
        EnchantmentResonanceService.pushItemOwner(level, stack);
    }

    @Inject(
            method = {
                    "processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I",
                    "processBlockExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I",
                    "modifyDurabilityToRepairFromXp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"
            },
            at = @At("RETURN")
    )
    private static void augmentationSurvival$popSingleItemOwner(
            ServerLevel level,
            ItemStack stack,
            int value,
            CallbackInfoReturnable<Integer> cir
    ) {
        EnchantmentResonanceService.popActor();
    }

    @Inject(
            method = "processAmmoUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I",
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushAmmoWeaponOwner(
            ServerLevel level,
            ItemStack weapon,
            ItemStack ammo,
            int count,
            CallbackInfoReturnable<Integer> cir
    ) {
        EnchantmentResonanceService.pushItemOwner(level, weapon);
    }

    @Inject(
            method = "processAmmoUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I",
            at = @At("RETURN")
    )
    private static void augmentationSurvival$popAmmoWeaponOwner(
            ServerLevel level,
            ItemStack weapon,
            ItemStack ammo,
            int count,
            CallbackInfoReturnable<Integer> cir
    ) {
        EnchantmentResonanceService.popActor();
    }

    @Inject(
            method = "getPiercingCount(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushPiercingWeaponOwner(
            ServerLevel level,
            ItemStack weapon,
            ItemStack ammo,
            CallbackInfoReturnable<Integer> cir
    ) {
        EnchantmentResonanceService.pushItemOwner(level, weapon);
    }

    @Inject(
            method = "getPiercingCount(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("RETURN")
    )
    private static void augmentationSurvival$popPiercingWeaponOwner(
            ServerLevel level,
            ItemStack weapon,
            ItemStack ammo,
            CallbackInfoReturnable<Integer> cir
    ) {
        EnchantmentResonanceService.popActor();
    }

    @Inject(
            method = "onProjectileSpawned",
            at = @At("HEAD")
    )
    private static void augmentationSurvival$pushProjectileOwner(
            ServerLevel level,
            ItemStack weapon,
            Projectile projectile,
            java.util.function.Consumer<?> onBreak,
            CallbackInfo ci
    ) {
        EnchantmentResonanceService.pushRelevantActor(level, weapon, projectile);
    }

    @Inject(method = "onProjectileSpawned", at = @At("RETURN"))
    private static void augmentationSurvival$popProjectileOwner(
            ServerLevel level,
            ItemStack weapon,
            Projectile projectile,
            java.util.function.Consumer<?> onBreak,
            CallbackInfo ci
    ) {
        EnchantmentResonanceService.popActor();
    }
}
