package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.world.item.ItemStack;

/** Equipment-specific facade over the shared external/runtime item visual resolver. */
final class EquipmentVisualItemResolver {
    private EquipmentVisualItemResolver() {}

    static ItemStack stack(String visualItem) {
        return RuntimeItemVisualResolver.stack(visualItem);
    }
}
