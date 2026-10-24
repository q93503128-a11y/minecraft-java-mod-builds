package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forbidden Magic relaxes anvil exclusivity and prior-work/Too-Expensive restrictions only. */
@Mixin(AnvilMenu.class)
abstract class AnvilForbiddenMagicMixin {
    @Shadow @Final private DataSlot cost;

    @Redirect(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z"
            )
    )
    private boolean augmentationSurvival$relaxExclusiveEnchantments(
            Holder<Enchantment> first,
            Holder<Enchantment> second
    ) {
        Player player = ((ItemCombinerMenuAccessor) this).augmentationSurvival$getPlayer();
        return AdvancedWorldAugmentService.hasForbiddenMagic(player)
                || Enchantment.areCompatible(first, second);
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
    private int augmentationSurvival$removeTooExpensiveResultCap(int vanillaCap) {
        Player player = ((ItemCombinerMenuAccessor) this).augmentationSurvival$getPlayer();
        return AdvancedWorldAugmentService.hasForbiddenMagic(player) ? Integer.MAX_VALUE : vanillaCap;
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void augmentationSurvival$removePriorWorkPenalty(CallbackInfo ci) {
        ItemCombinerMenuAccessor accessor = (ItemCombinerMenuAccessor) this;
        Player player = accessor.augmentationSurvival$getPlayer();
        if (!AdvancedWorldAugmentService.hasForbiddenMagic(player)) {
            return;
        }
        Container inputs = accessor.augmentationSurvival$getInputSlots();
        ResultContainer result = accessor.augmentationSurvival$getResultSlots();
        ItemStack output = result.getItem(0);
        if (output.isEmpty() || cost.get() <= 0) {
            return;
        }
        int firstPrior = inputs.getItem(0).getOrDefault(DataComponents.REPAIR_COST, 0);
        int secondPrior = inputs.getItem(1).getOrDefault(DataComponents.REPAIR_COST, 0);
        long prior = Math.max(0L, (long) firstPrior) + Math.max(0L, (long) secondPrior);
        int adjusted = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, (long) cost.get() - prior));
        cost.set(adjusted);
        output.remove(DataComponents.REPAIR_COST);
        result.setItem(0, output);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$allowHighCostPickup(
            Player player,
            boolean present,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!AdvancedWorldAugmentService.hasForbiddenMagic(player)) {
            return;
        }
        int levelCost = cost.get();
        cir.setReturnValue(
                present
                        && levelCost > 0
                        && (player.getAbilities().instabuild || player.experienceLevel >= levelCost)
        );
    }
}
