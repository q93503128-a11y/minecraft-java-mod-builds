package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideRegionManager;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.FlatCountrysideBootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
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
        if (serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!CountrysideRegionManager.isFlatWorld(serverLevel)) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.flat_world_required"));
            return;
        }

        CountrysideWorldData data = CountrysideWorldData.get(serverLevel.getServer());
        boolean hadHomestead = data.homesteadOrigin().isPresent();
        Optional<BlockPos> homestead = FlatCountrysideBootstrap.ensureGenerated(serverLevel, player.blockPosition());
        homestead.ifPresent(origin -> RuralNpcManager.ensureForHomestead(serverLevel, origin));

        boolean firstArrival = player.addTag(STARTER_KIT_TAG);
        if (firstArrival) {
            giveOrDrop(player, ModItems.COUNTRY_KITCHEN_COUNTER.get().getDefaultInstance());
            giveOrDrop(player, ModItems.RECIPE_NOTEBOOK.get().getDefaultInstance());
            giveOrDrop(player, Items.FISHING_ROD.getDefaultInstance());
            player.sendSystemMessage(Component.translatable("message.countrysidedays.starter_kit"));
        }

        if (homestead.isPresent() && (firstArrival || !hadHomestead)) {
            BlockPos origin = homestead.get();
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.homestead_ready",
                    origin.getX(),
                    origin.getY(),
                    origin.getZ()
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

        CountrysideRegionManager.prepareAroundPlayer(serverLevel, player.blockPosition());
        if (!CountrysideRegionManager.isInsideCountryside(serverLevel, player.blockPosition())) {
            return;
        }

        serverLevel.getEntitiesOfClass(
                Monster.class,
                player.getBoundingBox().inflate(96.0),
                monster -> CountrysideRegionManager.isInsideCountryside(serverLevel, monster.blockPosition())
        ).forEach(Monster::discard);
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (CountrysideRegionManager.isInsideCountryside(serverLevel, monster.blockPosition())) {
            event.setCanceled(true);
        }
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled() || !(event.getBreaker() instanceof Player player)) {
            return;
        }
        if (!isForagePlant(event.getState())) {
            return;
        }
        if (event.getLevel().getRandom().nextFloat() >= WILD_HERB_CHANCE) {
            return;
        }

        giveOrDrop(player, ModItems.WILD_HERB.get().getDefaultInstance());
        player.sendOverlayMessage(Component.translatable("message.countrysidedays.wild_herb_found"));
    }

    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getRandom().nextFloat() >= RIVER_FISH_CHANCE) {
            return;
        }

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
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
