package kr.moonseungjun.villageguardians;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class VillageStarterKit {
    private static final String STARTER_KIT_TAG = "villageguardians_starter_kit_v3";
    private static final String CALLER_TAG = "villageguardians_public_caller_v3";

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
        if (!player.addTag(CALLER_TAG)) {
            return;
        }
        giveOrDrop(player, named(Items.GOAT_HORN.getDefaultInstance(), "§6마을 수호단 호출기"));
        player.sendSystemMessage(Component.literal(
                "§6[마을 장비] §f호출기는 모든 플레이어가 현황·빠른 신호·투표에 사용할 수 있습니다."));
    }

    public static void grantMayorCaller(ServerPlayer player) {
        grantCaller(player);
    }

    public static void handleItemInteraction(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (stack.getItem() == Items.GOAT_HORN) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            VillageUiService.openDashboard(player);
        }
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
