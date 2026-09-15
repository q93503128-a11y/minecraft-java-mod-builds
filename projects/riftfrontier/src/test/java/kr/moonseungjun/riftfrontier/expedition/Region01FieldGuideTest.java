package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01FieldGuideTest {
    @Test
    void buildsVanillaWrittenBookFromCanonicalRegion01Rules() {
        ItemStack guide = Region01FieldGuide.create();
        assertTrue(guide.is(Items.WRITTEN_BOOK));

        WrittenBookContent content = guide.get(DataComponents.WRITTEN_BOOK_CONTENT);
        assertNotNull(content);
        assertEquals(Region01FieldGuide.TITLE, content.title().raw());
        assertEquals(Region01FieldGuide.AUTHOR, content.author());
        assertEquals(0, content.generation());
        assertTrue(content.resolved());
        assertEquals(Region01FieldGuide.PAGE_KEYS.size(), content.getPages(false).size());
        assertEquals(
            Component.translatable(Region01FieldGuide.PAGE_KEYS.getFirst()),
            content.getPages(false).getFirst()
        );
    }
}
