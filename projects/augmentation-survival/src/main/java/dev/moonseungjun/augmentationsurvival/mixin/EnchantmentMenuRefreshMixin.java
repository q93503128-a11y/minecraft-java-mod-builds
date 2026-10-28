package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server-authoritative fixed-cost refresh for the three enchantment-table offers. */
@Mixin(EnchantmentMenu.class)
abstract class EnchantmentMenuRefreshMixin {
    private static final String AUGMENTATION_SURVIVAL$ENCHANT_REFRESH_ID =
            AugmentationSurvival.MOD_ID + ":enchant_refresh";
    private static final int AUGMENTATION_SURVIVAL$REFRESH_BUTTON_ID = 3;
    private static final int AUGMENTATION_SURVIVAL$XP_COST = 5;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$refreshOffers(
            Player player,
            int buttonId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (buttonId != AUGMENTATION_SURVIVAL$REFRESH_BUTTON_ID) return;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            cir.setReturnValue(false);
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(serverPlayer);
        int ownedLevel = Math.min(
                state.level(AUGMENTATION_SURVIVAL$ENCHANT_REFRESH_ID),
                AugmentCatalog.require(AUGMENTATION_SURVIVAL$ENCHANT_REFRESH_ID).maxLevel()
        );
        if (ownedLevel <= 0) {
            cir.setReturnValue(false);
            return;
        }

        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        EnchantmentMenuAugmentAccessor accessor = (EnchantmentMenuAugmentAccessor) menu;
        Container slots = accessor.augmentationSurvival$getEnchantSlots();
        ItemStack item = slots.getItem(0);
        ItemStack lapis = slots.getItem(1);
        if (item.isEmpty()) {
            augmentationSurvival$feedback(serverPlayer, "새로고침할 장비를 마법부여대에 올려주세요.");
            cir.setReturnValue(false);
            return;
        }
        if (lapis.isEmpty() || lapis.getCount() < 1) {
            augmentationSurvival$feedback(serverPlayer, "새로고침에는 청금석 1개가 필요합니다.");
            cir.setReturnValue(false);
            return;
        }
        if (serverPlayer.totalExperience < AUGMENTATION_SURVIVAL$XP_COST) {
            augmentationSurvival$feedback(serverPlayer, "새로고침에는 경험치 5가 필요합니다.");
            cir.setReturnValue(false);
            return;
        }

        boolean hasOffer = false;
        for (int cost : menu.costs) {
            if (cost > 0) {
                hasOffer = true;
                break;
            }
        }
        if (!hasOffer) {
            augmentationSurvival$feedback(serverPlayer, "지금은 새로고침할 마법 후보가 없습니다.");
            cir.setReturnValue(false);
            return;
        }

        lapis.shrink(1);
        slots.setChanged();
        serverPlayer.giveExperiencePoints(-AUGMENTATION_SURVIVAL$XP_COST);
        int previousSeed = accessor.augmentationSurvival$getEnchantmentSeedData().get();
        int seed = serverPlayer.getRandom().nextInt();
        if (seed == previousSeed) seed++;
        ((PlayerEnchantSeedAccessor) serverPlayer).augmentationSurvival$setEnchantmentSeed(seed);
        accessor.augmentationSurvival$getEnchantmentSeedData().set(seed);
        menu.slotsChanged(slots);
        menu.broadcastChanges();
        cir.setReturnValue(true);
    }

    private static void augmentationSurvival$feedback(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
