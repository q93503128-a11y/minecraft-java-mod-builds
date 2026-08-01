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

import java.util.List;
import java.util.Optional;

/** Recipe-only notebook. Gameplay systems live in LifeGuideItem. */
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
        }
        return InteractionResult.SUCCESS;
    }

    private static WrittenBookContent createContent(ServerPlayer player) {
        CountrysideWorldData.PlayerEstate estate = CountrysideWorldData
                .get(player.level().getServer())
                .estate(player.getUUID())
                .orElse(null);
        String owner = estate == null ? player.getScoreboardName() : estate.ownerName();
        String restaurant = estate == null ? "나의 시골식당" : estate.restaurantName();
        int meals = estate == null ? 0 : estate.mealsPrepared();

        List<Filterable<Component>> pages = List.of(
                page("§6§l시골식당 요리 수첩§r\n\n§2" + restaurant + "§r\n주인  " + owner
                        + "\n\n이 책에는 요리법과 손님 주문 보상만 기록한다. 농사·목장·장터·주민 설명은 별도 시골생활 설명서를 확인하자."),
                page("§6§l시골 전골§r\n\n식당 조리대 전용\n\n1. 들나물을 조리대에 올려 손질한다.\n2. 민물고기를 올려 전골을 완성한다.\n\n들나물: 풀과 고사리 채집\n민물고기: 강에서 낚시\n손님 보상: 동전 5개"),
                page("§6§l들나물 차§r\n\n제작대 조합\n\n• 들나물 1개\n• 꿀이 든 병 1개\n\n가볍게 허기를 채우는 따뜻한 음료다.\n손님 보상: 동전 3개"),
                page("§6§l농가 아침식사§r\n\n제작대 조합\n\n• 달걀 1개\n• 감자 1개\n• 당근 1개\n• 그릇 1개\n\n농장과 목장을 함께 돌봐야 만드는 든든한 한 끼다.\n손님 보상: 동전 6개"),
                page("§6§l손님 주문§r\n\n하루 세 손님은 전골, 들나물 차, 농가 아침식사 중 하나를 주문한다.\n\n주문한 음식을 손에 들고 손님을 사용하면 결제된다. 같은 손님에게는 하루 한 번만 판매할 수 있다."),
                page("§6§l조리 기록§r\n\n지금까지 만든 요리  " + meals + "그릇\n\n새로운 요리가 추가되면 이 수첩에 별도 페이지로 기록된다.")
        );

        return new WrittenBookContent(
                new Filterable<>("시골식당 요리 수첩", Optional.empty()),
                owner,
                0,
                pages,
                false
        );
    }

    private static Filterable<Component> page(String text) {
        return new Filterable<>(Component.literal(text), Optional.empty());
    }
}
