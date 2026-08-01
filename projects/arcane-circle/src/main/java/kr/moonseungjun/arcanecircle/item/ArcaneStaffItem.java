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

    public ArcaneStaffItem(Properties properties, StaffProfile profile) {
        super(properties.stacksTo(1));
        this.profile = profile;
    }

    public StaffProfile profile() {
        return profile;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§5[마도 지팡이] §f" + profile.summary()));
        tooltip.accept(Component.literal("§8주 손 또는 보조 손에 들면 적용"));
        if (profile.maxManaBonus() != 0) tooltip.accept(Component.literal("§9최대 마력 " + signed(profile.maxManaBonus())));
        if (profile.manaCostMultiplier() != 1.0) tooltip.accept(Component.literal("§b마력 소모 " + percentDelta(profile.manaCostMultiplier(), true)));
        if (profile.powerMultiplier() != 1.0) tooltip.accept(Component.literal("§c위력 " + percentDelta(profile.powerMultiplier(), false)));
        if (profile.rangeMultiplier() != 1.0) tooltip.accept(Component.literal("§a범위·사거리 " + percentDelta(profile.rangeMultiplier(), false)));
        if (profile.cooldownMultiplier() != 1.0) tooltip.accept(Component.literal("§e재사용 대기시간 " + percentDelta(profile.cooldownMultiplier(), true)));
        if (profile.regenMultiplier() != 1.0) tooltip.accept(Component.literal("§d마력 회복 " + percentDelta(profile.regenMultiplier(), false)));
        if (profile.favoredSchool() != null) {
            tooltip.accept(Component.literal("§6" + profile.favoredSchool().displayName() + " 학파 위력 +"
                    + Math.round((profile.favoredPowerMultiplier() - 1.0) * 100.0) + "%"));
        }
        if (!profile.recipeHint().isBlank()) {
            tooltip.accept(Component.literal("§7제작: " + profile.recipeHint()));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static String percentDelta(double multiplier, boolean inverseGood) {
        long delta = Math.round((multiplier - 1.0) * 100.0);
        String sign = delta >= 0 ? "+" : "";
        return sign + delta + "%" + (inverseGood && delta < 0 ? " §7(감소)" : "");
    }

    public record StaffProfile(
            String id,
            String displayName,
            String summary,
            String recipeHint,
            int maxManaBonus,
            double manaCostMultiplier,
            double powerMultiplier,
            double rangeMultiplier,
            double cooldownMultiplier,
            double regenMultiplier,
            SpellDefinition.School favoredSchool,
            double favoredPowerMultiplier
    ) {
        public double powerFor(SpellDefinition.School school) {
            return powerMultiplier * (favoredSchool == school ? favoredPowerMultiplier : 1.0);
        }

        public static final StaffProfile NONE = new StaffProfile(
                "none", "맨손", "지팡이 효과 없음", "", 0,
                1.0, 1.0, 1.0, 1.0, 1.0, null, 1.0);
    }
}
