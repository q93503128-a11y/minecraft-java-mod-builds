package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import kr.countrysidedays.world.SharedRestaurantBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RuralNpcManager {
    public static final String RESIDENT_NAME = "복순 할머니";
    public static final String FARMER_NAME = "농부 한결";
    public static final String RANCHER_NAME = "목장지기 소미";
    public static final String HALL_KEEPER_NAME = "회관지기 도윤";
    public static final String PUBLIC_LIVESTOCK_TAG = "cd_public_livestock";
    public static final int DAILY_REWARD_COINS = 4;

    private static final String[] CUSTOMER_NAMES = {
            "시골식당 손님 민수",
            "시골식당 손님 영희",
            "시골식당 손님 준호"
    };
    private static final String LEGACY_CUSTOMER_MARKER = "의 손님 ";
    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final long OPEN_TIME = 1000L;
    private static final long CLOSE_TIME = 11500L;

    private RuralNpcManager() {
    }

    public static void ensurePublicVillage(ServerLevel level, BlockPos origin) {
        for (NpcDefinition definition : publicDefinitions(origin)) {
            Villager villager = find(level, definition.name(), definition.home()).orElse(null);
            if (villager == null) villager = spawnVillager(level, definition.name(), definition.home());
            if (villager != null) configureShop(level, villager, definition.name());
        }
        ensurePublicLivestock(level, origin);
    }

    public static void ensureEstateAnimals(ServerLevel level, CountrysideWorldData.PlayerEstate estate) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        SharedRestaurantAccess.restaurantEstate(data).ifPresent(shared -> {
            if (shared.ownerUuid().equals(estate.ownerUuid())) ensureSharedCustomers(level, shared);
        });
        removeLegacyCustomers(level, estate.originPos());

        BlockPos origin = estate.originPos();
        AABB ranch = estateRanchBounds(origin);
        spawnSpeciesIfMissing(level, ranch, "cow", origin.offset(13, 1, 18), origin.offset(19, 1, 22), estate);
        spawnSpeciesIfMissing(level, ranch, "sheep", origin.offset(15, 1, 20), origin.offset(25, 1, 18), estate);
        spawnSpeciesIfMissing(level, ranch, "chicken", origin.offset(18, 1, 24), origin.offset(21, 1, 24), estate);
    }

    private static void ensureSharedCustomers(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate restaurantEstate
    ) {
        BlockPos origin = restaurantEstate.originPos();
        for (int slot = 0; slot < CountrysideWorldData.DAILY_CUSTOMER_CAP; slot++) {
            String name = CUSTOMER_NAMES[slot];
            if (find(level, name, origin).isPresent()) continue;
            Villager customer = spawnVillager(level, name, PlayerEstateLayout.customerWaiting(origin, slot));
            if (customer != null) customer.addTag("cd_restaurant_customer_" + slot);
        }
    }

    private static void removeLegacyCustomers(ServerLevel level, BlockPos centre) {
        Set<String> active = new HashSet<>(List.of(CUSTOMER_NAMES));
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class,
                new AABB(centre).inflate(600.0, 64.0, 600.0),
                villager -> true
        )) {
            String name = villager.getName().getString();
            if ((name.contains(LEGACY_CUSTOMER_MARKER) || name.startsWith("시골식당 손님 "))
                    && !active.contains(name)) villager.discard();
        }
    }

    private static void spawnSpeciesIfMissing(
            ServerLevel level,
            AABB ranch,
            String id,
            BlockPos first,
            BlockPos second,
            CountrysideWorldData.PlayerEstate estate
    ) {
        boolean exists = !level.getEntitiesOfClass(
                Animal.class,
                ranch,
                animal -> animal.getType().toString().contains(id)
                        && RanchLifeManager.belongsTo(animal, estate.ownerUuid())
        ).isEmpty();
        if (exists) return;
        spawnAnimal(level, id, first, estate);
        spawnAnimal(level, id, second, estate);
    }

    public static void ensureForHomestead(ServerLevel level, BlockPos origin) {
        ensurePublicVillage(level, origin);
        CountrysideWorldData.get(level.getServer()).estates()
                .forEach(estate -> ensureEstateAnimals(level, estate));
    }

    public static void tickVillage(ServerLevel level, BlockPos villageOrigin) {
        long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
        long day = Math.max(0L, level.getOverworldClockTime() / 24000L);
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        CountrysideWorldData.PlayerEstate restaurantEstate = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(null);

        boolean active = restaurantEstate != null
                && restaurantEstate.restaurantOpen()
                && isRestaurantBusinessTime(level);
        if (restaurantEstate != null) {
            if (restaurantEstate.restaurantOpen() && !isRestaurantBusinessTime(level)) {
                SharedRestaurantAccess.setOpen(data, false);
                active = false;
            }
            boolean ownerAccess = !active && restaurantOwnerNeedsAccess(level, data, restaurantEstate.originPos());
            SharedRestaurantBuilder.setOpen(level, restaurantEstate.originPos(), active || ownerAccess);
            ensureSharedCustomers(level, restaurantEstate);
        }

        AABB village = new AABB(villageOrigin).inflate(600.0, 32.0, 600.0);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, village)) {
            NpcDefinition publicNpc = definitionByName(villageOrigin, villager.getName().getString());
            if (publicNpc != null) {
                tickResident(level, villager, publicNpc, time, day);
                continue;
            }
            CustomerRef customer = customerByName(restaurantEstate, villager.getName().getString()).orElse(null);
            if (customer != null) {
                tickCustomer(level, villageOrigin, villager, customer.estate(), customer.slot(), active, day);
            }
        }
        containPublicLivestock(level, villageOrigin);
    }

    private static boolean restaurantOwnerNeedsAccess(
            ServerLevel level,
            CountrysideWorldData data,
            BlockPos restaurantOrigin
    ) {
        UUID owner = SharedRestaurantAccess.restaurantOwner(data).orElse(null);
        if (owner == null) return false;
        return level.players().stream().anyMatch(player -> owner.equals(player.getUUID())
                && PlayerEstateLayout.isRestaurantOwnerAccessZone(
                        restaurantOrigin,
                        player.blockPosition()
                ));
    }

    public static boolean isRestaurantBusinessTime(ServerLevel level) {
        long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return time >= OPEN_TIME && time < CLOSE_TIME;
    }

    private static void tickResident(
            ServerLevel level,
            Villager villager,
            NpcDefinition definition,
            long time,
            long day
    ) {
        BlockPos target;
        boolean stationary;
        if (VillageLifeManager.isHoliday(day)) {
            target = activityOffset(definition.social(), definition.name(), time, 2);
            stationary = false;
        } else if (time < 2000L || time >= 13500L) {
            target = definition.home();
            stationary = false;
        } else if (time >= 6000L && time < 7200L) {
            target = definition.lunch();
            stationary = false;
        } else if (time >= 12000L) {
            target = activityOffset(definition.social(), definition.name(), time, 1);
            stationary = false;
        } else {
            target = definition.work();
            stationary = definition.stationaryAtWork();
        }
        navigate(level, villager, target, stationary ? 0.43 : 0.48, stationary);
    }

    private static BlockPos activityOffset(BlockPos base, String identity, long time, int radius) {
        int phase = Math.floorMod((int) (time / 600L) + identity.hashCode(), 8);
        int dx = switch (phase) {
            case 0, 1, 7 -> radius;
            case 3, 4, 5 -> -radius;
            default -> 0;
        };
        int dz = switch (phase) {
            case 1, 2, 3 -> radius;
            case 5, 6, 7 -> -radius;
            default -> 0;
        };
        return base.offset(dx, 0, dz);
    }

    private static void tickCustomer(
            ServerLevel level,
            BlockPos villageOrigin,
            Villager villager,
            CountrysideWorldData.PlayerEstate estate,
            int slot,
            boolean restaurantActive,
            long day
    ) {
        boolean served = estate.customerServedToday(day, slot);
        if (!restaurantActive || served) {
            villager.setNoAi(false);
            villager.setPose(Pose.STANDING);
            BlockPos outside = served
                    ? customerDayActivity(villageOrigin, slot, day)
                    : PlayerEstateLayout.customerWaiting(estate.originPos(), slot);
            outside = nearestWalkable(level, outside);
            if (PlayerEstateLayout.isRestaurantArea(estate.originPos(), villager.blockPosition())) {
                moveImmediately(villager, outside);
            }
            navigate(level, villager, outside, 0.48, false);
            return;
        }

        BlockPos approach = PlayerEstateLayout.customerApproach(estate.originPos(), slot);
        if (villager.blockPosition().distSqr(approach) > 2.25) {
            villager.setNoAi(false);
            villager.setPose(Pose.STANDING);
            navigate(level, villager, approach, 0.53, false);
            return;
        }

        BlockPos seat = PlayerEstateLayout.customerSeat(estate.originPos(), slot);
        villager.getNavigation().stop();
        villager.setPos(seat.getX() + 0.5, seat.getY() + 0.12, seat.getZ() + 0.5);
        villager.setYRot(PlayerEstateLayout.customerSeatYaw(slot));
        villager.setPose(Pose.SITTING);
        villager.setNoAi(true);
    }

    private static BlockPos customerDayActivity(BlockPos villageOrigin, int slot, long day) {
        int phase = Math.floorMod((int) day + slot, 3);
        return switch (Math.floorMod(slot + phase, 3)) {
            case 0 -> villageOrigin.offset(-24, 1, -21);
            case 1 -> villageOrigin.offset(22, 1, -21);
            default -> villageOrigin.offset(-42, 1, 35);
        };
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)) return;

        String name = villager.getName().getString();
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        CountrysideWorldData.PlayerEstate restaurantEstate = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(null);
        Optional<CustomerRef> customer = customerByName(restaurantEstate, name);
        if (customer.isPresent()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            handleCustomer(player, customer.get());
            return;
        }
        if (isShopNpc(name)) {
            configureShop(player.level(), villager, name);
            return;
        }
        if (!RESIDENT_NAME.equals(name)) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        handleResident(player);
    }

    private static boolean isShopNpc(String name) {
        return FARMER_NAME.equals(name) || RANCHER_NAME.equals(name) || HALL_KEEPER_NAME.equals(name);
    }

    private static void configureShop(ServerLevel level, Villager villager, String name) {
        if (!isShopNpc(name)) return;
        var profession = FARMER_NAME.equals(name)
                ? VillagerProfession.FARMER
                : RANCHER_NAME.equals(name)
                ? VillagerProfession.SHEPHERD
                : VillagerProfession.LIBRARIAN;
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.registryAccess(), profession)
                .withLevel(5));

        MerchantOffers offers = new MerchantOffers();
        if (FARMER_NAME.equals(name)) {
            offers.add(buyOffer(Items.CARROT, 20, 1));
            offers.add(buyOffer(Items.POTATO, 20, 1));
            offers.add(offer(1, Items.WHEAT_SEEDS, 8));
            offers.add(offer(3, Items.CARROT, 4));
            offers.add(offer(3, Items.POTATO, 4));
            offers.add(offer(3, Blocks.HAY_BLOCK, 1));
            offers.add(offer(4, Items.WATER_BUCKET, 1));
            offers.add(offer(3, Items.HONEY_BOTTLE, 1));
            offers.add(offer(1, Items.BOWL, 4));
        } else if (RANCHER_NAME.equals(name)) {
            offers.add(buyOffer(Items.EGG, 6, 2));
            offers.add(buyOffer(Items.MILK_BUCKET, 1, 3));
            offers.add(buyOffer(Blocks.WOOL.pick(DyeColor.WHITE), 2, 2));
            offers.add(offer(2, Items.WHEAT, 8));
            offers.add(offer(3, Blocks.OAK_FENCE_GATE, 2));
            offers.add(offer(4, Items.LEAD, 1));
            offers.add(offer(6, Items.NAME_TAG, 1));
            offers.add(offer(4, Items.SHEARS, 1));
        } else {
            offers.add(offer(2, Blocks.FLOWER_POT, 2));
            offers.add(offer(3, Blocks.LANTERN, 2));
            offers.add(offer(4, Items.ITEM_FRAME, 2));
            offers.add(offer(5, Items.PAINTING, 1));
            offers.add(offer(5, Blocks.BOOKSHELF, 2));
            offers.add(offer(3, Blocks.CARPET.pick(DyeColor.YELLOW), 8));
            offers.add(offer(4, Blocks.OAK_STAIRS, 12));
        }
        villager.setOffers(offers);
    }

    private static MerchantOffer offer(int coins, ItemLike result, int count) {
        return new MerchantOffer(
                new ItemCost(ModItems.VILLAGE_COIN.get(), coins),
                new ItemStack(result, count),
                999,
                1,
                0.0F
        );
    }

    private static MerchantOffer buyOffer(ItemLike input, int count, int coins) {
        return new MerchantOffer(
                new ItemCost(input, count),
                new ItemStack(ModItems.VILLAGE_COIN.get(), coins),
                999,
                1,
                0.0F
        );
    }

    private static void handleResident(ServerPlayer player) {
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        CountrysideWorldData.PlayerEstate ownEstate = data.estate(player.getUUID()).orElse(null);
        CountrysideWorldData.PlayerEstate restaurantEstate = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(ownEstate);
        if (ownEstate == null || restaurantEstate == null) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.resident_first_guidance"));
            return;
        }
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.resident_progress_extended",
                restaurantEstate.progressionStage(),
                restaurantEstate.customersServed(),
                ownEstate.ranchProductsCollected()
        ));
    }

    private static void handleCustomer(ServerPlayer player, CustomerRef customer) {
        ServerLevel level = player.level();
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        CountrysideWorldData.PlayerEstate estate = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(customer.estate());
        if (!SharedRestaurantAccess.isStaff(data, player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_owner_only"));
            return;
        }
        if (!estate.restaurantOpen()) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.restaurant_not_open"));
            return;
        }
        if (!isRestaurantBusinessTime(level)) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.restaurant_closed"));
            return;
        }

        long day = Math.max(0L, level.getOverworldClockTime() / 24000L);
        if (estate.customerServedToday(day, customer.slot())) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_already_served"));
            return;
        }

        CustomerOrder order = orderFor(estate, day, customer.slot());
        ItemStack held = player.getMainHandItem();
        if (!held.is(order.item())) {
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.customer_order_named",
                    Component.translatable(order.nameKey()), order.rewardCoins()
            ));
            return;
        }

        if (!SharedRestaurantAccess.recordCustomerService(
                data, day, customer.slot(), order.rewardCoins()
        )) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_already_served"));
            return;
        }

        if (!player.getAbilities().instabuild) held.shrink(1);
        giveOrDrop(player, new ItemStack(ModItems.VILLAGE_COIN.get(), order.rewardCoins()));
        player.giveExperiencePoints(order.experience());

        CountrysideWorldData.PlayerEstate updated = SharedRestaurantAccess
                .restaurantEstate(data)
                .orElse(estate);
        int today = updated.customersServedToday(day);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.customer_served_extended",
                order.rewardCoins(), today, CountrysideWorldData.DAILY_CUSTOMER_CAP,
                updated.customersServed()
        ));
        if (today >= CountrysideWorldData.DAILY_CUSTOMER_CAP) {
            SharedRestaurantAccess.setOpen(data, false);
            player.sendSystemMessage(Component.translatable("message.countrysidedays.shift_complete"));
        }
    }

    private static CustomerOrder orderFor(
            CountrysideWorldData.PlayerEstate estate,
            long day,
            int slot
    ) {
        long seed = day * 0x9E3779B97F4A7C15L
                ^ (long) estate.ownerUuid().hashCode() * 0xBF58476D1CE4E5B9L
                ^ (long) (slot + 1) * 0x94D049BB133111EBL;
        seed ^= seed >>> 30;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >>> 27;
        int index = Math.floorMod(Long.hashCode(seed), 6);
        return switch (index) {
            case 0 -> new CustomerOrder(ModItems.COUNTRY_STEW.get(),
                    "item.countrysidedays.country_stew", 5, 8);
            case 1 -> new CustomerOrder(ModItems.HERB_TEA.get(),
                    "item.countrysidedays.herb_tea", 3, 5);
            case 2 -> new CustomerOrder(ModItems.FARM_BREAKFAST.get(),
                    "item.countrysidedays.farm_breakfast", 6, 10);
            case 3 -> new CustomerOrder(ModItems.GRILLED_RIVER_FISH.get(),
                    "item.countrysidedays.grilled_river_fish", 5, 8);
            case 4 -> new CustomerOrder(ModItems.POTATO_PANCAKE.get(),
                    "item.countrysidedays.potato_pancake", 6, 9);
            default -> new CustomerOrder(ModItems.HONEY_CARROT_SALAD.get(),
                    "item.countrysidedays.honey_carrot_salad", 5, 8);
        };
    }

    private static void navigate(
            ServerLevel level,
            Villager villager,
            BlockPos requestedTarget,
            double speed,
            boolean stationaryAtTarget
    ) {
        BlockPos target = nearestWalkable(level, requestedTarget);
        if (!isWalkable(level, villager.blockPosition()) && villager.getPose() != Pose.SITTING) {
            moveImmediately(villager, target);
        }
        if (villager.blockPosition().distSqr(target) <= 2.25) {
            villager.getNavigation().stop();
            villager.setPose(Pose.STANDING);
            villager.setNoAi(stationaryAtTarget);
            return;
        }
        villager.setNoAi(false);
        villager.setPose(Pose.STANDING);
        villager.getNavigation().moveTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                speed
        );
    }

    private static BlockPos nearestWalkable(ServerLevel level, BlockPos target) {
        if (isWalkable(level, target)) return target;
        for (int radius = 1; radius <= 5; radius++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos candidate = target.offset(dx, dy, dz);
                        if (isWalkable(level, candidate)) return candidate;
                    }
                }
            }
        }
        return target;
    }

    private static boolean isWalkable(ServerLevel level, BlockPos pos) {
        BlockPos floorPos = pos.below();
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(floorPos).isFaceSturdy(level, floorPos, Direction.UP);
    }

    private static void moveImmediately(Villager villager, BlockPos pos) {
        villager.getNavigation().stop();
        villager.setNoAi(false);
        villager.setPose(Pose.STANDING);
        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    private static Optional<Villager> find(ServerLevel level, String name, BlockPos centre) {
        return level.getEntitiesOfClass(
                Villager.class,
                new AABB(centre).inflate(120.0, 24.0, 120.0),
                villager -> name.equals(villager.getName().getString())
        ).stream().findFirst();
    }

    private static Villager spawnVillager(ServerLevel level, String name, BlockPos pos) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (type == null) return null;
        Entity created = type.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return null;
        BlockPos safe = nearestWalkable(level, pos);
        villager.setPos(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(false);
        villager.setPersistenceRequired();
        villager.setInvulnerable(true);
        if (!level.addFreshEntity(villager)) return null;
        return villager;
    }

    private static void spawnAnimal(
            ServerLevel level,
            String id,
            BlockPos pos,
            CountrysideWorldData.PlayerEstate estate
    ) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(Identifier.fromNamespaceAndPath("minecraft", id))
                .orElse(null);
        if (type == null) return;
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (!(entity instanceof Animal animal)) return;
        animal.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        RanchLifeManager.initializeAnimal(animal, estate);
        level.addFreshEntity(animal);
    }

    private static void ensurePublicLivestock(ServerLevel level, BlockPos origin) {
        AABB bounds = publicRanchBounds(origin);
        spawnPublicSpeciesIfMissing(level, bounds, "cow", "소미네 젖소", origin.offset(36, 1, 36));
        spawnPublicSpeciesIfMissing(level, bounds, "sheep", "소미네 양", origin.offset(41, 1, 38));
        spawnPublicSpeciesIfMissing(level, bounds, "chicken", "마을 닭", origin.offset(46, 1, 40));
    }

    private static void spawnPublicSpeciesIfMissing(
            ServerLevel level,
            AABB bounds,
            String id,
            String name,
            BlockPos pos
    ) {
        boolean exists = !level.getEntitiesOfClass(
                Animal.class,
                bounds,
                animal -> animal.entityTags().contains(PUBLIC_LIVESTOCK_TAG)
                        && animal.getType().toString().contains(id)
        ).isEmpty();
        if (exists) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(Identifier.fromNamespaceAndPath("minecraft", id))
                .orElse(null);
        if (type == null) return;
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (!(entity instanceof Animal animal)) return;
        animal.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        animal.setCustomName(Component.literal(name));
        animal.setCustomNameVisible(false);
        animal.setPersistenceRequired();
        animal.addTag(PUBLIC_LIVESTOCK_TAG);
        level.addFreshEntity(animal);
    }

    private static void containPublicLivestock(ServerLevel level, BlockPos origin) {
        AABB search = new AABB(origin).inflate(96.0, 24.0, 96.0);
        AABB bounds = publicRanchBounds(origin);
        int index = 0;
        for (Animal animal : level.getEntitiesOfClass(
                Animal.class,
                search,
                animal -> animal.entityTags().contains(PUBLIC_LIVESTOCK_TAG)
        )) {
            if (!bounds.contains(animal.position())) {
                BlockPos returnPos = origin.offset(36 + index * 4, 1, 36 + index * 2);
                animal.getNavigation().stop();
                animal.setPos(returnPos.getX() + 0.5, returnPos.getY(), returnPos.getZ() + 0.5);
            }
            index++;
        }
    }

    private static AABB publicRanchBounds(BlockPos origin) {
        return new AABB(
                origin.getX() + 30.0, origin.getY(), origin.getZ() + 30.0,
                origin.getX() + 51.0, origin.getY() + 8.0, origin.getZ() + 45.0
        );
    }

    private static AABB estateRanchBounds(BlockPos origin) {
        return new AABB(
                origin.getX() + 6.0,
                origin.getY(),
                origin.getZ() + 2.0,
                origin.getX() + 29.0,
                origin.getY() + 10.0,
                origin.getZ() + 28.0
        );
    }

    private static Optional<CustomerRef> customerByName(
            CountrysideWorldData.PlayerEstate restaurantEstate,
            String name
    ) {
        if (restaurantEstate == null) return Optional.empty();
        for (int slot = 0; slot < CUSTOMER_NAMES.length; slot++) {
            if (CUSTOMER_NAMES[slot].equals(name)) return Optional.of(new CustomerRef(restaurantEstate, slot));
        }
        return Optional.empty();
    }

    private static NpcDefinition definitionByName(BlockPos origin, String name) {
        return publicDefinitions(origin).stream()
                .filter(definition -> definition.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static List<NpcDefinition> publicDefinitions(BlockPos origin) {
        return List.of(
                new NpcDefinition(
                        RESIDENT_NAME,
                        origin.offset(-37, 1, -17),
                        origin.offset(-10, 1, 8),
                        origin.offset(-12, 1, 8),
                        origin.offset(-12, 1, 24),
                        false
                ),
                new NpcDefinition(
                        FARMER_NAME,
                        origin.offset(37, 1, -17),
                        origin.offset(-20, 1, 10),
                        origin.offset(-20, 1, 16),
                        origin.offset(-23, 1, 25),
                        true
                ),
                new NpcDefinition(
                        RANCHER_NAME,
                        origin.offset(-37, 1, 21),
                        origin.offset(35, 1, 35),
                        origin.offset(33, 1, 30),
                        origin.offset(43, 1, 37),
                        true
                ),
                new NpcDefinition(
                        HALL_KEEPER_NAME,
                        origin.offset(37, 1, 21),
                        origin.offset(0, 1, -31),
                        origin.offset(5, 1, -28),
                        origin.offset(24, 1, -25),
                        true
                )
        );
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private record NpcDefinition(
            String name,
            BlockPos home,
            BlockPos work,
            BlockPos lunch,
            BlockPos social,
            boolean stationaryAtWork
    ) {
    }

    private record CustomerRef(CountrysideWorldData.PlayerEstate estate, int slot) {
    }

    private record CustomerOrder(Item item, String nameKey, int rewardCoins, int experience) {
    }
}
