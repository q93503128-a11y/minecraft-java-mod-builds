package kr.countrysidedays.item;

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

/** Contains recipes only. All management and life-system help belongs in LifeGuideItem. */
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
        List<Filterable<Component>> pages = List.of(
                page("§6§l시골 전골§r\n\n식당 조리대 전용\n\n재료\n• 들나물 1개\n• 민물고기 1마리\n\n조리\n1. 들나물을 든 채 조리대를 사용한다.\n2. 손질이 끝나면 민물고기를 든 채 다시 사용한다."),
                page("§6§l들나물 차§r\n\n제작대 조합\n\n재료\n• 들나물 1개\n• 꿀이 든 병 1개\n\n두 재료를 제작대에 올리면 따뜻한 들나물 차가 완성된다."),
                page("§6§l농가 아침식사§r\n\n제작대 조합\n\n재료\n• 달걀 1개\n• 감자 1개\n• 당근 1개\n• 그릇 1개\n\n네 재료를 함께 조합하면 농가 아침식사가 완성된다.")
        );

        return new WrittenBookContent(
                new Filterable<>("시골식당 요리 수첩", Optional.empty()),
                player.getScoreboardName(),
                0,
                pages,
                false
        );
    }

    private static Filterable<Component> page(String text) {
        return new Filterable<>(Component.literal(text), Optional.empty());
    }
}
