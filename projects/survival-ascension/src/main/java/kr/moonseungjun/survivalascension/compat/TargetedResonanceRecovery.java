package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Physical target selection for Deep-operation Resonance recovery.
 *
 * Players do not configure another menu or currency. The equipment category held in either hand at
 * extraction acts as the recovery focus. If the optional content pack contains a matching tagged
 * Resonance item, that category is returned; otherwise the existing general pool remains the fallback.
 */
public final class TargetedResonanceRecovery {
    private static final TagKey<Item> REWARD_POOL = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_resonance_rewards")
    );

    private TargetedResonanceRecovery() {}

    public static Recovery select(RandomSource random, ItemStack mainHand, ItemStack offHand) {
        Focus focus = focusOf(mainHand);
        if (focus == null) focus = focusOf(offHand);
        if (focus != null) {
            List<Item> matches = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (item == null || !item.builtInRegistryHolder().is(REWARD_POOL)) continue;
                ItemStack candidate = new ItemStack(item);
                if (candidate.isEmpty() || candidate.getMaxStackSize() != 1 || !focus.matches(candidate)) continue;
                matches.add(item);
            }
            if (!matches.isEmpty()) {
                return new Recovery(new ItemStack(matches.get(random.nextInt(matches.size()))), focus.koreanLabel(), true);
            }
        }

        ItemStack fallback = ContentPackCompatibility.randomResonanceOperationReward(random);
        return new Recovery(fallback, focus == null ? "무초점 무작위 회수" : focus.koreanLabel() + " 후보 없음 → 무작위 회수", false);
    }

    public static String describeFocus(ItemStack mainHand, ItemStack offHand) {
        Focus focus = focusOf(mainHand);
        if (focus == null) focus = focusOf(offHand);
        return focus == null ? "무초점(무작위)" : focus.koreanLabel();
    }

    private static Focus focusOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.is(ItemTags.PICKAXES)) return Focus.PICKAXE;
        if (stack.is(ItemTags.AXES)) return Focus.AXE;
        if (stack.is(ItemTags.SHOVELS)) return Focus.SHOVEL;
        if (stack.is(ItemTags.HOES)) return Focus.HOE;
        if (stack.is(ItemTags.SWORDS)) return Focus.SWORD;
        if (stack.is(ItemTags.HEAD_ARMOR)) return Focus.HELMET;
        if (stack.is(ItemTags.CHEST_ARMOR)) return Focus.CHESTPLATE;
        if (stack.is(ItemTags.LEG_ARMOR)) return Focus.LEGGINGS;
        if (stack.is(ItemTags.FOOT_ARMOR)) return Focus.BOOTS;
        return null;
    }

    private enum Focus {
        PICKAXE("곡괭이 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.PICKAXES); } },
        AXE("도끼 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.AXES); } },
        SHOVEL("삽 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.SHOVELS); } },
        HOE("괭이 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.HOES); } },
        SWORD("검 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.SWORDS); } },
        HELMET("투구 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.HEAD_ARMOR); } },
        CHESTPLATE("흉갑 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.CHEST_ARMOR); } },
        LEGGINGS("각반 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.LEG_ARMOR); } },
        BOOTS("장화 초점") { @Override boolean matches(ItemStack stack) { return stack.is(ItemTags.FOOT_ARMOR); } };

        private final String koreanLabel;

        Focus(String koreanLabel) { this.koreanLabel = koreanLabel; }
        String koreanLabel() { return koreanLabel; }
        abstract boolean matches(ItemStack stack);
    }

    public record Recovery(ItemStack stack, String focusLabel, boolean focused) {}
}
