package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideRegionManager;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.FlatCountrysideBootstrap;
import kr.countrysidedays.world.StarterHomesteadGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

public final class RuralGameplayHandler {
    private static final String STARTER_KIT_TAG = "countrysidedays_starter_kit";
    private static final float WILD_HERB_CHANCE = 0.32F;
    private static final float RIVER_FISH_CHANCE = 0.45F;

    private RuralGameplayHandler() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        enforceHealingRules(serverLevel);
        if (serverLevel.dimension() != Level.OVERWORLD) return;
        if (!CountrysideRegionManager.isFlatWorld(serverLevel)) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.flat_world_required"));
            return;
        }

        CountrysideWorldData data = CountrysideWorldData.get(serverLevel.getServer());
        boolean hadHomestead = data.homesteadOrigin().isPresent();
        Optional<BlockPos> homestead = FlatCountrysideBootstrap.ensureGenerated(serverLevel, player.blockPosition());
        boolean becameOwner = data.claimHomesteadOwner(player.getUUID(), player.getScoreboardName());
        homestead.ifPresent(origin -> {
            RuralNpcManager.ensureForHomestead(serverLevel, origin);
            StarterHomesteadGenerator.refreshOwnershipSigns(
                    serverLevel,
                    origin,
                    data.ownerName(),
                    data.restaurantName()
            );
        });

        boolean firstArrival = player.addTag(STARTER_KIT_TAG);
        if (firstArrival) {
            giveOrDrop(player, ModItems.RECIPE_NOTEBOOK.get().getDefaultInstance());
            giveOrDrop(player, Items.FISHING_ROD.getDefaultInstance());
            player.sendSystemMessage(Component.translatable("message.countrysidedays.starter_kit"));
        }

        if (homestead.isPresent() && (firstArrival || !hadHomestead || becameOwner)) {
            BlockPos origin = homestead.get();
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.homestead_ready",
                    origin.getX(), origin.getY(), origin.getZ()
            ));
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.property_deed",
                    origin.getX() - 34, origin.getY(), origin.getZ() - 25,
                    origin.getX() + 10, origin.getY(), origin.getZ() - 4,
                    origin.getX() - 7, origin.getY(), origin.getZ() - 5,
                    origin.getX() + 9, origin.getY(), origin.getZ() + 52
            ));
            player.sendSystemMessage(Component.translatable("message.countrysidedays.meet_resident"));
            player.sendSystemMessage(Component.translatable("message.countrysidedays.keep_inventory_enabled"));
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel serverLevel)
                || serverLevel.dimension() != Level.OVERWORLD
                || !CountrysideRegionManager.isFlatWorld(serverLevel)
                || player.tickCount % 40 != 0) {
            return;
        }

        serverLevel.getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(128.0),
                mob -> mob instanceof Enemy
        ).forEach(Mob::discard);
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof Enemy)
                || !CountrysideRegionManager.isInsideCountryside(serverLevel, event.getEntity().blockPosition())) {
            return;
        }
        event.setCanceled(true);
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof Player player)) return;
        if (!isForagePlant(event.getState())) return;
        if (event.getLevel().getRandom().nextFloat() >= WILD_HERB_CHANCE) return;

        giveOrDrop(player, ModItems.WILD_HERB.get().getDefaultInstance());
        player.sendOverlayMessage(Component.translatable("message.countrysidedays.wild_herb_found"));
    }

    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getRandom().nextFloat() >= RIVER_FISH_CHANCE) return;

        giveOrDrop(player, ModItems.RIVER_FISH.get().getDefaultInstance());
        player.sendOverlayMessage(Component.translatable("message.countrysidedays.river_fish_caught"));
    }

    public static boolean isForagePlant(BlockState state) {
        return state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN);
    }

    private static void enforceHealingRules(ServerLevel level) {
        level.getServer().getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
}
