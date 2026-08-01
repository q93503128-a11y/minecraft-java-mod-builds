package kr.countrysidedays.gameplay;

import kr.countrysidedays.network.EstateHudPayload;
import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideRegionManager;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.FlatCountrysideBootstrap;
import kr.countrysidedays.world.PlayerEstateLayout;
import kr.countrysidedays.world.SharedRestaurantBuilder;
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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public final class RuralGameplayHandler {
    private static final String STARTER_KIT_TAG = "countrysidedays_starter_kit_alpha10";
    private static final float WILD_HERB_CHANCE = 0.32F;
    private static final float RIVER_FISH_CHANCE = 0.45F;

    private RuralGameplayHandler() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel serverLevel)) return;

        enforceHealingRules(serverLevel);
        if (serverLevel.dimension() != Level.OVERWORLD) return;
        if (!CountrysideRegionManager.isFlatWorld(serverLevel)) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.flat_world_required"));
            return;
        }

        CountrysideWorldData data = CountrysideWorldData.get(serverLevel.getServer());
        Optional<BlockPos> village = FlatCountrysideBootstrap.ensureGenerated(serverLevel, player.blockPosition());
        if (village.isEmpty()) return;

        BlockPos villageOrigin = village.get();
        RuralNpcManager.ensurePublicVillage(serverLevel, villageOrigin);

        CountrysideWorldData.EstateAllocation allocation = data.ensureEstate(
                player.getUUID(), player.getScoreboardName(), villageOrigin
        );
        CountrysideWorldData.PlayerEstate estate = allocation.estate();
        BlockPos estateOrigin = estate.originPos();
        if (allocation.created()) {
            StarterHomesteadGenerator.buildPlayerEstate(
                    serverLevel, estateOrigin, estate.ownerName(), estate.restaurantName()
            );
            VillageLifeManager.prepareNewEstate(serverLevel, estateOrigin);
            StarterHomesteadGenerator.connectEstateToVillage(serverLevel, villageOrigin, estateOrigin);
        } else {
            StarterHomesteadGenerator.refreshEstateSigns(
                    serverLevel, estateOrigin, estate.ownerName(), estate.restaurantName()
            );
        }

        CountrysideWorldData.PlayerEstate sharedRestaurant = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(estate);
        boolean restaurantOpenNow = sharedRestaurant.restaurantOpen()
                && RuralNpcManager.isRestaurantBusinessTime(serverLevel);
        if (sharedRestaurant.restaurantOpen() && !restaurantOpenNow) {
            SharedRestaurantAccess.setOpen(data, false);
        }
        for (CountrysideWorldData.PlayerEstate existingEstate : data.estates()) {
            boolean isSharedRestaurant = existingEstate.ownerUuid().equals(sharedRestaurant.ownerUuid());
            SharedRestaurantBuilder.normalizeEstate(
                    serverLevel,
                    existingEstate,
                    isSharedRestaurant,
                    isSharedRestaurant && restaurantOpenNow
            );
        }

        RuralNpcManager.ensureEstateAnimals(serverLevel, estate);
        BlockPos ranchSupply = PlayerEstateLayout.ranchSupplyBarrel(estateOrigin);
        if (!serverLevel.getBlockState(ranchSupply).is(Blocks.BARREL)) {
            serverLevel.setBlock(ranchSupply, Blocks.BARREL.defaultBlockState(), 3);
        }
        VillageLifeManager.ensureVillageLife(serverLevel, villageOrigin, data);

        boolean firstArrival = player.addTag(STARTER_KIT_TAG);
        if (firstArrival) {
            giveOrDrop(player, ModItems.RECIPE_NOTEBOOK.get().getDefaultInstance());
            giveOrDrop(player, ModItems.LIFE_GUIDE.get().getDefaultInstance());
            giveOrDrop(player, Items.FISHING_ROD.getDefaultInstance());
            giveOrDrop(player, new ItemStack(Items.WHEAT_SEEDS, 32));
            giveOrDrop(player, new ItemStack(Items.CARROT, 8));
            giveOrDrop(player, new ItemStack(Items.POTATO, 8));
            player.sendSystemMessage(Component.translatable("message.countrysidedays.starter_kit"));
        }

        if (firstArrival || allocation.created()) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.personal_estate_ready"));
            player.sendSystemMessage(Component.translatable(
                    SharedRestaurantAccess.isOwner(data, player.getUUID())
                            ? "message.countrysidedays.restaurant_role_owner"
                            : "message.countrysidedays.restaurant_role_staff"
            ));
            player.sendSystemMessage(Component.translatable("message.countrysidedays.property_deed"));
            player.sendSystemMessage(Component.translatable("message.countrysidedays.empty_farm_ready"));
            player.sendSystemMessage(Component.translatable("message.countrysidedays.meet_resident"));
            player.sendSystemMessage(Component.translatable("message.countrysidedays.keep_inventory_enabled"));
        }

        syncEstateHud(player, estate);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel serverLevel)
                || serverLevel.dimension() != Level.OVERWORLD
                || !CountrysideRegionManager.isFlatWorld(serverLevel)) return;

        if (player.tickCount % 40 == 0) {
            serverLevel.getEntitiesOfClass(
                    Mob.class,
                    player.getBoundingBox().inflate(128.0),
                    mob -> mob instanceof Enemy
            ).forEach(Mob::discard);
        }

        if (player.tickCount % 40 == 0) {
            CountrysideWorldData data = CountrysideWorldData.get(serverLevel.getServer());
            data.homesteadOrigin().ifPresent(origin -> RuralNpcManager.tickVillage(serverLevel, origin));
            data.estate(player.getUUID()).ifPresent(estate -> syncEstateHud(player, estate));
        }
    }

    private static void syncEstateHud(ServerPlayer player, CountrysideWorldData.PlayerEstate ownEstate) {
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        CountrysideWorldData.PlayerEstate restaurantEstate = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(ownEstate);
        BlockPos home = PlayerEstateLayout.home(ownEstate.originPos());
        BlockPos restaurant = PlayerEstateLayout.restaurant(restaurantEstate.originPos());
        long day = Math.max(0L, player.level().getGameTime() / 24000L);
        int pendingRanchProducts = ownEstate.pendingEggs()
                + ownEstate.pendingMilk()
                + ownEstate.pendingWool();
        PacketDistributor.sendToPlayer(player, new EstateHudPayload(
                home.getX(), home.getY(), home.getZ(),
                restaurant.getX(), restaurant.getY(), restaurant.getZ(),
                restaurantEstate.restaurantOpen(),
                restaurantEstate.customersServedToday(day),
                CountrysideWorldData.DAILY_CUSTOMER_CAP,
                restaurantEstate.customersServed(),
                restaurantEstate.progressionStage(),
                pendingRanchProducts
        ));
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !(event.getEntity() instanceof Enemy)
                || !CountrysideRegionManager.isInsideCountryside(serverLevel, event.getEntity().blockPosition())) return;
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
                || !(player.level() instanceof ServerLevel serverLevel)) return;
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
