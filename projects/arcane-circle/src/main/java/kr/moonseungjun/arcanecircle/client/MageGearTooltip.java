package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public final class MageGearTooltip {
    private MageGearTooltip() {}

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        List<Component> lines = event.getToolTip();
        if (item == ModItems.MAGE_HAT.get()) hat(lines, 90, 20, 8, 3, 1, 3, "기초 마력 회로");
        else if (item == ModItems.SAGE_HAT.get()) hat(lines, 360, 55, 22, 10, 8, 16, "다중 사고 회로");
        else if (item == ModItems.ARCHMAGE_CROWN.get()) hat(lines, 1200, 130, 48, 28, 20, 42, "대마도사 왕관 회로");
        else if (item == ModItems.MAGE_ROBE.get()) robe(lines, 45, 8, 3, 9, 3, 5, 24, "몸·바지 두 슬롯 사용");
        else if (item == ModItems.SAGE_ROBE.get()) robe(lines, 260, 25, 10, 28, 12, 18, 72, "강화 저항 · 몸·바지 두 슬롯");
        else if (item == ModItems.ARCHMAGE_ROBE.get()) robe(lines, 900, 70, 25, 65, 30, 40, 165, "저항 II · 화염 저항 · 두 슬롯");
        else if (item == ModItems.MAGE_ROBE_HEM.get()
                || item == ModItems.SAGE_ROBE_HEM.get()
                || item == ModItems.ARCHMAGE_ROBE_HEM.get()) {
            title(lines, "로브 하단부");
            lines.add(Component.literal("흉갑 로브 착용 시 자동 장착·해제").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("독립 장비 효과 없음").withStyle(ChatFormatting.DARK_GRAY));
        } else if (item == ModItems.MAGE_BOOTS.get()) boots(lines, 10, 3, 1, 2, 7, 6, "이동 속도 I · 도약 I");
        else if (item == ModItems.SKYWALKER_BOOTS.get()) boots(lines, 90, 10, 4, 8, 25, 25, "이동 II · 도약 II · 공중 완강");
        else if (item == ModItems.FROSTSTEP_BOOTS.get()) boots(lines, 300, 25, 10, 18, 55, 52, "이동 III · 빙결 보행 · 공중 완강");
    }

    private static void hat(List<Component> lines, int mana, int regen, int costDown,
                            int power, int range, int cooldownDown, String trait) {
        title(lines, "마도 모자");
        stat(lines, "최대 마력 +" + mana, ChatFormatting.AQUA);
        stat(lines, "마력 회복 +" + regen + "%", ChatFormatting.BLUE);
        stat(lines, "마력 소모 -" + costDown + "%", ChatFormatting.GREEN);
        stat(lines, "주문 위력 +" + power + "% · 범위 +" + range + "%", ChatFormatting.LIGHT_PURPLE);
        stat(lines, "재사용 대기시간 -" + cooldownDown + "%", ChatFormatting.YELLOW);
        trait(lines, trait);
    }

    private static void robe(List<Component> lines, int mana, int regen, int costDown,
                             int power, int range, int cooldownDown, int vitality, String trait) {
        title(lines, "전투 마도 로브");
        stat(lines, "최대 마력 +" + mana + " · 실효 체력 약 +" + vitality + "%", ChatFormatting.RED);
        stat(lines, "마력 회복 +" + regen + "% · 소모 -" + costDown + "%", ChatFormatting.AQUA);
        stat(lines, "주문 위력 +" + power + "% · 범위 +" + range + "%", ChatFormatting.LIGHT_PURPLE);
        stat(lines, "재사용 대기시간 -" + cooldownDown + "%", ChatFormatting.YELLOW);
        trait(lines, trait);
    }

    private static void boots(List<Component> lines, int mana, int regen, int costDown,
                              int power, int range, int cooldownDown, String trait) {
        title(lines, "기동 마도화");
        stat(lines, "최대 마력 +" + mana + " · 회복 +" + regen + "%", ChatFormatting.AQUA);
        stat(lines, "마력 소모 -" + costDown + "% · 위력 +" + power + "%", ChatFormatting.LIGHT_PURPLE);
        stat(lines, "범위 +" + range + "% · 재사용 -" + cooldownDown + "%", ChatFormatting.YELLOW);
        trait(lines, trait);
    }

    private static void title(List<Component> lines, String value) {
        lines.add(Component.literal("◆ " + value).withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void stat(List<Component> lines, String value, ChatFormatting color) {
        lines.add(Component.literal(value).withStyle(color));
    }

    private static void trait(List<Component> lines, String value) {
        lines.add(Component.literal("고유 효과: " + value).withStyle(ChatFormatting.GOLD));
    }
}
