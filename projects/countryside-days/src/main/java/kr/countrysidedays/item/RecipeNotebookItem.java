package kr.countrysidedays.item;

import kr.countrysidedays.world.CountrysideWorldData;
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
        String ownerName = estate == null ? player.getScoreboardName() : estate.ownerName();
        String restaurantName = estate == null ? "나의 시골식당" : estate.restaurantName();
        int meals = estate == null ? 0 : estate.mealsPrepared();
        int guests = estate == null ? 0 : estate.customersServed();
        int coins = estate == null ? 0 : estate.coinsEarned();
        long day = Math.max(0L, player.level().getGameTime() / 24000L);
        int guestsToday = estate == null ? 0 : estate.customersServedToday(day);
        int eggs = estate == null ? 0 : estate.pendingEggs();
        int milk = estate == null ? 0 : estate.pendingMilk();
        int wool = estate == null ? 0 : estate.pendingWool();
        int stage = estate == null ? 0 : estate.progressionStage();
        String shift = estate != null && estate.restaurantOpen() ? "§a영업 중" : "§7영업 닫힘";

        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(page(
                "§6§l시골생활 요리 수첩§r\n\n"
                        + "§2" + restaurantName + "§r\n"
                        + "주인  " + ownerName + "\n\n"
                        + "레시피·식당 영업·장터·\n"
                        + "목장과 생활 진행을 한 권에서\n"
                        + "확인하는 실제 생활 안내서다."
        ));

        pages.add(page(
                "§6§l오늘의 생활 현황§r\n\n"
                        + "식당  " + shift + "\n"
                        + "오늘 손님  " + guestsToday + "/" + CountrysideWorldData.DAILY_CUSTOMER_CAP + "\n"
                        + "누적 손님  " + guests + "\n"
                        + "준비한 요리  " + meals + "\n"
                        + "번 마을 동전  " + coins + "\n"
                        + "생활 단계  " + stage + "\n\n"
                        + "집과 식당은 화면 오른쪽의\n"
                        + "화살표와 거리로 찾아간다."
        ));

        pages.add(page(
                "§6§l생활 진행 순서§r\n\n"
                        + "1. 첫 영업을 열고 손님 맞이\n"
                        + "2. 누적 손님 5명 달성\n"
                        + "3. 목장 생산물 첫 수거\n"
                        + "4. 누적 손님 15명 달성\n"
                        + "5. 식당·목장·집 자유 운영\n\n"
                        + "현재 목표는 화면 왼쪽 위에\n"
                        + "작게 표시된다."
        ));

        pages.add(page(
                "§6§l레시피 1 · 시골 전골§r\n\n"
                        + "식당 조리대 전용\n\n"
                        + "1. §2들나물§r을 조리대에\n"
                        + "   올려 손질한다.\n"
                        + "2. §9민물고기§r를 올린다.\n\n"
                        + "들나물: 풀·고사리 채집\n"
                        + "민물고기: 강에서 낚시\n"
                        + "손님 보상: 동전 5개"
        ));

        pages.add(page(
                "§6§l레시피 2 · 들나물 차§r\n\n"
                        + "제작대 조합\n\n"
                        + "• 들나물 1\n"
                        + "• 꿀이 든 병 1\n\n"
                        + "꿀병은 농부 한결에게 산다.\n"
                        + "손님 보상: 동전 3개"
        ));

        pages.add(page(
                "§6§l레시피 3 · 농가 아침식사§r\n\n"
                        + "제작대 조합\n\n"
                        + "• 달걀 1  • 감자 1\n"
                        + "• 당근 1  • 그릇 1\n\n"
                        + "달걀은 건강한 닭이 낳는다.\n"
                        + "손님 보상: 동전 6개"
        ));

        pages.add(page(
                "§6§l식당 영업 방법§r\n\n"
                        + "1. 조리대를 §l빈손§r으로 눌러\n"
                        + "   식당 문을 연다.\n"
                        + "2. 손님 세 명이 각자 의자에\n"
                        + "   앉으면 주문을 확인한다.\n"
                        + "3. 요구한 요리를 손에 들고\n"
                        + "   손님을 다시 누른다.\n\n"
                        + "세 명을 모두 대접하면\n"
                        + "그날 영업은 자동 종료된다."
        ));

        pages.add(page(
                "§6§l오늘의 주문 규칙§r\n\n"
                        + "손님마다 전골·들나물 차·\n"
                        + "농가 아침식사 중 하나를\n"
                        + "주문한다. 주문은 매일 바뀐다.\n\n"
                        + "전골  동전 5 / 경험치 8\n"
                        + "차    동전 3 / 경험치 5\n"
                        + "아침  동전 6 / 경험치 10\n\n"
                        + "남의 식당 손님은 대접할 수 없다."
        ));

        pages.add(page(
                "§6§l목장 생산과 수거§r\n\n"
                        + "수거 대기\n"
                        + "달걀 " + eggs + " · 우유 " + milk + " · 양털 " + wool + "\n\n"
                        + "잘 먹은 성체 닭·소·양은\n"
                        + "하루마다 생산물을 남긴다.\n"
                        + "헛간 안쪽 배럴을 누르면\n"
                        + "내 생산물을 한꺼번에 받는다.\n\n"
                        + "배고픈 가축은 생산하지 않는다."
        ));

        pages.add(page(
                "§6§l목장 관리§r\n\n"
                        + "가축은 내 UUID에 귀속된다.\n"
                        + "포만도가 낮아지면 무리가\n"
                        + "건초 한 블록과 물 한 칸을\n"
                        + "실제로 소비해 회복한다.\n\n"
                        + "오래 굶으면 상태가 이름에\n"
                        + "표시되고 결국 죽는다.\n"
                        + "잘 먹은 성체는 3일마다 번식한다."
        ));

        pages.add(page(
                "§6§l느티나무 장터§r\n\n"
                        + "§2한결§r  씨앗·작물·건초·물·\n"
                        + "      꿀병·그릇 판매\n"
                        + "      당근·감자 매입\n\n"
                        + "§6소미§r  먹이·울타리문·끈·\n"
                        + "      이름표·가위 판매\n"
                        + "      달걀·우유·양털 매입\n\n"
                        + "§b도윤§r  조명·카펫·책장·그림·\n"
                        + "      화분·의자 재료 판매"
        ));

        pages.add(page(
                "§6§l집과 인테리어§r\n\n"
                        + "집에는 침대·상자·화로·\n"
                        + "작업대·책장·식탁이 있다.\n\n"
                        + "도윤의 살림 장터에서\n"
                        + "랜턴, 카펫, 그림, 화분과\n"
                        + "의자 재료를 사서 꾸민다.\n\n"
                        + "식당 이름은 모루로 적은\n"
                        + "이름표를 전면 벽 간판에 쓴다."
        ));

        pages.add(page(
                "§6§l마을의 하루§r\n\n"
                        + "새벽  집에서 하루 준비\n"
                        + "낮    각자의 일터에서 생활\n"
                        + "오후  장터와 회관에서 교류\n"
                        + "밤    각자의 집으로 귀가\n\n"
                        + "식당 손님은 영업 중일 때만\n"
                        + "의자에 앉고 영업이 끝나면\n"
                        + "중앙 마을로 돌아간다."
        ));

        pages.add(page(
                "§6§l마을 약속§r\n\n"
                        + "• 남의 생활 구획은\n  주인만 사용할 수 있다\n"
                        + "• 남의 가축과 생산물은\n  건드릴 수 없다\n"
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
}
