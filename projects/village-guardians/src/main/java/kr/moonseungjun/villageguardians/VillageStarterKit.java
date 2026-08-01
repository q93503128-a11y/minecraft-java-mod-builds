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
    private static final String CALLER_TAG = "villageguardians_public_caller_v3";
    private static final String CALLER_NAME = "마을 수호단 호출기";

    private VillageStarterKit() {
    }

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
        boolean firstGrant = player.addTag(CALLER_TAG);
        if (!firstGrant && hasCaller(player)) {
            return;
        }
        giveOrDrop(player, namedCaller());
        player.sendSystemMessage(Component.literal(
                "§6[마을 장비] §f호출기를 들고 우클릭하면 빠른 신호 창이 열립니다."));
    }

    public static void grantMayorCaller(ServerPlayer player) {
        grantCaller(player);
    }

    public static void handleItemInteraction(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (!isCaller(stack)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageUiService.openQuickChat(player);
    }

    private static ItemStack namedCaller() {
        ItemStack stack = Items.GOAT_HORN.getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(CALLER_NAME).withStyle(ChatFormatting.GOLD));
        return stack;
    }

    private static boolean hasCaller(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isCaller(player.getInventory().getItem(slot))) {
                return true;
            }
        }
        return isCaller(player.getOffhandItem());
    }

    private static boolean isCaller(ItemStack stack) {
        if (stack.getItem() != Items.GOAT_HORN) {
            return false;
        }
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        return customName != null
                && CALLER_NAME.equals(ChatFormatting.stripFormatting(customName.getString()));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
