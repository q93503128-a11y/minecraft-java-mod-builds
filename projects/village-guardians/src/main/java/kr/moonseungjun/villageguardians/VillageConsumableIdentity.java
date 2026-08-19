package kr.moonseungjun.villageguardians;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Server-authored identity for tactical consumables; display names alone never grant gameplay effects. */
public final class VillageConsumableIdentity {
    private static final String KEY_MARKER = "villageguardians_consumable";
    private static final String KEY_ID = "villageguardians_consumable_id";

    private VillageConsumableIdentity() {}

    public static void stamp(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = tagCopy(stack);
        tag.putBoolean(KEY_MARKER, true);
        tag.putString(KEY_ID, id == null ? "" : id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String id(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return "";
        CompoundTag tag = data.copyTag();
        if (!tag.getBooleanOr(KEY_MARKER, false)) return "";
        return tag.getStringOr(KEY_ID, "");
    }

    private static CompoundTag tagCopy(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        return existing == null ? new CompoundTag() : existing.copyTag();
    }
}
