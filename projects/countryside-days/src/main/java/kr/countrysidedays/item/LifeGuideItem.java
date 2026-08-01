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

/** System manual kept separate from the recipe notebook. */
public final class LifeGuideItem extends WrittenBookItem {
    public LifeGuideItem(Item.Properties properties) {
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
        int guests = estate == null ? 0 : estate.customersServed();
        int ranchGoods = estate == null ? 0 : estate.ranchProductsCollected();

        List<Filterable<Component>> pages = List.of(
                page("§6§l시골생활 설명서§r\n\n§2" + restaurant + "§r\n주인  " + owner
                        + "\n\n요리법을 제외한 영업, 농사, 목장, 장터와 주민 생활 규칙을 정리한 안내서다."),
                page("§6§l시작 지원§r\n\n개인 농장은 아무 작물도 심지 않은 빈 밭으로 시작한다.\n\n지급품\n• 밀 씨앗 24개\n• 당근 16개\n• 감자 16개\n• 낚싯대\n\n직접 심거나 농장 배럴에 넣어 일꾼에게 맡길 수 있다."),
                page("§6§l식당 영업§r\n\n빈손으로 조리대를 사용하면 영업을 열고 닫는다.\n\n하루 손님은 최대 3명이며 각자 다른 요리를 주문한다. 세 손님을 모두 받으면 그날 영업이 끝난다.\n\n누적 손님  " + guests),
                page("§6§l농장 일꾼§r\n\n농장 창고 배럴에 밀 씨앗, 당근이나 감자를 넣어 둔다.\n\n일꾼은 근무시간에 빈 경작지에 작물을 심고, 다 자라면 수확해 같은 배럴에 보관한다.\n\n플레이어가 직접 농사해도 된다."),
                page("§6§l목장 일꾼§r\n\n헛간 보급 배럴에 건초 블록과 물 양동이를 넣어 둔다.\n\n일꾼은 비어 있는 급이대와 물통을 채운다. 가축은 실제로 먹이와 물을 소비하고 오래 굶으면 죽는다."),
                page("§6§l목장 생산§r\n\n잘 먹은 성체는 달걀, 우유와 양털을 생산한다. 생산물은 헛간 수거 배럴에서 받는다.\n\n수거한 생산물  " + ranchGoods + "\n\n생산물은 목장지기에게 판매할 수 있다."),
                page("§6§l주민의 하루§r\n\n새벽  집에서 준비\n오전  직업별 근무\n정오  점심 휴식\n오후  다시 근무\n해질녘  광장과 회관\n밤  집으로 귀가\n\n7일마다 하루는 휴일이며 주민과 상점도 쉰다."),
                page("§6§l변동 장터§r\n\n농산물, 축산물과 살림 물품의 가격은 매일 달라진다.\n\n싸게 사는 날과 비싸게 팔 수 있는 날을 골라 거래하자. 휴일에는 상점이 문을 열지 않는다."),
                page("§6§lHUD와 길찾기§r\n\n왼쪽 위에는 현재 목표와 영업 상태가 표시된다.\n\n오른쪽 위는 마을 동전, 집과 식당의 상대 방향 화살표와 거리를 보여준다. 좌표를 외울 필요가 없다."),
                page("§6§l마을 약속§r\n\n• 남의 생활 구획과 보관함은 사용할 수 없다\n• 남의 가축은 만지거나 공격할 수 없다\n• 공공 건물과 도로는 보호된다\n• 죽어도 아이템과 장비는 유지된다")
        );

        return new WrittenBookContent(
                new Filterable<>("시골생활 설명서", Optional.empty()),
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
