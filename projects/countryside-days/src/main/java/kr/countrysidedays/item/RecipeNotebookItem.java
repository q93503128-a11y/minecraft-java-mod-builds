package kr.countrysidedays.item;

import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dynamic recipe book, estate guide and progression record for one player. */
public final class RecipeNotebookItem extends WrittenBookItem {
    public RecipeNotebookItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT, createContent(serverPlayer));
            serverPlayer.openItemGui(stack, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    private static WrittenBookContent createContent(ServerPlayer player) {
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        CountrysideWorldData.PlayerEstate estate = data.estate(player.getUUID()).orElse(null);
        BlockPos origin = estate == null ? BlockPos.ZERO : estate.originPos();
        String ownerName = estate == null ? player.getScoreboardName() : estate.ownerName();
        String restaurantName = estate == null ? "나의 시골식당" : estate.restaurantName();
        int meals = estate == null ? 0 : estate.mealsPrepared();
        int guests = estate == null ? 0 : estate.customersServed();
        int coins = estate == null ? 0 : estate.coinsEarned();

        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(page(
                "§6§l시골생활 요리 수첩§r\n\n"
                        + "§2" + restaurantName + "§r\n"
                        + "주인  " + ownerName + "\n\n"
                        + "레시피, 장터, 목장과\n"
                        + "식당 진행을 한 권에서\n"
                        + "확인하는 생활 안내서다."
        ));

        pages.add(page(
                "§6§l나의 생활 터전§r\n\n"
                        + location("집", PlayerEstateLayout.home(origin))
                        + location("농장", PlayerEstateLayout.farm(origin))
                        + location("식당", PlayerEstateLayout.restaurant(origin))
                        + location("목장", PlayerEstateLayout.ranch(origin))
                        + "\nHUD는 좌표 대신 바라보는\n"
                        + "방향 화살표와 거리로 안내한다."
        ));

        pages.add(page(
                "§6§l레시피 1 · 시골 전골§r\n\n"
                        + "식당 조리대 전용\n\n"
                        + "1. §2들나물§r을 조리대에\n"
                        + "   올려 손질한다.\n"
                        + "2. §9민물고기§r를 올린다.\n\n"
                        + "들나물: 풀·고사리 채집\n"
                        + "민물고기: 강에서 낚시"
        ));

        pages.add(page(
                "§6§l레시피 2 · 들나물 차§r\n\n"
                        + "제작대 조합\n\n"
                        + "• 들나물 1\n"
                        + "• 꿀이 든 병 1\n\n"
                        + "가볍게 허기를 채우고\n"
                        + "식당 영업 전 마시기 좋은\n"
                        + "따뜻한 생활 음료다."
        ));

        pages.add(page(
                "§6§l레시피 3 · 농가 아침식사§r\n\n"
                        + "제작대 조합\n\n"
                        + "• 달걀 1\n"
                        + "• 감자 1\n"
                        + "• 당근 1\n"
                        + "• 그릇 1\n\n"
                        + "농장과 목장을 함께\n"
                        + "돌봐야 만들 수 있는 한 끼다."
        ));

        pages.add(page(
                "§6§l식당 영업§r\n\n"
                        + "영업시간  아침~해질녘\n"
                        + "손님은 식당 의자까지 와서\n"
                        + "앉아 주문을 기다린다.\n\n"
                        + "준비한 요리  " + meals + "\n"
                        + "맞이한 손님  " + guests + "\n"
                        + "번 마을 동전  " + coins
        ));

        pages.add(page(
                "§6§l느티나무 장터§r\n\n"
                        + "§2농부 한결§r\n"
                        + "씨앗·당근·감자·건초·물\n\n"
                        + "§6목장지기 소미§r\n"
                        + "먹이·울타리문·끈·이름표\n\n"
                        + "§b회관지기 도윤§r\n"
                        + "랜턴·책장·화분·그림·\n"
                        + "카펫·의자 재료\n\n"
                        + "모두 마을 동전을 사용한다."
        ));

        pages.add(page(
                "§6§l목장 관리§r\n\n"
                        + "가축은 내 UUID에 귀속된다.\n"
                        + "배고프면 건초 급이대로 가서\n"
                        + "건초를 실제로 소모한 뒤\n"
                        + "물통에서 물을 마신다.\n\n"
                        + "오래 굶으면 상태가 이름에\n"
                        + "표시되고 결국 죽는다.\n"
                        + "잘 먹은 성체는 자동 번식한다."
        ));

        pages.add(page(
                "§6§l집과 인테리어§r\n\n"
                        + "집에는 침대·상자·화로·\n"
                        + "작업대·책장·식탁이 있다.\n\n"
                        + "도윤의 살림 장터에서\n"
                        + "조명, 카펫, 그림, 화분과\n"
                        + "의자 재료를 사서 꾸민다.\n\n"
                        + "식당 이름은 이름표를\n"
                        + "식당 전면 벽 간판에 사용한다."
        ));

        pages.add(page(
                "§6§l마을의 하루§r\n\n"
                        + "새벽  집에서 하루 준비\n"
                        + "낮    각자의 일터에서 생활\n"
                        + "오후  장터와 회관에서 교류\n"
                        + "밤    각자의 집으로 귀가\n\n"
                        + "손님도 영업시간에만 와서\n"
                        + "앉고, 문을 닫으면 돌아간다."
        ));

        pages.add(page(
                "§6§l마을 약속§r\n\n"
                        + "• 남의 생활 구획은\n  주인만 사용할 수 있다\n"
                        + "• 남의 가축은 만지거나\n  공격할 수 없다\n"
                        + "• 공공시설과 주민 집은\n  부술 수 없다\n"
                        + "• 죽어도 소지품은 유지된다"
        ));

        return new WrittenBookContent(
                new Filterable<>("시골생활 요리 수첩", Optional.empty()),
                ownerName,
                0,
                pages,
                false
        );
    }

    private static Filterable<Component> page(String text) {
        return new Filterable<>(Component.literal(text), Optional.empty());
    }

    private static String location(String label, BlockPos pos) {
        return label + "  " + pos.getX() + ", " + pos.getZ() + "\n";
    }
}
