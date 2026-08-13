package kr.moonseungjun.arcanecircle.item;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class ArcaneStaffItem extends Item {
    private final StaffProfile profile;
    public ArcaneStaffItem(Properties properties,StaffProfile profile){super(properties.stacksTo(1));this.profile=profile;}
    public StaffProfile profile(){return profile;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> tooltip,TooltipFlag flag){tooltip.accept(Component.literal("§5[마도 지팡이] §f"+profile.summary()));tooltip.accept(Component.literal("§8주 손 또는 보조 손에 들면 적용 · 모든 비율은 곱연산"));if(profile.maxManaBonus()!=0||profile.maxManaMultiplier()!=1)tooltip.accept(Component.literal("§9최대 마력 "+signed(profile.maxManaBonus())+" · "+percent(profile.maxManaMultiplier())));if(profile.manaCostMultiplier()!=1)tooltip.accept(Component.literal("§b마력 소모 ×"+two(profile.manaCostMultiplier())));if(profile.powerMultiplier()!=1)tooltip.accept(Component.literal("§c위력 ×"+two(profile.powerMultiplier())));if(profile.rangeMultiplier()!=1)tooltip.accept(Component.literal("§a범위·사거리 ×"+two(profile.rangeMultiplier())));if(profile.castTimeMultiplier()!=1)tooltip.accept(Component.literal("§d시전 전개시간 ×"+two(profile.castTimeMultiplier())));if(profile.cooldownMultiplier()!=1)tooltip.accept(Component.literal("§e재사용 대기시간 ×"+two(profile.cooldownMultiplier())));if(profile.regenMultiplier()!=1)tooltip.accept(Component.literal("§d마력 회복 ×"+two(profile.regenMultiplier())));if(profile.favoredSchool()!=null)tooltip.accept(Component.literal("§6"+profile.favoredSchool().displayName()+" 학파 위력 ×"+two(profile.favoredPowerMultiplier())));if(!profile.recipeHint().isBlank())tooltip.accept(Component.literal("§7제작: "+profile.recipeHint()));super.appendHoverText(stack,context,display,tooltip,flag);}
    private static String signed(int v){return v>=0?"+"+v:Integer.toString(v);}private static String percent(double m){long d=Math.round((m-1)*100);return(d>=0?"+":"")+d+"%";}private static String two(double v){return String.format(java.util.Locale.ROOT,"%.2f",v);}
    public record StaffProfile(String id,String displayName,String summary,String recipeHint,int maxManaBonus,double maxManaMultiplier,double manaCostMultiplier,double powerMultiplier,double rangeMultiplier,double castTimeMultiplier,double cooldownMultiplier,double regenMultiplier,SpellDefinition.School favoredSchool,double favoredPowerMultiplier){public StaffProfile{castTimeMultiplier=Math.max(.20,castTimeMultiplier);}public double powerFor(SpellDefinition.School school){return powerMultiplier*(favoredSchool==school?favoredPowerMultiplier:1);}public static final StaffProfile NONE=new StaffProfile("none","맨손","지팡이 효과 없음","",0,1,1,1,1,1,1,1,null,1);}
}
