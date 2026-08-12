package kr.moonseungjun.senbonzakura.item;

import kr.moonseungjun.senbonzakura.bankai.BankaiService;
import net.minecraft.network.chat.Component;
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

public final class ZanpakutoItem extends Item {
    public ZanpakutoItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        return BankaiService.activate(serverPlayer) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§f참백도 · 천본앵"));
        tooltip.accept(Component.literal("§d우클릭: §f卍解 · 千本桜景厳"));
        tooltip.accept(Component.literal("§710초 전개 · 30초 재사용 대기"));
        tooltip.accept(Component.literal("§8수백 개 칼날은 클라이언트 3D geometry로 렌더링"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
