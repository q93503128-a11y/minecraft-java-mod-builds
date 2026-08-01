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

/** A dynamic parchment notebook for the reader's private estate and village life. */
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
                "§6§l시골생활 수첩§r\n\n"
                        + "§2" + restaurantName + "§r\n"
                        + "주인  " + ownerName + "\n\n"
                        + "이 수첩은 다른 사람의\n"
                        + "구획이 아니라 내 생활 터전만\n"
                        + "표시한다."
        ));

        pages.add(page(
                "§6§l나의 생활 터전§r\n\n"
                        + location("집", PlayerEstateLayout.home(origin))
                        + location("농장", PlayerEstateLayout.farm(origin))
                        + location("식당", PlayerEstateLayout.restaurant(origin))
                        + location("목장", PlayerEstateLayout.ranch(origin))
                        + "\nHUD에도 집과 식당까지의\n"
                        + "방향과 거리가 표시된다."
        ));

        pages.add(page(
                "§6§l첫 번째 요리§r\n\n"
                        + "§2들나물§r을 식당 조리대에\n"
                        + "먼저 손질한다.\n\n"
                        + "그다음 §9민물고기§r를\n"
                        + "올리면 시골 전골 완성!\n\n"
                        + "들나물: 풀·고사리 채집\n"
                        + "민물고기: 강에서 낚시"
        ));

        pages.add(page(
                "§6§l식당 영업§r\n\n"
                        + "영업시간  아침~해질녘\n"
                        + "내 식당 손님에게 전골을\n"
                        + "들고 말을 걸어 서빙한다.\n\n"
                        + "준비한 요리  " + meals + "\n"
                        + "맞이한 손님  " + guests + "\n"
                        + "번 마을 동전  " + coins
        ));

        pages.add(page(
                "§6§l우리 마을 사람들§r\n\n"
                        + "복순 할머니  마을 어른\n"
                        + "농부 한결  장터와 농사\n"
                        + "목장지기 소미  동물 돌보기\n"
                        + "회관지기 도윤  마을 안내\n\n"
                        + "각자 가구가 놓인 집에서\n"
                        + "살고 낮에는 일터로 간다."
        ));

        pages.add(page(
                "§6§l마을 약속§r\n\n"
                        + "• 남의 생활 구획은\n  주인만 사용할 수 있다\n"
                        + "• 공공시설과 주민 집은\n  부술 수 없다\n"
                        + "• 주인 없는 농장과 목장은\n  만들지 않는다\n"
                        + "• 죽어도 소지품은 유지\n\n"
                        + "식당 이름: 이름표를\n"
                        + "식당 입간판에 사용한다."
        ));

        return new WrittenBookContent(
                new Filterable<>("시골생활 수첩", Optional.empty()),
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
