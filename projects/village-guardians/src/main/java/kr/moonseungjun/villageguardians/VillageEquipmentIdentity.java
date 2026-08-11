package kr.moonseungjun.villageguardians;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class VillageEquipmentIdentity {
    private static final String KEY_MARKER = "villageguardians_equipment";
    private static final String KEY_RARITY = "villageguardians_rarity";
    private static final String KEY_ENHANCEMENT = "villageguardians_enhancement";
    private static final String KEY_OFFER = "villageguardians_offer";

    private VillageEquipmentIdentity() {}

    public static void stampRarity(ItemStack stack, String rarity, int enhancement) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = tagCopy(stack);
        tag.putBoolean(KEY_MARKER, true);
        tag.putString(KEY_RARITY, rarity == null ? "" : rarity);
        tag.putInt(KEY_ENHANCEMENT, Math.max(0, enhancement));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void stampOffer(ItemStack stack, String offerId) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = tagCopy(stack);
        tag.putBoolean(KEY_MARKER, true);
        tag.putString(KEY_OFFER, offerId == null ? "" : offerId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean stamped(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(KEY_MARKER, false);
    }

    public static String rarity(ItemStack stack) {
        if (!stamped(stack)) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getStringOr(KEY_RARITY, "");
    }

    public static int enhancement(ItemStack stack) {
        if (!stamped(stack)) return -1;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? -1 : Math.max(0, data.copyTag().getIntOr(KEY_ENHANCEMENT, 0));
    }

    public static String offer(ItemStack stack) {
        if (!stamped(stack)) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getStringOr(KEY_OFFER, "");
    }

    public static boolean canReadLegacyName(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stamped(stack)) return false;
        Integer repairCost = stack.get(DataComponents.REPAIR_COST);
        return repairCost == null || repairCost <= 0;
    }

    private static CompoundTag tagCopy(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        return existing == null ? new CompoundTag() : existing.copyTag();
    }
}
