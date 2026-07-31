package kr.moonseungjun.livingkingdoms.profile;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StarterLoadoutManager {
    private StarterLoadoutManager() {
    }

    public static void grant(ServerPlayer player, OriginProfile profile) {
        give(player, new ItemStack(Items.BREAD, 4));

        switch (profile.backgroundId()) {
            case "fisher_family" -> {
                give(player, new ItemStack(Items.FISHING_ROD));
                give(player, new ItemStack(Items.COD, 3));
                give(player, new ItemStack(Items.OAK_BOAT));
            }
            case "wanderer" -> {
                give(player, new ItemStack(Items.COMPASS));
                give(player, new ItemStack(Items.CAMPFIRE));
                give(player, new ItemStack(Items.LEATHER_BOOTS));
            }
            case "scholar_student" -> {
                give(player, new ItemStack(Items.WRITABLE_BOOK));
                give(player, new ItemStack(Items.SPYGLASS));
                give(player, new ItemStack(Items.PAPER, 8));
            }
            default -> {
                give(player, new ItemStack(Items.EMERALD, 3));
                give(player, new ItemStack(Items.WOODEN_HOE));
            }
        }

        switch (profile.speciesId()) {
            case "elf" -> {
                give(player, new ItemStack(Items.BOW));
                give(player, new ItemStack(Items.ARROW, 12));
            }
            case "dwarf" -> {
                give(player, new ItemStack(Items.STONE_PICKAXE));
                give(player, new ItemStack(Items.TORCH, 16));
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
