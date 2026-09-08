package kr.moonseungjun.turnboundre.world;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Gives and identifies the vanilla-book-backed Expedition Journal without adding a key binding. */
public final class ExpeditionJournalAccess {
    private static final Component JOURNAL_NAME = Component.translatable("item.turnbound_re.expedition_journal");

    private ExpeditionJournalAccess() {}

    public static boolean isJournal(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.BOOK)) return false;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return JOURNAL_NAME.equals(name);
    }

    public static void ensure(ServerPlayer player) {
        if (player == null) return;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (isJournal(inventory.getItem(slot))) return;
        }
        ItemStack journal = new ItemStack(Items.BOOK);
        journal.set(DataComponents.CUSTOM_NAME, JOURNAL_NAME);
        if (!inventory.add(journal)) player.drop(journal, false);
    }
}
