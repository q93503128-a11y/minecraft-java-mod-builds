package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Resolves external/Mojang runtime item ids without introducing TURNBOUND-owned placeholder art. */
final class RuntimeItemVisualResolver {
    private RuntimeItemVisualResolver() {}

    static ItemStack stack(String visualItem) {
        if (visualItem == null || visualItem.isBlank()) return ItemStack.EMPTY;
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(visualItem));
            if (item == null || item == Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(item);
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }
}
