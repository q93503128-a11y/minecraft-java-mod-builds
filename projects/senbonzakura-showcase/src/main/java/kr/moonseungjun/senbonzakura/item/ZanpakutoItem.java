package kr.moonseungjun.senbonzakura.item;

import net.minecraft.network.chat.Component;
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
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§f참백도 · 천본앵"));
        tooltip.accept(Component.literal("§dShift + B: §f卍解 · 千本桜景厳"));
        tooltip.accept(Component.literal("§7전개 13초 · 재사용 대기 30초"));
        tooltip.accept(Component.literal("§8거대 검열 → 분해 → 칼날 해류"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
