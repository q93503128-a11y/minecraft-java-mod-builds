package kr.moonseungjun.turnboundre.progression;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class M6EquipmentInventoryTransactionTest {
    @Test
    void uncommittedExtractionRollsBackAndCommittedExtractionConsumesExactMaterial() {
        NonNullList<ItemStack> stacks = NonNullList.withSize(4, ItemStack.EMPTY);
        stacks.set(0, new ItemStack(Items.IRON_INGOT, 4));
        stacks.set(1, new ItemStack(Items.COPPER_INGOT, 11));
        stacks.set(2, new ItemStack(Items.IRON_INGOT, 5));
        ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(stacks);

        assertEquals(9, EquipmentForgeService.countItem(inventory, Items.IRON_INGOT));
        assertEquals(11, EquipmentForgeService.countItem(inventory, Items.COPPER_INGOT));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(6, EquipmentForgeService.extractItem(inventory, Items.IRON_INGOT, 6, transaction));
            assertEquals(3, EquipmentForgeService.countItem(inventory, Items.IRON_INGOT));
        }
        assertEquals(9, EquipmentForgeService.countItem(inventory, Items.IRON_INGOT));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(6, EquipmentForgeService.extractItem(inventory, Items.IRON_INGOT, 6, transaction));
            transaction.commit();
        }
        assertEquals(3, EquipmentForgeService.countItem(inventory, Items.IRON_INGOT));
        assertEquals(11, EquipmentForgeService.countItem(inventory, Items.COPPER_INGOT));
    }

    @Test
    void partialExtractionAlsoRollsBackWhenExactCostCannotBeMet() {
        NonNullList<ItemStack> stacks = NonNullList.withSize(2, ItemStack.EMPTY);
        stacks.set(0, new ItemStack(Items.GOLD_INGOT, 3));
        ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(stacks);

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(3, EquipmentForgeService.extractItem(inventory, Items.GOLD_INGOT, 4, transaction));
            assertEquals(0, EquipmentForgeService.countItem(inventory, Items.GOLD_INGOT));
        }
        assertEquals(3, EquipmentForgeService.countItem(inventory, Items.GOLD_INGOT));
    }
}
