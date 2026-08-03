package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Every piece states flat and percentage growth and the exact multiplicative combat factors. */
public final class MageGearTooltip {
    private MageGearTooltip(){}
    public static void onTooltip(ItemTooltipEvent e){Item i=e.getItemStack().getItem();List<Component> l=e.getToolTip();
        if(i==ModItems.MAGE_HAT.get())g(l,"입문 마도 모자",60,4,0,0,1.12,.94,1.03,1.02,.96,"기초 마력 회로");
        else if(i==ModItems.SAGE_HAT.get())g(l,"현자 마도 모자",180,8,20,3,1.25,.88,1.08,1.08,.88,"다중 사고 회로");
        else if(i==ModItems.ARCHMAGE_CROWN.get())g(l,"대마도사 관",420,14,60,6,1.45,.78,1.16,1.15,.76,"고위 마력 관제");
        else if(i==ModItems.MAGE_ROBE.get())g(l,"중층 전투 로브",40,3,30,10,1.07,.97,1.08,1.03,.95,"몸/바지 사용");
        else if(i==ModItems.SAGE_ROBE.get())g(l,"현자의 로브",140,7,90,20,1.16,.92,1.18,1.10,.86,"저항 I");
        else if(i==ModItems.ARCHMAGE_ROBE.get())g(l,"대마도사 예복",360,13,250,36,1.32,.84,1.34,1.22,.74,"저항 II");
        else if(i==ModItems.MAGE_BOOTS.get())g(l,"유랑 마도화",20,2,10,3,1.03,.99,1.02,1.06,.95,"이동/도약 I");
        else if(i==ModItems.SKYWALKER_BOOTS.get())g(l,"천공 마도화",70,4,30,7,1.07,.97,1.06,1.16,.86,"수평속도 유지 완강");
        else if(i==ModItems.FROSTSTEP_BOOTS.get())g(l,"빙결 보행화",150,6,60,10,1.12,.94,1.10,1.28,.72,"빙결 보행·완강");
        else if(i==ModItems.CINDER_HOOD.get())g(l,"잿불 전투모",140,7,20,4,1.16,.90,1.15,1.04,.90,"화염 저항");
        else if(i==ModItems.CINDER_ROBE.get())g(l,"잿불 전투로브",160,8,80,18,1.14,.91,1.25,1.06,.88,"화염 저항·공격 특화");
        else if(i==ModItems.CINDER_BOOTS.get())g(l,"화염답화",70,3,25,5,1.06,.97,1.10,1.14,.84,"이동 II·화염 전투");
        else if(i==ModItems.GLACIER_CIRCLET.get())g(l,"빙정 관모",170,8,30,5,1.22,.88,1.10,1.14,.90,"동결 제어");
        else if(i==ModItems.GLACIER_ROBE.get())g(l,"빙정 의복",190,9,110,24,1.18,.90,1.16,1.18,.87,"저항 II·동결 제어");
        else if(i==ModItems.GLACIER_BOOTS.get())g(l,"설원답화",80,4,40,8,1.08,.96,1.07,1.22,.82,"빙결 보행");
        else if(i==ModItems.TEMPEST_HOOD.get())g(l,"폭풍 후드",160,7,20,4,1.18,.90,1.09,1.20,.82,"기동 시전");
        else if(i==ModItems.TEMPEST_ROBE.get())g(l,"폭풍비단 로브",170,8,70,16,1.16,.91,1.14,1.24,.80,"상시 가속");
        else if(i==ModItems.TEMPEST_BOOTS.get())g(l,"천뢰 장화",130,6,45,9,1.12,.94,1.09,1.38,.68,"이동/도약 III·완강");
        else if(i==ModItems.RIFT_CROWN.get())g(l,"균열 관",340,13,60,8,1.38,.80,1.20,1.32,.76,"상시 야간 시야");
        else if(i==ModItems.RIFT_ROBE.get())g(l,"균열 예복",380,14,220,34,1.30,.83,1.38,1.38,.72,"고위 공간술 전투복");
        else if(i==ModItems.RIFT_BOOTS.get())g(l,"성간 보행화",220,9,80,12,1.22,.88,1.18,1.52,.62,"자유 비행·완강");
        else if(hem(i)){title(l,"로브 하단부");l.add(Component.literal("흉갑 로브 착용 시 자동 장착·해제").withStyle(ChatFormatting.GRAY));l.add(Component.literal("독립 장비 효과 없음").withStyle(ChatFormatting.DARK_GRAY));}}
    private static void g(List<Component>l,String name,int mp,int mpPct,int hp,int hpPct,double regen,double cost,double power,double range,double cooldown,String trait){title(l,name);l.add(Component.literal("마력 +"+mp+" · +"+mpPct+"%  |  체력 +"+hp+" · +"+hpPct+"%").withStyle(ChatFormatting.AQUA));l.add(Component.literal("회복 ×"+f(regen)+" · 소모 ×"+f(cost)+" · 위력 ×"+f(power)).withStyle(ChatFormatting.LIGHT_PURPLE));l.add(Component.literal("범위 ×"+f(range)+" · 쿨 ×"+f(cooldown)+"  §8(모두 곱연산)").withStyle(ChatFormatting.YELLOW));l.add(Component.literal("고유 효과: "+trait).withStyle(ChatFormatting.GOLD));}
    private static String f(double v){return String.format(java.util.Locale.ROOT,"%.2f",v);}private static void title(List<Component>l,String v){l.add(Component.literal("◆ "+v).withStyle(ChatFormatting.DARK_PURPLE));}private static boolean hem(Item i){return i==ModItems.MAGE_ROBE_HEM.get()||i==ModItems.SAGE_ROBE_HEM.get()||i==ModItems.ARCHMAGE_ROBE_HEM.get()||i==ModItems.CINDER_ROBE_HEM.get()||i==ModItems.GLACIER_ROBE_HEM.get()||i==ModItems.TEMPEST_ROBE_HEM.get()||i==ModItems.RIFT_ROBE_HEM.get();}
}
