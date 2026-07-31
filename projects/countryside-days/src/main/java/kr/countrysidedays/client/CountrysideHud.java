package kr.countrysidedays.client;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = CountrysideDays.MOD_ID, value = Dist.CLIENT)
public final class CountrysideHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(CountrysideDays.MOD_ID, "rural_objective");

    private CountrysideHud() {
    }

    @SubscribeEvent
    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, (graphics, deltaTracker) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        int x = 8;
        int y = 8;
        int width = 206;
        int height = 38;
        graphics.fill(x, y, x + width, y + height, 0xC0282018);
        graphics.fill(x, y, x + 4, y + height, 0xFFE1B56D);
        graphics.fill(x + 4, y, x + width, y + 2, 0x806B4B2C);
        graphics.text(minecraft.font, Component.translatable("hud.countrysidedays.title"), x + 11, y + 7, 0xFFF5E7C9);
        graphics.text(minecraft.font, currentObjective(player), x + 11, y + 22, 0xFFD7E9C2);
    }

    private static Component currentObjective(LocalPlayer player) {
        if (hasItem(player, ModItems.COUNTRY_STEW.get())) {
            return Component.translatable("hud.countrysidedays.serve");
        }
        boolean hasHerb = hasItem(player, ModItems.WILD_HERB.get());
        boolean hasFish = hasItem(player, ModItems.RIVER_FISH.get());
        if (hasHerb && hasFish) {
            return Component.translatable("hud.countrysidedays.cook");
        }
        if (hasHerb) {
            return Component.translatable("hud.countrysidedays.fish");
        }
        return Component.translatable("hud.countrysidedays.forage");
    }

    private static boolean hasItem(LocalPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }
}
