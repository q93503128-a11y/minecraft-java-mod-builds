package kr.moonseungjun.anomalousclasses.item;

import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import kr.moonseungjun.anomalousclasses.state.MentalDominatorRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.Optional;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.state.ClassFocus;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.FocusTier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ClassFocusItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.ITEM, AnomalousClasses.MOD_ID);

    private static final Map<ClassId, Map<FocusTier, DeferredHolder<Item, Item>>> ITEMS_BY_FOCUS = createItems();

    private ClassFocusItems() {}

    private static Map<ClassId, Map<FocusTier, DeferredHolder<Item, Item>>> createItems() {
        EnumMap<ClassId, Map<FocusTier, DeferredHolder<Item, Item>>> result = new EnumMap<>(ClassId.class);
        for (ClassId classId : ClassId.values()) {
            EnumMap<FocusTier, DeferredHolder<Item, Item>> tiers = new EnumMap<>(FocusTier.class);
            for (FocusTier tier : FocusTier.values()) {
                ClassFocus focus = new ClassFocus(classId, tier);
                tiers.put(tier, ITEMS.register(focus.itemPath(),
                        () -> new ClassFocusItem(focus, new Item.Properties().stacksTo(1))));
            }
            result.put(classId, Map.copyOf(tiers));
        }
        return Map.copyOf(result);
    }

    public static Item item(ClassFocus focus) {
        return ITEMS_BY_FOCUS.get(focus.classId()).get(focus.tier()).get();
    }

    public static ItemStack stack(ClassFocus focus) {
        return new ItemStack(item(focus));
    }

    public static Optional<ClassFocus> identity(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (Map.Entry<ClassId, Map<FocusTier, DeferredHolder<Item, Item>>> classEntry : ITEMS_BY_FOCUS.entrySet()) {
            for (Map.Entry<FocusTier, DeferredHolder<Item, Item>> tierEntry : classEntry.getValue().entrySet()) {
                if (stack.is(tierEntry.getValue().get())) {
                    return Optional.of(new ClassFocus(classEntry.getKey(), tierEntry.getKey()));
                }
            }
        }
        return Optional.empty();
    }

    public static final class ClassFocusItem extends Item {
        private final ClassFocus focus;

        private ClassFocusItem(ClassFocus focus, Properties properties) {
            super(properties);
            this.focus = focus;
        }

        public ClassFocus focus() {
            return focus;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context,
                                    List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("tooltip.anomalousclasses.focus.identity",
                    Component.translatable("class.anomalousclasses." + focus.classId().key()),
                    Component.translatable("screen.anomalousclasses.class_focus.tier." + focus.tier().key()))
                    .withStyle(ChatFormatting.GRAY));
            if (focus.classId() == ClassId.MENTAL_DOMINATOR) {
                tooltip.add(Component.translatable("screen.anomalousclasses.class_focus.effect.max_psyche",
                        MentalDominatorRules.focusMaxPsychePercent(focus.tier())).withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("screen.anomalousclasses.class_focus.effect.upkeep",
                        MentalDominatorRules.focusUpkeepReductionPercent(focus.tier())).withStyle(ChatFormatting.AQUA));
                int intrusion = MentalDominatorRules.focusIntrusionBonusPercent(focus.tier());
                if (intrusion > 0) {
                    tooltip.add(Component.translatable("screen.anomalousclasses.class_focus.effect.intrusion",
                            intrusion).withStyle(ChatFormatting.AQUA));
                }
            }
            tooltip.add(Component.translatable("tooltip.anomalousclasses.focus.slot")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return focus.tier().index() >= FocusTier.ELDRITCH.index();
        }
    }
}
