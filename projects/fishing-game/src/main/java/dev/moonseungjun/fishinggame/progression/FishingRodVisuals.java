package dev.moonseungjun.fishinggame.progression;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FishingRodVisuals {
    private FishingRodVisuals() {
    }

    public static void ensureEquipped(ServerPlayer player, RodDefinition rod) {
        ensureEquipped(player, rod, 0);
    }

    public static void ensureEquipped(ServerPlayer player, RodDefinition rod, int rebirths) {
        ItemStack stack = player.getInventory().getItem(0);
        if (!stack.is(Items.FISHING_ROD)) {
            stack = new ItemStack(Items.FISHING_ROD);
            player.getInventory().setItem(0, stack);
        }
        apply(stack, rod, rebirths);
    }

    public static void refreshEquipped(ServerPlayer player, RodDefinition rod) {
        refreshEquipped(player, rod, 0);
    }

    public static void refreshEquipped(ServerPlayer player, RodDefinition rod, int rebirths) {
        ItemStack stack = player.getInventory().getItem(0);
        if (stack.is(Items.FISHING_ROD)) {
            apply(stack, rod, rebirths);
        }
    }

    public static void apply(ItemStack stack, RodDefinition rod) {
        apply(stack, rod, 0);
    }

    public static void apply(ItemStack stack, RodDefinition rod, int rebirths) {
        if (!stack.is(Items.FISHING_ROD)) return;

        stack.set(DataComponents.ITEM_MODEL, FishingGameMod.id("rod/" + rod.id()));
        String name = rod.displayName();
        if (rebirths > 0) name += " · 환생 " + rebirths;
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));

        if (rod.tier() >= FishingRods.maxTier() || rebirths > 0) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }
}
