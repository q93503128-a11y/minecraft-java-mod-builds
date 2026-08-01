package kr.moonseungjun.livingkingdoms.profile;

import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Grants social identity and travel equipment, not a compressed vanilla survival tech tree. */
public final class StarterLoadoutManager {
    private StarterLoadoutManager() {
    }

    public static void grant(ServerPlayer player, OriginProfile profile) {
        RealmEconomyManager.account(player);
        give(player, new ItemStack(Items.COMPASS));
        give(player, new ItemStack(Items.WRITABLE_BOOK));
        give(player, new ItemStack(Items.TORCH, 8));

        switch (profile.backgroundId()) {
            case "fisher_family" -> {
                give(player, new ItemStack(Items.FISHING_ROD));
                give(player, new ItemStack(Items.OAK_BOAT));
                give(player, new ItemStack(Items.COD, 3));
            }
            case "wanderer" -> {
                give(player, new ItemStack(Items.SPYGLASS));
                give(player, new ItemStack(Items.LEATHER_BOOTS));
                give(player, new ItemStack(Items.LEAD, 2));
            }
            case "scholar_student" -> {
                give(player, new ItemStack(Items.SPYGLASS));
                give(player, new ItemStack(Items.PAPER, 8));
                give(player, new ItemStack(Items.INK_SAC, 2));
            }
            default -> {
                give(player, new ItemStack(Items.LEATHER_BOOTS));
                give(player, new ItemStack(Items.PAPER, 4));
                give(player, new ItemStack(Items.LEATHER, 2));
            }
        }

        switch (profile.speciesId()) {
            case "elf" -> {
                give(player, new ItemStack(Items.BOW));
                give(player, new ItemStack(Items.ARROW, 12));
            }
            case "dwarf" -> {
                give(player, new ItemStack(Items.SHIELD));
                give(player, new ItemStack(Items.COAL, 2));
            }
            default -> give(player, new ItemStack(Items.LEATHER_HELMET));
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
