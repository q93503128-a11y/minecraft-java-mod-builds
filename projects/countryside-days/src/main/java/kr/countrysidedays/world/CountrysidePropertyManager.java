package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.List;
import java.util.Optional;

/** Defines private plots and enforces the village's no-theft healing rule. */
public final class CountrysidePropertyManager {
    private CountrysidePropertyManager() {
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getPlayer();
        Optional<Plot> plot = plotAt(level, event.getPos());
        if (plot.isEmpty() || canModify(level, player, plot.get())) return;

        event.setCanceled(true);
        event.setNotifyClient(true);
        player.sendOverlayMessage(Component.translatable(
                "message.countrysidedays.property_break_denied",
                plot.get().displayName()
        ));
    }

    public static void onUseBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK
                || !(event.getLevel() instanceof ServerLevel level)
                || event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        Optional<BlockPos> origin = data.homesteadOrigin();
        if (origin.isPresent()
                && event.getPos().equals(origin.get().offset(-7, 1, 3))
                && event.getItemStack().is(Items.NAME_TAG)) {
            renameRestaurant(event, level, player, data, origin.get());
            return;
        }

        if (level.getBlockEntity(event.getPos()) == null) return;
        Optional<Plot> plot = plotAt(level, event.getPos());
        if (plot.isEmpty() || canModify(level, player, plot.get())) return;

        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
        player.sendOverlayMessage(Component.translatable(
                "message.countrysidedays.property_container_denied",
                plot.get().displayName()
        ));
    }

    private static void renameRestaurant(
            UseItemOnBlockEvent event,
            ServerLevel level,
            Player player,
            CountrysideWorldData data,
            BlockPos origin
    ) {
        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
        if (!data.isHomesteadOwner(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.restaurant_name_owner_only"));
            return;
        }

        ItemStack nameTag = event.getItemStack();
        if (!nameTag.has(DataComponents.CUSTOM_NAME)) {
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.restaurant_name_tag_required"));
            return;
        }

        String requested = nameTag.getHoverName().getString();
        if (!data.renameRestaurant(player.getUUID(), requested)) {
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.restaurant_name_unchanged"));
            return;
        }

        StarterHomesteadGenerator.refreshOwnershipSigns(
                level,
                origin,
                data.ownerName(),
                data.restaurantName()
        );
        if (!player.getAbilities().instabuild) nameTag.shrink(1);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.restaurant_renamed",
                data.restaurantName()
        ));
    }

    public static Optional<Plot> plotAt(ServerLevel level, BlockPos pos) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        return data.homesteadOrigin().flatMap(origin -> plots(origin).stream()
                .filter(plot -> plot.contains(pos))
                .findFirst());
    }

    public static List<Plot> plots(BlockPos origin) {
        return List.of(
                plot(origin, PlotKind.PLAYER_HOME, "내 집", -47, -35, -20, 8),
                plot(origin, PlotKind.PLAYER_RESTAURANT, "내 식당", -17, -14, 2, 8),
                plot(origin, PlotKind.PLAYER_FARM, "내 농장", 2, -13, 18, 7),
                plot(origin, PlotKind.PLAYER_RANCH, "내 목장", -8, 39, 26, 69),
                plot(origin, PlotKind.NPC_HOME, "복순 할머니네", 17, -35, 40, -9),
                plot(origin, PlotKind.NPC_HOME, "농부 한결이네", -48, 24, -21, 50),
                plot(origin, PlotKind.NPC_HOME, "목장지기 소미네", 15, 25, 40, 52),
                plot(origin, PlotKind.PUBLIC_PROTECTED, "마을회관", -15, 24, 15, 50),
                plot(origin, PlotKind.PUBLIC_PROTECTED, "마을 장터", -23, 7, 22, 27),
                plot(origin, PlotKind.PUBLIC_PROTECTED, "공동 과수원", -48, 38, -7, 70)
        );
    }

    private static Plot plot(
            BlockPos origin,
            PlotKind kind,
            String displayName,
            int minX,
            int minZ,
            int maxX,
            int maxZ
    ) {
        return new Plot(kind, displayName, origin.offset(minX, -6, minZ), origin.offset(maxX, 18, maxZ));
    }

    private static boolean canModify(ServerLevel level, Player player, Plot plot) {
        if (player.canUseGameMasterBlocks()) return true;
        if (!plot.kind().isPlayerOwned()) return false;
        return CountrysideWorldData.get(level.getServer()).isHomesteadOwner(player.getUUID());
    }

    public enum PlotKind {
        PLAYER_HOME(true),
        PLAYER_RESTAURANT(true),
        PLAYER_FARM(true),
        PLAYER_RANCH(true),
        NPC_HOME(false),
        PUBLIC_PROTECTED(false);

        private final boolean playerOwned;

        PlotKind(boolean playerOwned) {
            this.playerOwned = playerOwned;
        }

        public boolean isPlayerOwned() {
            return playerOwned;
        }
    }

    public record Plot(PlotKind kind, String displayName, BlockPos min, BlockPos max) {
        public boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
    }
}
