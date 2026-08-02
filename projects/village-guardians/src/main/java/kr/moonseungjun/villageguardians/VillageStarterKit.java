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
    private static final String CALLER_MIGRATION_TAG = "villageguardians_inventory_caller_v1";
    private static final String CALLER_NAME = "마을 수호단 호출기";

    private VillageStarterKit() {}

    public static void grantOnLogin(ServerPlayer player) {
        if (player.addTag(STARTER_KIT_TAG)) {
            giveOrDrop(player, VillageEquipmentRaritySystem.createNamed(
                    Items.IRON_SWORD, VillageEquipmentRaritySystem.Rarity.COMMON, "초임 수호검"));
            giveOrDrop(player, VillageEquipmentRaritySystem.createNamed(
                    Items.SHIELD, VillageEquipmentRaritySystem.Rarity.COMMON, "초임 수호 방패"));
            giveOrDrop(player, VillageEquipmentRaritySystem.createNamed(
                    Items.BOW, VillageEquipmentRaritySystem.Rarity.COMMON, "초임 성루궁"));
            ItemStack arrows = Items.ARROW.getDefaultInstance();
            arrows.setCount(64);
            arrows.set(DataComponents.CUSTOM_NAME,
                    Component.literal("수호 화살").withStyle(ChatFormatting.WHITE));
            giveOrDrop(player, arrows);
            player.sendSystemMessage(Component.literal(
                    "§a[지급 완료] §f기본 전투 장비와 첫 화살 64개가 지급되었습니다."));
        }
        VillageProgressionSystem.registerPlayer(player);
        grantCaller(player);
        VillageProgressionSystem.grantDailyBreadOnLogin(player);
    }

    /** Compatibility name retained for old call sites. It now removes obsolete caller items. */
    public static void grantCaller(ServerPlayer player) {
        boolean removed = removeCallerItems(player);
        boolean firstNotice = player.addTag(CALLER_MIGRATION_TAG);
        if (firstNotice) {
            player.sendSystemMessage(Component.literal(
                    "§6[수호단 조작] §f빠른 통신은 인벤토리 버튼이나 B/U 키로 엽니다. "
                            + "H 상태 · J 성장 · K 직업 성장 · B/U 빠른 통신 · Z/X 기술"));
        } else if (removed) {
            player.sendSystemMessage(Component.literal(
                    "§e기존 호출기 아이템을 제거했습니다. 인벤토리 화면의 빠른 통신 버튼을 사용하세요."));
        }
    }

    public static void grantMayorCaller(ServerPlayer player) { grantCaller(player); }

    public static void handleItemInteraction(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return;
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!isCaller(stack)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        player.setItemInHand(event.getHand(), ItemStack.EMPTY);
        player.sendSystemMessage(Component.literal(
                "§e호출기 아이템은 폐지되었습니다. 인벤토리 화면의 빠른 통신 버튼을 사용하세요."));
    }

    private static boolean removeCallerItems(ServerPlayer player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isCaller(stack)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) player.getInventory().setChanged();
        return changed;
    }

    private static boolean isCaller(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() != Items.CLOCK && stack.getItem() != Items.GOAT_HORN) return false;
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        return customName != null
                && CALLER_NAME.equals(ChatFormatting.stripFormatting(customName.getString()));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
}
