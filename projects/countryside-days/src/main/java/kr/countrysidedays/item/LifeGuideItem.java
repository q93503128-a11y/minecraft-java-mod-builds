package kr.countrysidedays.item;

import kr.countrysidedays.gameplay.EstateWorkerManager;
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
                page("§6§l시작 지원§r\n\n개인 농장은 아무 작물도 심지 않은 빈 밭으로 시작한다.\n\n지급품\n• 돌 괭이\n• 밀 씨앗 32개\n• 당근 8개\n• 감자 8개\n• 낚싯대\n\n처음에는 자동 일꾼이 없으므로 직접 농사와 목장을 시작한다."),
                page("§6§l공동 식당§r\n\n멀티에서도 식당은 월드에 하나다. 첫 주민이 주인이 되고 이후 주민은 직원이 된다.\n\n주인은 영업 전에도 가까이 가면 문과 울타리가 자동으로 열린다. 직원은 영업 중에만 출입한다."),
                page("§6§l식당 영업§r\n\n영업시간에 빈손으로 조리대를 사용하면 문과 앞 울타리가 열린다.\n\n손님 세 명은 밖에서 기다리다가 각자 의자에 앉고 여섯 요리 중 하나를 주문한다. 영업을 닫거나 시간이 끝나면 모두 퇴장한다.\n\n누적 손님  " + guests),
                page("§6§l일꾼 고용§r\n\n농부 한결이나 목장지기 소미에게 마을 동전을 들고 웅크린 채 말을 걸면 해당 일꾼을 고용한다.\n\n고용비  " + EstateWorkerManager.HIRING_FEE + "동전\n하루 월급  " + EstateWorkerManager.DAILY_WAGE + "동전\n\n처음부터 무료 일꾼은 제공되지 않는다."),
                page("§6§l월급 지급§r\n\n농장 일꾼 월급은 농장 창고 배럴에서, 목장 일꾼 월급은 목장 보급 배럴에서 매일 빠져나간다.\n\n마을 동전이 없으면 그날 일을 쉬고, 3일 연속 밀리면 떠난다. 일꾼에게 말을 걸면 상태를 확인할 수 있다."),
                page("§6§l개인 농장§r\n\n밀 씨앗, 당근이나 감자를 직접 심을 수 있다. 농장 일꾼을 고용했다면 재료와 월급을 농장 창고 배럴에 넣어 둔다.\n\n일꾼은 근무시간에 파종하고 다 자란 작물을 수확해 같은 배럴에 보관한다."),
                page("§6§l개인 목장§r\n\n가축은 자기 목장 안에서 생활한다. 목장 일꾼을 고용했다면 보급 배럴에 건초·물 양동이·월급 동전을 넣어 둔다.\n\n다른 플레이어는 도축, 우유 짜기, 털 깎기, 끌고 가기를 할 수 없다. 수거한 생산물  " + ranchGoods),
                page("§6§l공용 낚시터§r\n\n낚시는 마을 서남쪽의 공용 낚시터에서만 가능하다.\n\n남의 집 연못, 개인 농장과 목장 물통, 장식용 수역에서는 물고기를 잡을 수 없다. 공용 낚시터에는 어부가 근무한다."),
                page("§6§l주민의 하루§r\n\n주민들은 모두 중앙 광장으로 몰리지 않는다. 집·가게·농장·목장·과수원·낚시터·정원 등 자기 생활권에서 일하고 쉬며, 시간대에 따라 주변을 조금씩 돌아다닌다.\n\n7일마다 하루는 휴일이다."),
                page("§6§l변동 장터§r\n\n농산물, 축산물과 살림 물품의 가격은 매일 달라진다. 같은 날에는 같은 시세가 유지되고, 되팔기만으로 동전을 무한히 늘릴 수 없도록 매입가가 제한된다."),
                page("§6§l마을 약속§r\n\n• 남의 집·농장·목장·보관함은 사용할 수 없다\n• 남의 사유 수역에서는 낚시할 수 없다\n• 공동 식당은 등록된 주인과 직원만 운영한다\n• 죽어도 아이템과 장비는 유지된다")
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
