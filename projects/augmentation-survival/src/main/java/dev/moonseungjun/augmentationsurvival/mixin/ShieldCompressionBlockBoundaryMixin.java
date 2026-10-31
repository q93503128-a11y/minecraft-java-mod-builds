package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ShieldCompressionService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts only real blocking events that actually consume durability on the actively used shield/item. */
@Mixin(LivingEntity.class)
abstract class ShieldCompressionBlockBoundaryMixin {
    @Unique private Item augmentationSurvival$blockingItem;
    @Unique private int augmentationSurvival$blockingDamageBefore = -1;
    @Unique private int augmentationSurvival$blockingCountBefore = -1;

    @Inject(method = "applyItemBlocking", at = @At("HEAD"))
    private void augmentationSurvival$captureBlockingDurabilityBefore(
            ServerLevel level,
            DamageSource source,
            float incomingDamage,
            CallbackInfoReturnable<Float> cir
    ) {
        augmentationSurvival$resetBlockingCapture();
        if (!((Object) this instanceof ServerPlayer player)) return;
        ItemStack stack = player.getUseItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) return;
        augmentationSurvival$blockingItem = stack.getItem();
        augmentationSurvival$blockingDamageBefore = stack.getDamageValue();
        augmentationSurvival$blockingCountBefore = stack.getCount();
    }

    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void augmentationSurvival$countDurabilityConsumingBlock(
            ServerLevel level,
            DamageSource source,
            float incomingDamage,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!((Object) this instanceof ServerPlayer player)
                || augmentationSurvival$blockingItem == null
                || augmentationSurvival$blockingDamageBefore < 0) {
            augmentationSurvival$resetBlockingCapture();
            return;
        }

        ItemStack after = player.getUseItem();
        boolean damaged = !after.isEmpty()
                && after.getItem() == augmentationSurvival$blockingItem
                && after.getDamageValue() > augmentationSurvival$blockingDamageBefore;
        boolean broke = after.isEmpty() && augmentationSurvival$blockingCountBefore > 0;
        augmentationSurvival$resetBlockingCapture();

        if ((damaged || broke) && incomingDamage > 0.0F && Float.isFinite(incomingDamage)) {
            ShieldCompressionService.onDurabilityBlocking(player, source, incomingDamage);
        }
    }

    @Unique
    private void augmentationSurvival$resetBlockingCapture() {
        augmentationSurvival$blockingItem = null;
        augmentationSurvival$blockingDamageBefore = -1;
        augmentationSurvival$blockingCountBefore = -1;
    }
}
