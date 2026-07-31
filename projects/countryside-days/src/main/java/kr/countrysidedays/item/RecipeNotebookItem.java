package kr.countrysidedays.item;

import kr.countrysidedays.world.CountrysideWorldData;
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

/** A dynamic parchment notebook for recipes, property directions and village life. */
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
        BlockPos origin = data.homesteadOrigin().orElse(BlockPos.ZERO);
        List<Filterable<Component>> pages = new ArrayList<>();

        pages.add(page(
                "§6§l시골생활 수첩§r\n\n"
                        + "§2" + data.restaurantName() + "§r\n"
                        + "주인  " + fallback(data.ownerName(), player.getScoreboardName()) + "\n\n"
                        + "오늘도 서두르지 말고\n"
                        + "천천히 마을을 돌보자."
        ));

        pages.add(page(
                "§6§l나의 생활 터전§r\n\n"
                        + location("집", origin.offset(-34, 0, -16))
                        + location("농장", origin.offset(10, 0, -4))
                        + location("식당", origin.offset(-7, 0, -5))
                        + location("목장", origin.offset(9, 0, 52))
                        + "\n표지판과 연결된 길을\n따라가면 찾을 수 있다."
        ));

        pages.add(page(
                "§6§l첫 번째 요리§r\n\n"
                        + "§2들나물§r을 식당 조리대에\n"
                        + "먼저 손질한다.\n\n"
                        + "그다음 §9민물고기§r를\n"
                        + "올리면 시골 전골 완성!\n\n"
                        + "들나물: 풀·고사리 채집\n"
                        + "민물고기: 강·연못 낚시"
        ));

        pages.add(page(
                "§6§l식당 영업§r\n\n"
                        + "영업시간  아침~해질녘\n"
                        + "손님에게 전골을 들고\n"
                        + "말을 걸어 서빙한다.\n\n"
                        + "준비한 요리  " + data.mealsPrepared() + "\n"
                        + "맞이한 손님  " + data.customersServed() + "\n"
                        + "모은 마을 동전  " + data.villageCoinsEarned()
        ));

        pages.add(page(
                "§6§l우리 마을 사람들§r\n\n"
                        + "복순 할머니  마을 어른\n"
                        + "농부 한결  농장 돌보기\n"
                        + "목장지기 소미  동물 돌보기\n"
                        + "회관지기 도윤  마을 안내\n"
                        + "민수  식당 단골손님\n\n"
                        + "낮에는 일터와 식당에,\n"
                        + "밤에는 각자의 집으로 간다."
        ));

        pages.add(page(
                "§6§l마을 약속§r\n\n"
                        + "• 남의 집과 창고를\n  허락 없이 열지 않기\n"
                        + "• 주민 농작물을\n  함부로 가져가지 않기\n"
                        + "• 공동시설은 깨끗하게\n"
                        + "• 죽어도 소지품은 유지\n\n"
                        + "식당 이름 바꾸기:\n"
                        + "이름표를 모루에서 고친 뒤\n"
                        + "식당 간판에 사용한다."
        ));

        return new WrittenBookContent(
                new Filterable<>("시골생활 수첩", Optional.empty()),
                fallback(data.ownerName(), player.getScoreboardName()),
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

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
