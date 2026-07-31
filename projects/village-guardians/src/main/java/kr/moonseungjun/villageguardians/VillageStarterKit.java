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
    private static final String STARTER_KIT_TAG = "villageguardians_starter_kit_v2";
    private static final String MAYOR_CALLER_TAG = "villageguardians_mayor_caller_v2";

    private VillageStarterKit() {
    }

    public static void grantOnLogin(ServerPlayer player) {
        if (player.addTag(STARTER_KIT_TAG)) {
            giveOrDrop(player, named(Items.KNOWLEDGE_BOOK.getDefaultInstance(), "§6마을 수호대 작전 설명서"));
            giveOrDrop(player, Items.IRON_SWORD.getDefaultInstance());
            giveOrDrop(player, Items.SHIELD.getDefaultInstance());
            giveOrDrop(player, Items.BOW.getDefaultInstance());

            ItemStack arrows = Items.ARROW.getDefaultInstance();
            arrows.setCount(64);
            giveOrDrop(player, arrows);

            ItemStack food = Items.COOKED_BEEF.getDefaultInstance();
            food.setCount(12);
            giveOrDrop(player, food);

            player.sendSystemMessage(Component.literal(
                    "§a[지급 완료] §f설명서, 기본 전투 장비, 첫 화살 64개가 지급되었습니다."));
        }
        VillageProgressionSystem.registerPlayer(player);
        grantMayorCaller(player);
    }

    public static void grantMayorCaller(ServerPlayer player) {
        if (!VillageCouncilState.isMayor(player) || !player.addTag(MAYOR_CALLER_TAG)) {
            return;
        }
        giveOrDrop(player, named(Items.GOAT_HORN.getDefaultInstance(), "§6촌장 전용 마을 호출기"));
        player.sendSystemMessage(Component.literal(
                "§6[촌장 장비] §f호출기를 사용하면 마을 운영 UI가 열립니다."));
    }

    public static void handleItemInteraction(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (stack.getItem() == Items.KNOWLEDGE_BOOK) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            VillageUiService.openManual(player);
            return;
        }

        if (stack.getItem() == Items.GOAT_HORN) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            VillageUiService.openMayor(player);
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
