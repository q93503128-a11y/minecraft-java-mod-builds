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
    private static final String STARTER_KIT_TAG = "villageguardians_starter_kit_v1";
    private static final String MAYOR_CALLER_TAG = "villageguardians_mayor_caller_v1";

    private VillageStarterKit() {
    }

    public static void grantOnLogin(ServerPlayer player) {
        if (player.addTag(STARTER_KIT_TAG)) {
            giveOrDrop(player, named(Items.KNOWLEDGE_BOOK.getDefaultInstance(), "§6마을 수호대 작전 설명서"));
            giveOrDrop(player, Items.IRON_SWORD.getDefaultInstance());
            giveOrDrop(player, Items.SHIELD.getDefaultInstance());
            giveOrDrop(player, Items.BOW.getDefaultInstance());

            ItemStack arrows = Items.ARROW.getDefaultInstance();
            arrows.setCount(24);
            giveOrDrop(player, arrows);

            ItemStack food = Items.COOKED_BEEF.getDefaultInstance();
            food.setCount(12);
            giveOrDrop(player, food);

            player.sendSystemMessage(Component.literal(
                    "§a[지급 완료] §f작전 설명서와 기본 전투 장비가 인벤토리에 들어왔습니다."));
        }
        grantMayorCaller(player);
    }

    public static void grantMayorCaller(ServerPlayer player) {
        if (!VillageCouncilState.isMayor(player) || !player.addTag(MAYOR_CALLER_TAG)) {
            return;
        }
        giveOrDrop(player, named(Items.GOAT_HORN.getDefaultInstance(), "§6촌장 전용 마을 호출기"));
        player.sendSystemMessage(Component.literal(
                "§6[촌장 장비] §f마을 호출기를 지급했습니다. 사용: 현황 확인 / 웅크리고 사용: 다음 단계 투표."));
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
            showManual(player);
            return;
        }

        if (stack.getItem() == Items.GOAT_HORN) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            useMayorCaller(player);
        }
    }

    private static void showManual(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6========== 마을 수호대 작전 설명서 =========="));
        player.sendSystemMessage(Component.literal("§f1. 아침·낮: 건물 시설을 사용하고 보급품으로 업그레이드합니다."));
        player.sendSystemMessage(Component.literal("§f2. 회관 종: 현황 확인 / 웅크리고 사용하면 시간 진행 투표."));
        player.sendSystemMessage(Component.literal("§f3. 창고 통: 하루 한 번 식량·화살 보급."));
        player.sendSystemMessage(Component.literal("§f4. 병영 과녁: 3분마다 훈련 XP 획득."));
        player.sendSystemMessage(Component.literal("§f5. 대장간·의무소·성벽 관리소: 효과 확인 / 웅크리고 업그레이드."));
        player.sendSystemMessage(Component.literal("§f6. 밤이 되면 습격이 시작됩니다. 모든 웨이브를 막으면 보급품과 XP를 얻습니다."));
        player.sendSystemMessage(Component.literal("§f7. 역할 선택: /vg role <역할> | 역할 스킬: /vg skill"));
        player.sendSystemMessage(Component.literal("§7역할: guard_captain, builder, quartermaster, scout, steward, medic"));
        player.sendSystemMessage(Component.literal("§6============================================"));
    }

    private static void useMayorCaller(ServerPlayer player) {
        if (!VillageCouncilState.isMayor(player)) {
            player.sendSystemMessage(Component.literal("§c현재 촌장만 이 호출기를 사용할 수 있습니다."));
            return;
        }

        if (player.isShiftKeyDown()) {
            player.sendSystemMessage(Component.literal(VillageCouncilState.proposeAdvanceTime(player)));
            return;
        }

        player.sendSystemMessage(Component.literal(VillageCouncilState.status(player.level().getServer(), player)));
        player.sendSystemMessage(Component.literal(VillageProgressionSystem.status()));
        player.sendSystemMessage(Component.literal(VillageRaidSystem.status()));
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
