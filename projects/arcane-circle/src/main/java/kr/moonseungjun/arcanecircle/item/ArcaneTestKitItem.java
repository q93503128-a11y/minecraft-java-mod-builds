package kr.moonseungjun.arcanecircle.item;

import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCastingService;
import kr.moonseungjun.arcanecircle.magic.SpellKineticsService;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import kr.moonseungjun.arcanecircle.world.ArcaneWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/** No recipe/loot source: this exists only in the creative tools tab. */
public final class ArcaneTestKitItem extends Item {
    public ArcaneTestKitItem(Properties properties){super(properties.stacksTo(1));}
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand){
        if(!(player instanceof ServerPlayer serverPlayer)||!(level instanceof ServerLevel serverLevel))return InteractionResult.SUCCESS;
        if(!serverPlayer.hasInfiniteMaterials()){
            serverPlayer.sendSystemMessage(Component.literal("§c[아르카나 시험핵] §f크리에이티브 테스트 전용 아이템입니다."));
            return InteractionResult.FAIL;
        }
        SpellCastingService.clearSession(serverPlayer.getUUID());
        SpellKineticsService.clear(serverPlayer.getUUID());
        MagicPlayerData data=MagicPlayerData.get(serverLevel.getServer());
        int unlocked=data.enableCreativeTestProfile(serverPlayer);
        long marks=ArcaneWorldData.get(serverLevel.getServer()).addMarks(serverPlayer,1_000_000_000L);
        ArcaneNetwork.sync(serverPlayer);
        serverPlayer.sendSystemMessage(Component.literal("§d[아르카나 시험핵] §f9써클 · 전체 주문 · 최대 시험 숙련 · 쿨타임 초기화 적용"));
        serverPlayer.sendSystemMessage(Component.literal("§7추가 해금 "+unlocked+"개 · 아르카나 "+marks+" · 최고써클 주문 5개 자동 장착"));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                           Consumer<Component> tooltip, TooltipFlag flag){
        tooltip.accept(Component.literal("§d[크리에이티브 개발용]"));
        tooltip.accept(Component.literal("§f9써클 · 전체 주문 · 숙련 최대 · 마력 회복"));
        tooltip.accept(Component.literal("§f쿨타임 초기화 · 아르카나 +10억 · 최고써클 5개 장착"));
        tooltip.accept(Component.literal("§8조합법/전리품 없음 · 크리에이티브 탭 전용"));
        super.appendHoverText(stack,context,display,tooltip,flag);
    }
}
