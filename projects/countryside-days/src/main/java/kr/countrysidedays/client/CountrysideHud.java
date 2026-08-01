package kr.countrysidedays.client;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
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
        if (player == null) return;

        Component objective = currentObjective(player);
        Component status = estateStatus(player);
        Component coordinates = estateCoordinates();
        int x = 7;
        int y = 7;
        int textWidth = Math.max(
                minecraft.font.width(objective),
                Math.max(minecraft.font.width(status), minecraft.font.width(coordinates))
        );
        int width = Math.max(156, Math.min(280, textWidth + 18));
        int height = 42;

        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x55281712);
        graphics.fill(x, y, x + width, y + height, 0xD96B4329);
        graphics.fill(x, y, x + 3, y + height, 0xFFF0B56A);
        graphics.fill(x + 3, y, x + width, y + 1, 0x99FFD99B);
        graphics.fill(x + 3, y + height - 1, x + width, y + height, 0x9942261A);
        graphics.text(minecraft.font, objective, x + 9, y + 5, 0xFFFFF0CE);
        graphics.text(minecraft.font, status, x + 9, y + 17, 0xFFFFD99B);
        graphics.text(minecraft.font, coordinates, x + 9, y + 29, 0xFFEAD9BD);
    }

    private static Component estateStatus(LocalPlayer player) {
        int coins = countItem(player, ModItems.VILLAGE_COIN.get());
        BlockPos home = ClientEstateState.home();
        BlockPos restaurant = ClientEstateState.restaurant();
        if (home == null || restaurant == null) {
            return Component.literal("마을 동전 " + coins + "  생활 구획 불러오는 중");
        }
        return Component.literal(
                "마을 동전 " + coins
                        + "  집 " + direction(player.blockPosition(), home) + " " + distance(player.blockPosition(), home) + "m"
                        + "  식당 " + direction(player.blockPosition(), restaurant) + " " + distance(player.blockPosition(), restaurant) + "m"
        );
    }

    private static Component estateCoordinates() {
        BlockPos home = ClientEstateState.home();
        BlockPos restaurant = ClientEstateState.restaurant();
        if (home == null || restaurant == null) return Component.literal("집 · 식당 위치 동기화 중");
        return Component.literal(
                "집 " + home.getX() + ", " + home.getZ()
                        + "  |  식당 " + restaurant.getX() + ", " + restaurant.getZ()
        );
    }

    private static int distance(BlockPos from, BlockPos to) {
        long dx = (long) to.getX() - from.getX();
        long dz = (long) to.getZ() - from.getZ();
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }

    private static String direction(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3) return "도착";
        String ns = dz < -3 ? "북" : dz > 3 ? "남" : "";
        String ew = dx < -3 ? "서" : dx > 3 ? "동" : "";
        return ns + ew;
    }

    private static Component currentObjective(LocalPlayer player) {
        if (hasItem(player, ModItems.COUNTRY_STEW.get())) {
            return Component.translatable("hud.countrysidedays.serve");
        }
        boolean hasHerb = hasItem(player, ModItems.WILD_HERB.get());
        boolean hasFish = hasItem(player, ModItems.RIVER_FISH.get());
        if (hasHerb && hasFish) return Component.translatable("hud.countrysidedays.cook");
        if (hasHerb) return Component.translatable("hud.countrysidedays.fish");
        return Component.translatable("hud.countrysidedays.forage");
    }

    private static boolean hasItem(LocalPlayer player, Item item) {
        return countItem(player, item) > 0;
    }

    private static int countItem(LocalPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
}
