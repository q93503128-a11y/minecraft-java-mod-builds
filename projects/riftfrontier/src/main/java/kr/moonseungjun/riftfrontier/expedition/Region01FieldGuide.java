package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;

/**
 * Minecraft-native first-slice field guide.
 *
 * <p>This deliberately uses the vanilla written-book surface instead of inventing a Riftfrontier Screen before the
 * final UI language is reviewed. Its pages only explain already-authoritative Region 01 rules; the book owns no
 * gameplay state and cannot mutate expedition progression.</p>
 */
public final class Region01FieldGuide {
    static final String TITLE = "Riftfrontier Field Guide";
    static final String AUTHOR = "Riftfrontier";
    static final List<String> PAGE_KEYS = List.of(
        "riftfrontier.guide.region_01.page.overview",
        "riftfrontier.guide.region_01.page.patrol",
        "riftfrontier.guide.region_01.page.salvage",
        "riftfrontier.guide.region_01.page.logistics"
    );

    private Region01FieldGuide() {}

    public static ItemStack create() {
        List<Filterable<Component>> pages = PAGE_KEYS.stream()
            .map(key -> Filterable.passThrough(Component.translatable(key)))
            .toList();

        ItemStack guide = new ItemStack(Items.WRITTEN_BOOK);
        guide.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
            Filterable.passThrough(TITLE),
            AUTHOR,
            0,
            pages,
            true
        ));
        guide.set(DataComponents.CUSTOM_NAME, Component.translatable("riftfrontier.guide.region_01.name"));
        return guide;
    }

    /**
     * Supplies the guide during the one-time fresh-world bootstrap. No persistent "guide received" flag is added:
     * bootstrap itself is already authoritative and only succeeds before expedition history exists.
     */
    public static void giveInitial(ServerPlayer player) {
        ItemStack guide = create();
        if (!player.getInventory().add(guide)) {
            player.drop(guide, false);
        }
    }
}
