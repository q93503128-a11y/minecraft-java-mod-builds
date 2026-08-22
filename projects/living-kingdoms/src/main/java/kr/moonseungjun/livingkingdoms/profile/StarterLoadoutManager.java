package kr.moonseungjun.livingkingdoms.profile;

import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Grants the fixed civic starter kit for the current Erden-only playable slice. */
public final class StarterLoadoutManager {
    private StarterLoadoutManager() {
    }

    public static void grant(ServerPlayer player, OriginProfile profile) {
        if (!PlayableOriginCatalog.DEFAULT_SPECIES.equals(profile.speciesId())
                || !PlayableOriginCatalog.DEFAULT_HOMELAND.equals(profile.homelandId())
                || !PlayableOriginCatalog.DEFAULT_BACKGROUND.equals(profile.backgroundId())
                || !PlayableOriginCatalog.DEFAULT_RESIDENCE.equals(profile.residenceId())) {
            throw new IllegalStateException("Inactive origin reached Erden starter loadout: " + profile);
        }

        RealmEconomyManager.account(player);
        give(player, new ItemStack(Items.COMPASS));
        give(player, new ItemStack(Items.WRITABLE_BOOK));
        give(player, new ItemStack(Items.TORCH, 8));
        give(player, new ItemStack(Items.LEATHER_BOOTS));
        give(player, new ItemStack(Items.PAPER, 4));
        give(player, new ItemStack(Items.LEATHER, 2));
        give(player, new ItemStack(Items.LEATHER_HELMET));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
