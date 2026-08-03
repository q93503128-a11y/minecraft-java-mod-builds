package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public final class MageGearTooltip {
    private MageGearTooltip(){}
    public static void onTooltip(ItemTooltipEvent event){Item i=event.getItemStack().getItem();List<Component> l=event.getToolTip();
        if(i==ModItems.MAGE_HAT.get())gear(l,"입문 마도 모자","MP +90 · 회복 +20%","소모 -8% · 위력 +3% · 범위 +1% · 쿨 -3%","기초 마력 회로");
        else if(i==ModItems.SAGE_HAT.get())gear(l,"현자 마도 모자","MP +300 · 회복 +42%","소모 -18% · 위력 +9% · 범위 +8% · 쿨 -14%","다중 사고 회로");
        else if(i==ModItems.ARCHMAGE_CROWN.get())gear(l,"대마도사 관","MP +850 · 회복 +85%","소모 -35% · 위력 +20% · 범위 +17% · 쿨 -32%","대마도사 왕관 회로");
        else if(i==ModItems.MAGE_ROBE.get())gear(l,"중층 전투 로브","MP +45 · 회복 +8%","소모 -3% · 위력 +8% · 범위 +3% · 쿨 -5%","실효 체력 +24% · 몸/바지 사용");
        else if(i==ModItems.SAGE_ROBE.get())gear(l,"현자의 로브","MP +210 · 회복 +20%","소모 -9% · 위력 +20% · 범위 +11% · 쿨 -16%","실효 체력 +72% · 저항");
        else if(i==ModItems.ARCHMAGE_ROBE.get())gear(l,"대마도사 예복","MP +650 · 회복 +48%","소모 -20% · 위력 +42% · 범위 +25% · 쿨 -33%","실효 체력 +165% · 저항 II");
        else if(i==ModItems.MAGE_BOOTS.get())gear(l,"유랑 마도화","MP +10 · 회복 +3%","위력 +2% · 범위 +7% · 쿨 -6%","이동/도약 I");
        else if(i==ModItems.SKYWALKER_BOOTS.get())gear(l,"천공 마도화","MP +75 · 회복 +8%","위력 +7% · 범위 +20% · 쿨 -20%","이동/도약 II · 완강");
        else if(i==ModItems.FROSTSTEP_BOOTS.get())gear(l,"빙결 보행화","MP +220 · 회복 +18%","위력 +13% · 범위 +42% · 쿨 -42%","빙결 보행 · 장기 완강");
        else if(i==ModItems.CINDER_HOOD.get())gear(l,"잿불 전투모","MP +170 · 회복 +18%","소모 -10% · 위력 +17% · 범위 +4% · 쿨 -10%","화염 저항");
        else if(i==ModItems.CINDER_ROBE.get())gear(l,"잿불 전투로브","MP +160 · 회복 +15%","소모 -8% · 위력 +27% · 범위 +5% · 쿨 -12%","화염 저항 · 공격 특화");
        else if(i==ModItems.CINDER_BOOTS.get())gear(l,"화염답화","MP +55 · 회복 +6%","소모 -3% · 위력 +12% · 범위 +16% · 쿨 -17%","이동 II · 화염 전투");
        else if(i==ModItems.GLACIER_CIRCLET.get())gear(l,"빙정 관모","MP +220 · 회복 +30%","소모 -14% · 위력 +12% · 범위 +16% · 쿨 -12%","동결 제어 특화");
        else if(i==ModItems.GLACIER_ROBE.get())gear(l,"빙정 의복","MP +250 · 회복 +22%","소모 -10% · 위력 +18% · 범위 +22% · 쿨 -14%","저항 II · 동결 제어");
        else if(i==ModItems.GLACIER_BOOTS.get())gear(l,"설원답화","MP +80 · 회복 +10%","위력 +8% · 범위 +30% · 쿨 -22%","빙결 보행");
        else if(i==ModItems.TEMPEST_HOOD.get())gear(l,"폭풍 후드","MP +190 · 회복 +24%","소모 -11% · 위력 +11% · 범위 +24% · 쿨 -24%","기동 시전 특화");
        else if(i==ModItems.TEMPEST_ROBE.get())gear(l,"폭풍비단 로브","MP +180 · 회복 +18%","소모 -8% · 위력 +16% · 범위 +30% · 쿨 -25%","상시 가속");
        else if(i==ModItems.TEMPEST_BOOTS.get())gear(l,"천뢰 장화","MP +150 · 회복 +16%","소모 -6% · 위력 +10% · 범위 +55% · 쿨 -45%","이동/도약 III · 완강");
        else if(i==ModItems.RIFT_CROWN.get())gear(l,"균열 관","MP +700 · 회복 +70%","소모 -30% · 위력 +26% · 범위 +45% · 쿨 -34%","상시 야간 시야");
        else if(i==ModItems.RIFT_ROBE.get())gear(l,"균열 예복","MP +760 · 회복 +52%","소모 -22% · 위력 +48% · 범위 +52% · 쿨 -36%","고위 공간술 전투복");
        else if(i==ModItems.RIFT_BOOTS.get())gear(l,"성간 보행화","MP +380 · 회복 +32%","소모 -14% · 위력 +20% · 범위 +80% · 쿨 -54%","자유 비행 · 완강");
        else if(isHem(i)){title(l,"로브 하단부");l.add(Component.literal("흉갑 로브 착용 시 자동 장착·해제").withStyle(ChatFormatting.GRAY));l.add(Component.literal("독립 장비 효과 없음").withStyle(ChatFormatting.DARK_GRAY));}}
    private static boolean isHem(Item i){return i==ModItems.MAGE_ROBE_HEM.get()||i==ModItems.SAGE_ROBE_HEM.get()||i==ModItems.ARCHMAGE_ROBE_HEM.get()||i==ModItems.CINDER_ROBE_HEM.get()||i==ModItems.GLACIER_ROBE_HEM.get()||i==ModItems.TEMPEST_ROBE_HEM.get()||i==ModItems.RIFT_ROBE_HEM.get();}
    private static void gear(List<Component> l,String type,String line1,String line2,String trait){title(l,type);l.add(Component.literal(line1).withStyle(ChatFormatting.AQUA));l.add(Component.literal(line2).withStyle(ChatFormatting.LIGHT_PURPLE));l.add(Component.literal("고유 효과: "+trait).withStyle(ChatFormatting.GOLD));}
    private static void title(List<Component> l,String v){l.add(Component.literal("◆ "+v).withStyle(ChatFormatting.DARK_PURPLE));}
}
