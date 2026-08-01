package kr.countrysidedays.item;

import kr.countrysidedays.gameplay.SharedRestaurantAccess;
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
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        CountrysideWorldData.PlayerEstate ownEstate = data.estate(player.getUUID()).orElse(null);
        CountrysideWorldData.PlayerEstate restaurantEstate = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(ownEstate);
        String owner = restaurantEstate == null ? player.getScoreboardName() : restaurantEstate.ownerName();
        String restaurant = restaurantEstate == null ? "마을 공동 시골식당" : restaurantEstate.restaurantName();
        String role = SharedRestaurantAccess.isOwner(data, player.getUUID()) ? "식당 주인" : "식당 직원";
        int guests = restaurantEstate == null ? 0 : restaurantEstate.customersServed();
        int ranchGoods = ownEstate == null ? 0 : ownEstate.ranchProductsCollected();

        List<Filterable<Component>> pages = List.of(
                page("§6§l시골생활 설명서§r\n\n§2" + restaurant + "§r\n식당 주인  " + owner
                        + "\n내 역할  " + role
                        + "\n\n요리법을 제외한 식당·농사·목장·장터·주민 생활 규칙을 정리한 안내서다."),
                page("§6§l시작 지원§r\n\n개인 농장은 아무 작물도 심지 않은 빈 밭으로 시작한다.\n\n지급품\n• 밀 씨앗 32개\n• 당근 8개\n• 감자 8개\n• 낚싯대\n\n직접 심거나 농장 배럴에 넣어 일꾼에게 맡길 수 있다."),
                page("§6§l공동 식당§r\n\n멀티에서도 식당은 월드에 하나다. 첫 주민이 주인이 되고 이후 주민은 직원이 된다.\n\n주인과 직원 모두 조리·서빙·영업 개폐를 할 수 있다. 식당 이름 변경은 주인만 가능하다."),
                page("§6§l식당 영업§r\n\n영업시간에 빈손으로 조리대를 사용하면 문과 앞 울타리가 열린다.\n\n손님 세 명은 밖에서 기다리다가 들어와 각자 자리에 앉고 무작위 요리를 주문한다. 영업을 닫거나 시간이 끝나면 모두 퇴장하고 문이 잠긴다.\n\n누적 손님  " + guests),
                page("§6§l개인 농장§r\n\n모든 플레이어는 UUID별로 별도 농장을 가진다. 농장 배럴에 밀 씨앗, 당근이나 감자를 넣어 둔다.\n\n일꾼은 근무시간에 파종하고, 다 자란 작물을 수확해 같은 배럴에 보관한 뒤 다시 심는다."),
                page("§6§l개인 목장§r\n\n모든 플레이어는 별도 목장과 목장 일꾼을 가진다. 보급 배럴에 건초 블록과 물 양동이를 넣으면 비어 있는 급이대와 물통을 채운다.\n\n가축은 울타리 안에서만 생활한다."),
                page("§6§l가축 소유권§r\n\n가축은 태어날 때 목장 주인 UUID에 귀속된다. 다른 플레이어는 도축, 우유 짜기, 털 깎기, 끌고 가기를 할 수 없다.\n\n잘 먹은 성체는 달걀·우유·양털을 생산한다. 수거한 생산물  " + ranchGoods),
                page("§6§l주민의 하루§r\n\n새벽  집에서 준비\n오전  직업별 근무\n정오  광장 점심 휴식\n오후  다시 근무\n해질녘  장터·회관 교류\n밤  자기 집으로 귀가\n\n7일마다 하루는 휴일이며 주민과 상점도 쉰다."),
                page("§6§l시골 직업§r\n\n농부·목장지기·어부·과수원지기·양봉가·낙농·양계처럼 시골 생산 직업이 충분히 배치된다.\n\n판매하는 주민은 근무시간에 자기 가게나 판매대를 지킨다."),
                page("§6§l변동 장터§r\n\n농산물, 축산물과 살림 물품의 가격은 매일 달라진다. 같은 날에는 같은 시세가 유지되고, 되팔기만으로 동전을 무한히 늘릴 수 없도록 매입가가 제한된다."),
                page("§6§lHUD와 길찾기§r\n\n왼쪽 위에는 목표와 공동 식당 영업 상태가 표시된다.\n\n오른쪽 위는 마을 동전, 내 집과 공동 식당의 시야 기준 화살표·거리를 보여준다. 원시 좌표는 표시하지 않는다."),
                page("§6§l마을 약속§r\n\n• 남의 집·농장·목장·보관함은 사용할 수 없다\n• 공동 식당은 등록된 주인과 직원만 운영한다\n• 공공 건물과 도로는 보호된다\n• 죽어도 아이템과 장비는 유지된다")
        );

        return new WrittenBookContent(
                new Filterable<>("시골생활 설명서", Optional.empty()),
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
