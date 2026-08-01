package kr.countrysidedays.client;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = CountrysideDays.MOD_ID, value = Dist.CLIENT)
public final class CountrysideHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            CountrysideDays.MOD_ID,
            "rural_status"
    );
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private CountrysideHud() {
    }

    @SubscribeEvent
    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, (graphics, deltaTracker) -> render(graphics));
        CountrysideDays.LOGGER.info("Countryside Days compact navigation HUD registered");
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        drawPanel(graphics, 7, 7, currentObjective(player), 0xD96B4329, 0xFFF0B56A);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        Component currency = Component.literal("◆ " + countItem(player, ModItems.VILLAGE_COIN.get()));
        int currencyWidth = Math.max(58, minecraft.font.width(currency) + 18);
        drawFixedPanel(
                graphics,
                screenWidth - currencyWidth - 7,
                7,
                currencyWidth,
                currency,
                0xD9493A2A,
                0xFFFFC96B
        );

        BlockPos home = ClientEstateState.home();
        BlockPos restaurant = ClientEstateState.restaurant();
        Component navigation = home == null || restaurant == null
                ? Component.literal("생활 구획 동기화 중")
                : Component.literal(
                        "집 " + relativeArrow(player, home) + " " + distance(player.blockPosition(), home) + "m"
                                + "   식당 " + relativeArrow(player, restaurant) + " "
                                + distance(player.blockPosition(), restaurant) + "m"
                );
        int navWidth = Math.max(142, minecraft.font.width(navigation) + 18);
        drawFixedPanel(
                graphics,
                screenWidth - navWidth - 7,
                31,
                navWidth,
                navigation,
                0xD9383026,
                0xFFD8B979
        );
    }

    private static void drawPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Component text,
            int background,
            int accent
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(108, minecraft.font.width(text) + 18);
        drawFixedPanel(graphics, x, y, width, text, background, accent);
    }

    private static void drawFixedPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            Component text,
            int background,
            int accent
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int height = 20;
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x44201712);
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + 3, y + height, accent);
        graphics.fill(x + 3, y, x + width, y + 1, 0x66FFF0C8);
        graphics.text(minecraft.font, text, x + 9, y + 6, 0xFFFFF2D2);
    }

    private static String relativeArrow(LocalPlayer player, BlockPos target) {
        double dx = target.getX() + 0.5 - player.getX();
        double dz = target.getZ() + 0.5 - player.getZ();
        if (dx * dx + dz * dz <= 9.0) return "●";

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float delta = Mth.wrapDegrees(targetYaw - player.getYRot());
        int index = Math.floorMod(Mth.floor((delta + 22.5F) / 45.0F), 8);
        return ARROWS[index];
    }

    private static int distance(BlockPos from, BlockPos to) {
        long dx = (long) to.getX() - from.getX();
        long dz = (long) to.getZ() - from.getZ();
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
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
