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
        CountrysideDays.LOGGER.info("Countryside Days rural objective HUD registered");
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        Component objective = currentObjective(player);
        int x = 7;
        int y = 7;
        int width = Math.max(82, Math.min(148, minecraft.font.width(objective) + 17));
        int height = 20;
        graphics.fill(x, y, x + width, y + height, 0xA826211A);
        graphics.fill(x, y, x + 3, y + height, 0xFFD7AE67);
        graphics.fill(x + 3, y, x + width, y + 1, 0x705C452F);
        graphics.text(minecraft.font, objective, x + 9, y + 6, 0xFFE8E1C7);
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
