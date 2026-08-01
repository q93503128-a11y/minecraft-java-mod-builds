package kr.countrysidedays.world;

import kr.countrysidedays.gameplay.SharedRestaurantAccess;
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

import java.util.Optional;

/** Enforces private estates, protected resident homes and unbreakable public facilities. */
public final class CountrysidePropertyManager {
    private CountrysidePropertyManager() {
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getPlayer();
        Optional<Plot> plot = plotAt(level, event.getPos());
        if (plot.isEmpty() || canModify(player, plot.get())) return;

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
                || event.getPlayer() == null) return;

        Player player = event.getPlayer();
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        Optional<CountrysideWorldData.PlayerEstate> sharedRestaurant = SharedRestaurantAccess.restaurantEstate(data);

        if (sharedRestaurant.isPresent()) {
            BlockPos origin = sharedRestaurant.get().originPos();
            if (isAutomaticRestaurantAccess(origin, event.getPos())) {
                event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
                player.sendOverlayMessage(Component.translatable(
                        "message.countrysidedays.restaurant_access_automatic"
                ));
                return;
            }
            if (event.getPos().equals(PlayerEstateLayout.restaurantSign(origin))
                    && event.getItemStack().is(Items.NAME_TAG)) {
                renameRestaurant(event, level, player, data, sharedRestaurant.get());
                return;
            }
            if (PlayerEstateLayout.isRestaurantArea(origin, event.getPos())
                    && SharedRestaurantAccess.isStaff(data, player.getUUID())) {
                return;
            }
        }

        Optional<Plot> plot = plotAt(level, event.getPos());
        if (plot.isEmpty() || canModify(player, plot.get())) return;

        boolean privateEstate = plot.get().ownerUuid().isPresent();
        boolean protectedContainer = level.getBlockEntity(event.getPos()) != null;
        if (!privateEstate && !protectedContainer) return;

        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
        player.sendOverlayMessage(Component.translatable(
                "message.countrysidedays.property_container_denied",
                plot.get().displayName()
        ));
    }

    private static boolean isAutomaticRestaurantAccess(BlockPos origin, BlockPos pos) {
        BlockPos gate = PlayerEstateLayout.restaurantGate(origin);
        BlockPos door = PlayerEstateLayout.restaurantDoor(origin);
        return pos.equals(gate) || pos.equals(door) || pos.equals(door.above());
    }

    private static void renameRestaurant(
            UseItemOnBlockEvent event,
            ServerLevel level,
            Player player,
            CountrysideWorldData data,
            CountrysideWorldData.PlayerEstate estate
    ) {
        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
        if (!SharedRestaurantAccess.isOwner(data, player.getUUID())) {
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

        CountrysideWorldData.PlayerEstate updated = data.estate(player.getUUID()).orElse(estate);
        SharedRestaurantBuilder.refreshSign(
                level, updated.originPos(), updated.ownerName(), updated.restaurantName()
        );
        if (!player.getAbilities().instabuild) nameTag.shrink(1);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.restaurant_renamed",
                updated.restaurantName()
        ));
    }

    public static Optional<Plot> plotAt(ServerLevel level, BlockPos pos) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        Optional<CountrysideWorldData.PlayerEstate> estate = data.estateAt(pos);
        if (estate.isPresent()) {
            CountrysideWorldData.PlayerEstate value = estate.get();
            return Optional.of(new Plot(
                    value.ownerName() + "의 생활 구획",
                    Optional.of(value.ownerUuid()),
                    value.originPos().offset(PlayerEstateLayout.MIN_X, -6, PlayerEstateLayout.MIN_Z),
                    value.originPos().offset(PlayerEstateLayout.MAX_X, 18, PlayerEstateLayout.MAX_Z)
            ));
        }

        return data.homesteadOrigin().flatMap(origin -> publicPlot(origin, pos));
    }

    private static Optional<Plot> publicPlot(BlockPos origin, BlockPos pos) {
        BlockPos min = origin.offset(-StarterHomesteadGenerator.PUBLIC_HALF_WIDTH, -6,
                -StarterHomesteadGenerator.PUBLIC_HALF_DEPTH);
        BlockPos max = origin.offset(StarterHomesteadGenerator.PUBLIC_HALF_WIDTH, 18,
                StarterHomesteadGenerator.PUBLIC_HALF_DEPTH);
        Plot publicVillage = new Plot("공공 마을 시설", Optional.empty(), min, max);
        return publicVillage.contains(pos) ? Optional.of(publicVillage) : Optional.empty();
    }

    private static boolean canModify(Player player, Plot plot) {
        if (player.canUseGameMasterBlocks()) return true;
        return plot.ownerUuid().map(owner -> owner.equals(player.getUUID().toString())).orElse(false);
    }

    public record Plot(String displayName, Optional<String> ownerUuid, BlockPos min, BlockPos max) {
        public boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
    }
}
