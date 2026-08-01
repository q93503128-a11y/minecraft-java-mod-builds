package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class VillageStarterKit {
    private static final String STARTER_KIT_TAG = "villageguardians_starter_kit_v3";
    private static final String CALLER_TAG = "villageguardians_public_caller_v4";
    private static final String CALLER_NAME = "마을 수호단 호출기";

    private VillageStarterKit() {}

    public static void grantOnLogin(ServerPlayer player) {
        if (player.addTag(STARTER_KIT_TAG)) {
            giveOrDrop(player, Items.IRON_SWORD.getDefaultInstance());
            giveOrDrop(player, Items.SHIELD.getDefaultInstance());
            giveOrDrop(player, Items.BOW.getDefaultInstance());
            ItemStack arrows = Items.ARROW.getDefaultInstance();
            arrows.setCount(64);
            giveOrDrop(player, arrows);
            player.sendSystemMessage(Component.literal(
                    "§a[지급 완료] §f기본 전투 장비와 첫 화살 64개가 지급되었습니다."));
        }
        VillageProgressionSystem.registerPlayer(player);
        grantCaller(player);
        VillageProgressionSystem.grantDailyBreadOnLogin(player);
    }

    public static void grantCaller(ServerPlayer player) {
        boolean migrated = migrateLegacyCaller(player);
        boolean firstGrant = player.addTag(CALLER_TAG);
        if (hasModernCaller(player)) {
            if (migrated) {
                player.sendSystemMessage(Component.literal(
                        "§6[호출기 갱신] §f낡은 뿔 호출기를 원래 형태의 휴대용 호출기로 교체했습니다."));
            }
            return;
        }
        giveOrDrop(player, namedCaller());
        if (firstGrant || migrated) {
            player.sendSystemMessage(Component.literal(
                    "§6[마을 장비] §f호출기를 우클릭하면 상태·빠른 신호·귀환 메뉴가 열립니다."));
        }
    }

    public static void grantMayorCaller(ServerPlayer player) {
        grantCaller(player);
    }

    public static void handleItemInteraction(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!isCaller(stack)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageUiService.openCallerMenu(player);
    }

    private static ItemStack namedCaller() {
        ItemStack stack = Items.CLOCK.getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(CALLER_NAME).withStyle(ChatFormatting.GOLD));
        return stack;
    }

    private static boolean migrateLegacyCaller(ServerPlayer player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isLegacyCaller(stack)) {
                ItemStack replacement = namedCaller();
                replacement.setCount(stack.getCount());
                player.getInventory().setItem(slot, replacement);
                changed = true;
            }
        }
        if (isLegacyCaller(player.getOffhandItem())) {
            ItemStack replacement = namedCaller();
            replacement.setCount(player.getOffhandItem().getCount());
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, replacement);
            changed = true;
        }
        if (changed) {
            player.getInventory().setChanged();
        }
        return changed;
    }

    private static boolean hasModernCaller(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isModernCaller(player.getInventory().getItem(slot))) return true;
        }
        return isModernCaller(player.getOffhandItem());
    }

    private static boolean isCaller(ItemStack stack) {
        return isModernCaller(stack) || isLegacyCaller(stack);
    }

    private static boolean isModernCaller(ItemStack stack) {
        return stack.getItem() == Items.CLOCK && hasCallerName(stack);
    }

    private static boolean isLegacyCaller(ItemStack stack) {
        return stack.getItem() == Items.GOAT_HORN && hasCallerName(stack);
    }

    private static boolean hasCallerName(ItemStack stack) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        return customName != null
                && CALLER_NAME.equals(ChatFormatting.stripFormatting(customName.getString()));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
}
