package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
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

import java.util.List;
import java.util.Optional;

public final class RuralNpcManager {
    public static final String RESIDENT_NAME = "복순 할머니";
    public static final String FARMER_NAME = "농부 한결";
    public static final String RANCHER_NAME = "목장지기 소미";
    public static final String HALL_KEEPER_NAME = "회관지기 도윤";
    public static final int DAILY_REWARD_COINS = 4;

    private static final String[] CUSTOMER_NAMES = {"민수", "영희", "준호"};
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
    }

    public static void ensureEstateAnimals(ServerLevel level, CountrysideWorldData.PlayerEstate estate) {
        BlockPos origin = estate.originPos();
        for (int slot = 0; slot < CountrysideWorldData.DAILY_CUSTOMER_CAP; slot++) {
            String name = customerName(estate, slot);
            if (find(level, name, PlayerEstateLayout.restaurant(origin)).isEmpty()) {
                spawnVillager(level, name, PlayerEstateLayout.restaurantDoor(origin).offset(slot - 1, 0, 2));
            }
        }

        AABB ranch = estateRanchBounds(origin);
        spawnSpeciesIfMissing(level, ranch, "cow", origin.offset(13, 1, 18), origin.offset(19, 1, 22), estate);
        spawnSpeciesIfMissing(level, ranch, "sheep", origin.offset(15, 1, 20), origin.offset(25, 1, 18), estate);
        spawnSpeciesIfMissing(level, ranch, "chicken", origin.offset(18, 1, 24), origin.offset(21, 1, 24), estate);
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
        long time = Math.floorMod(level.getGameTime(), 24000L);
        long day = Math.max(0L, level.getGameTime() / 24000L);
        AABB village = new AABB(villageOrigin).inflate(600.0, 24.0, 600.0);
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        for (Villager villager : level.getEntitiesOfClass(Villager.class, village)) {
            NpcDefinition publicNpc = definitionByName(villageOrigin, villager.getName().getString());
            if (publicNpc != null) {
                tickResident(villager, publicNpc, villageOrigin, time);
                continue;
            }
            customerByName(data, villager.getName().getString())
                    .ifPresent(customer -> tickCustomer(
                            villageOrigin, villager, customer.estate(), customer.slot(), time, day
                    ));
        }
    }

    private static void tickResident(
            Villager villager,
            NpcDefinition definition,
            BlockPos villageOrigin,
            long time
    ) {
        villager.setNoAi(false);
        villager.setPose(Pose.STANDING);
        if (time < 1000L) navigate(villager, villageOrigin.offset(0, 1, 5), 0.42);
        else if (time < 9000L) navigate(villager, definition.work(), 0.48);
        else if (time < 12000L) navigate(villager, definition.social(), 0.44);
        else navigate(villager, definition.home(), 0.40);
    }

    private static void tickCustomer(
            BlockPos villageOrigin,
            Villager villager,
            CountrysideWorldData.PlayerEstate estate,
            int slot,
            long time,
            long day
    ) {
        boolean available = estate.restaurantOpen()
                && time >= OPEN_TIME
                && time < CLOSE_TIME
                && !estate.customerServedToday(day, slot);
        if (!available) {
            villager.setNoAi(false);
            villager.setPose(Pose.STANDING);
            navigate(villager, villageOrigin.offset(slot - 1, 1, 8), 0.45);
            return;
        }

        BlockPos seat = PlayerEstateLayout.customerSeat(estate.originPos(), slot);
        Direction approachDirection = slot == 1 ? Direction.WEST : Direction.SOUTH;
        BlockPos approach = seat.relative(approachDirection);
        if (villager.blockPosition().distSqr(approach) > 4.0) {
            villager.setNoAi(false);
            villager.setPose(Pose.STANDING);
            navigate(villager, approach, 0.50);
            return;
        }

        villager.getNavigation().stop();
        villager.setPos(seat.getX() + 0.5, seat.getY() + 0.55, seat.getZ() + 0.5);
        villager.setYRot(slot == 1 ? -90.0F : 180.0F);
        villager.setPose(Pose.SITTING);
        villager.setNoAi(true);
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)) return;

        String name = villager.getName().getString();
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        Optional<CustomerRef> customer = customerByName(data, name);
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
            offers.add(buyOffer(Items.CARROT, 12, 2));
            offers.add(buyOffer(Items.POTATO, 12, 2));
            offers.add(offer(1, Items.WHEAT_SEEDS, 8));
            offers.add(offer(2, Items.CARROT, 4));
            offers.add(offer(2, Items.POTATO, 4));
            offers.add(offer(3, Blocks.HAY_BLOCK, 1));
            offers.add(offer(4, Items.WATER_BUCKET, 1));
            offers.add(offer(3, Items.HONEY_BOTTLE, 1));
            offers.add(offer(1, Items.BOWL, 4));
        } else if (RANCHER_NAME.equals(name)) {
            offers.add(buyOffer(Items.EGG, 4, 2));
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
        CountrysideWorldData.PlayerEstate estate = CountrysideWorldData.get(player.level().getServer())
                .estate(player.getUUID())
                .orElse(null);
        if (estate == null) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.resident_first_guidance"));
            return;
        }
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.resident_progress_extended",
                estate.progressionStage(), estate.customersServed(), estate.ranchProductsCollected()
        ));
    }

    private static void handleCustomer(ServerPlayer player, CustomerRef customer) {
        CountrysideWorldData.PlayerEstate estate = customer.estate();
        if (!estate.isOwner(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_owner_only"));
            return;
        }

        ServerLevel level = player.level();
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        estate = data.estate(player.getUUID()).orElse(estate);
        if (!estate.restaurantOpen()) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.restaurant_not_open"));
            return;
        }

        long time = Math.floorMod(level.getGameTime(), 24000L);
        if (time < OPEN_TIME || time >= CLOSE_TIME) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.restaurant_closed"));
            return;
        }

        long day = Math.max(0L, level.getGameTime() / 24000L);
        if (estate.customerServedToday(day, customer.slot())) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_already_served"));
            return;
        }

        CustomerOrder order = orderFor(day, customer.slot());
        ItemStack held = player.getMainHandItem();
        if (!held.is(order.item())) {
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.customer_order_named",
                    Component.translatable(order.nameKey()), order.rewardCoins()
            ));
            return;
        }

        if (!data.recordCustomerService(
                player.getUUID(), day, customer.slot(), order.rewardCoins()
        )) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_already_served"));
            return;
        }

        if (!player.getAbilities().instabuild) held.shrink(1);
        giveOrDrop(player, new ItemStack(ModItems.VILLAGE_COIN.get(), order.rewardCoins()));
        player.giveExperiencePoints(order.experience());

        CountrysideWorldData.PlayerEstate updated = data.estate(player.getUUID()).orElse(estate);
        int today = updated.customersServedToday(day);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.customer_served_extended",
                order.rewardCoins(), today, CountrysideWorldData.DAILY_CUSTOMER_CAP,
                updated.customersServed()
        ));
        if (today >= CountrysideWorldData.DAILY_CUSTOMER_CAP) {
            data.setRestaurantOpen(player.getUUID(), false);
            player.sendSystemMessage(Component.translatable("message.countrysidedays.shift_complete"));
        }
    }

    private static CustomerOrder orderFor(long day, int slot) {
        int index = Math.floorMod((int) (day + slot), 3);
        return switch (index) {
            case 0 -> new CustomerOrder(ModItems.COUNTRY_STEW.get(),
                    "item.countrysidedays.country_stew", 5, 8);
            case 1 -> new CustomerOrder(ModItems.HERB_TEA.get(),
                    "item.countrysidedays.herb_tea", 3, 5);
            default -> new CustomerOrder(ModItems.FARM_BREAKFAST.get(),
                    "item.countrysidedays.farm_breakfast", 6, 10);
        };
    }

    private static void navigate(Villager villager, BlockPos target, double speed) {
        if (villager.blockPosition().distSqr(target) <= 4.0) return;
        villager.getNavigation().moveTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                speed
        );
    }

    private static Optional<Villager> find(ServerLevel level, String name, BlockPos centre) {
        return level.getEntitiesOfClass(
                Villager.class,
                new AABB(centre).inflate(96.0, 18.0, 96.0),
                villager -> name.equals(villager.getName().getString())
        ).stream().findFirst();
    }

    private static Villager spawnVillager(ServerLevel level, String name, BlockPos pos) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (type == null) return null;
        Entity created = type.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return null;
        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(true);
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

    private static Optional<CustomerRef> customerByName(CountrysideWorldData data, String name) {
        for (CountrysideWorldData.PlayerEstate estate : data.estates()) {
            for (int slot = 0; slot < CountrysideWorldData.DAILY_CUSTOMER_CAP; slot++) {
                if (customerName(estate, slot).equals(name)) {
                    return Optional.of(new CustomerRef(estate, slot));
                }
            }
        }
        return Optional.empty();
    }

    private static String customerName(CountrysideWorldData.PlayerEstate estate, int slot) {
        return estate.ownerName() + "의 손님 " + CUSTOMER_NAMES[Math.floorMod(slot, CUSTOMER_NAMES.length)];
    }

    private static NpcDefinition definitionByName(BlockPos origin, String name) {
        return publicDefinitions(origin).stream()
                .filter(definition -> definition.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static List<NpcDefinition> publicDefinitions(BlockPos origin) {
        return List.of(
                new NpcDefinition(RESIDENT_NAME, origin.offset(-37, 1, -17), origin.offset(-10, 1, 8), origin.offset(0, 1, 5)),
                new NpcDefinition(FARMER_NAME, origin.offset(37, 1, -17), origin.offset(-20, 1, 8), origin.offset(-10, 1, 4)),
                new NpcDefinition(RANCHER_NAME, origin.offset(-37, 1, 21), origin.offset(-18, 1, 8), origin.offset(10, 1, 4)),
                new NpcDefinition(HALL_KEEPER_NAME, origin.offset(37, 1, 21), origin.offset(0, 1, -31), origin.offset(0, 1, 5))
        );
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private record NpcDefinition(String name, BlockPos home, BlockPos work, BlockPos social) {
    }

    private record CustomerRef(CountrysideWorldData.PlayerEstate estate, int slot) {
    }

    private record CustomerOrder(Item item, String nameKey, int rewardCoins, int experience) {
    }
}
