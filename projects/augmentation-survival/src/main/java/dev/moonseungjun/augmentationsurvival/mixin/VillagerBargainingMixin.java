package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Bargaining after vanilla has rebuilt reputation/demand price adjustments. */
@Mixin(Villager.class)
public abstract class VillagerBargainingMixin {
    private static final String BARGAINING_ID = AugmentationSurvival.MOD_ID + ":bargaining";

    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void augmentationSurvival$applyBargaining(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(serverPlayer);
        int level = state.level(BARGAINING_ID);
        if (level <= 0) {
            return;
        }
        level = Math.min(level, AugmentCatalog.require(BARGAINING_ID).maxLevel());
        double discount = AdvancedActiveTuning.current().bargainingDiscountFraction(level);
        Villager self = (Villager) (Object) this;
        for (MerchantOffer offer : self.getOffers()) {
            if (!offer.getCostA().is(Items.EMERALD)) {
                continue;
            }
            int current = offer.getCostA().getCount();
            if (current <= 1) {
                continue;
            }
            int target = Math.max(1, (int) Math.floor(current * (1.0D - discount)));
            if (target < current) {
                offer.addToSpecialPriceDiff(target - current);
            }
        }
    }
}
