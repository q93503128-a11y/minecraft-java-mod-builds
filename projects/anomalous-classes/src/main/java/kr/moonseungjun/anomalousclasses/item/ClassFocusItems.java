package kr.moonseungjun.anomalousclasses.item;

import java.util.EnumMap;
import java.util.Map;
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
        public boolean isFoil(ItemStack stack) {
            return focus.tier().index() >= FocusTier.ELDRITCH.index();
        }
    }
}
