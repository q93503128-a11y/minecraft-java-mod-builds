package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Resolves server-authored equipment visual ids to Mojang runtime item models without TURNBOUND-owned placeholder art. */
final class EquipmentVisualItemResolver {
    private EquipmentVisualItemResolver() {}

    static ItemStack stack(String visualItem) {
        if (visualItem == null || visualItem.isBlank()) return ItemStack.EMPTY;
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(visualItem));
            if (item == null || item == Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(item);
        } catch (RuntimeException ignored) {
            // Server data validation should reject malformed ids; presentation still fails closed instead of crashing the UI.
            return ItemStack.EMPTY;
        }
    }
}
