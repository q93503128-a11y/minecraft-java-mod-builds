package kr.moonseungjun.senbonzakura.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class ZanpakutoItem extends Item {
    public ZanpakutoItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§f참백도 · 천본앵"));
        tooltip.accept(Component.literal("§dShift + B: §f卍解 · 千本桜景厳"));
        tooltip.accept(Component.literal("§710초 전개 · 30초 재사용 대기"));
        tooltip.accept(Component.literal("§8우클릭으로는 만해가 발동하지 않음"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
