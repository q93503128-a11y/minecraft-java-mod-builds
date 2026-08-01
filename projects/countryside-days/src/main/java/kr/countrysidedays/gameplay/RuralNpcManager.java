package kr.countrysidedays.gameplay;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import net.minecraft.core.BlockPos;
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

    private static final String CUSTOMER_SUFFIX = "의 손님 민수";
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
        String customerName = customerName(estate);
        if (find(level, customerName, PlayerEstateLayout.restaurant(origin)).isEmpty()) {
            spawnVillager(level, customerName, PlayerEstateLayout.restaurantDoor(origin));
        }

        AABB ranch = estateRanchBounds(origin);
        if (level.getEntitiesOfClass(Animal.class, ranch, animal -> animal.getType().toString().contains("cow")).isEmpty()) {
            spawnAnimal(level, "cow", origin.offset(13, 1, 18), estate);
            spawnAnimal(level, "cow", origin.offset(19, 1, 22), estate);
        }
        if (level.getEntitiesOfClass(Animal.class, ranch, animal -> animal.getType().toString().contains("sheep")).isEmpty()) {
            spawnAnimal(level, "sheep", origin.offset(15, 1, 20), estate);
            spawnAnimal(level, "sheep", origin.offset(25, 1, 18), estate);
        }
        if (level.getEntitiesOfClass(Animal.class, ranch, animal -> animal.getType().toString().contains("chicken")).isEmpty()) {
            spawnAnimal(level, "chicken", origin.offset(18, 1, 24), estate);
            spawnAnimal(level, "chicken", origin.offset(21, 1, 24), estate);
        }
    }

    public static void ensureForHomestead(ServerLevel level, BlockPos origin) {
        ensurePublicVillage(level, origin);
        CountrysideWorldData.get(level.getServer()).estates()
                .forEach(estate -> ensureEstateAnimals(level, estate));
    }

    public static void tickVillage(ServerLevel level, BlockPos villageOrigin) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        AABB village = new AABB(villageOrigin).inflate(600.0, 24.0, 600.0);
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());

        for (Villager villager : level.getEntitiesOfClass(Villager.class, village)) {
            NpcDefinition publicDefinition = definitionByName(villageOrigin, villager.getName().getString());
            if (publicDefinition != null) {
                villager.setPose(Pose.STANDING);
                BlockPos target;
                double speed;
                if (time < 1000L) {
                    target = villageOrigin.offset(0, 1, 5);
                    speed = 0.42;
                } else if (time < 9000L) {
                    target = publicDefinition.work();
                    speed = 0.48;
                } else if (time < 12000L) {
                    target = publicDefinition.social();
                    speed = 0.44;
                } else {
                    target = publicDefinition.home();
                    speed = 0.40;
                }
                navigate(villager, target, speed);
                continue;
            }

            Optional<CountrysideWorldData.PlayerEstate> estate = estateByCustomerName(data, villager.getName().getString());
            if (estate.isEmpty()) continue;
            tickCustomer(villageOrigin, villager, estate.get(), time);
        }
    }

    private static void tickCustomer(
            BlockPos villageOrigin,
            Villager villager,
            CountrysideWorldData.PlayerEstate estate,
            long time
    ) {
        if (time >= OPEN_TIME && time < CLOSE_TIME) {
            BlockPos seat = PlayerEstateLayout.customerSeat(estate.originPos());
            if (villager.blockPosition().distSqr(seat) <= 3.0) {
                villager.getNavigation().stop();
                villager.setPos(seat.getX() + 0.5, seat.getY() + 0.05, seat.getZ() + 0.5);
                villager.setYRot(180.0F);
                villager.setPose(Pose.SITTING);
            } else {
                villager.setPose(Pose.STANDING);
                navigate(villager, seat, 0.50);
            }
        } else {
            villager.setPose(Pose.STANDING);
            navigate(villager, villageOrigin.offset(0, 1, 8), 0.45);
        }
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)) return;

        String name = villager.getName().getString();
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        Optional<CountrysideWorldData.PlayerEstate> customerEstate = estateByCustomerName(data, name);
        if (customerEstate.isPresent()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            handleCustomer(player, customerEstate.get());
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
        villager.setVillagerData(
                villager.getVillagerData()
                        .withProfession(level.registryAccess(), profession)
                        .withLevel(5)
        );

        MerchantOffers offers = new MerchantOffers();
        if (FARMER_NAME.equals(name)) {
            offers.add(offer(1, Items.WHEAT_SEEDS, 8));
            offers.add(offer(2, Items.CARROT, 4));
            offers.add(offer(2, Items.POTATO, 4));
            offers.add(offer(3, Blocks.HAY_BLOCK, 1));
            offers.add(offer(4, Items.WATER_BUCKET, 1));
            offers.add(offer(3, Items.HONEY_BOTTLE, 1));
            offers.add(offer(1, Items.BOWL, 4));
        } else if (RANCHER_NAME.equals(name)) {
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

    private static void handleResident(ServerPlayer player) {
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        CountrysideWorldData.PlayerEstate estate = data.estate(player.getUUID()).orElse(null);
        if (estate == null || estate.customersServed() == 0) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.resident_first_guidance"));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.resident_progress",
                    estate.customersServed(), estate.coinsEarned()
            ));
        }
    }

    private static void handleCustomer(ServerPlayer player, CountrysideWorldData.PlayerEstate estate) {
        if (!estate.isOwner(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_owner_only"));
            return;
        }

        ServerLevel level = player.level();
        long time = Math.floorMod(level.getDayTime(), 24000L);
        if (time < OPEN_TIME || time >= CLOSE_TIME) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.restaurant_closed"));
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.COUNTRY_STEW.get())) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_order"));
            return;
        }

        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        long day = Math.max(0L, level.getDayTime() / 24000L);
        if (!data.recordCustomerService(player.getUUID(), day, DAILY_REWARD_COINS)) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_already_served"));
            return;
        }

        if (!player.getAbilities().instabuild) held.shrink(1);
        giveOrDrop(player, new ItemStack(ModItems.VILLAGE_COIN.get(), DAILY_REWARD_COINS));
        player.giveExperiencePoints(8);
        int served = data.estate(player.getUUID())
                .map(CountrysideWorldData.PlayerEstate::customersServed)
                .orElse(0);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.customer_served",
                DAILY_REWARD_COINS, served
        ));
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
        AABB area = new AABB(centre).inflate(96.0, 18.0, 96.0);
        return level.getEntitiesOfClass(
                Villager.class,
                area,
                villager -> name.equals(villager.getName().getString())
        ).stream().findFirst();
    }

    private static Villager spawnVillager(ServerLevel level, String name, BlockPos pos) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) {
            CountrysideDays.LOGGER.error("Minecraft villager entity type is unavailable");
            return null;
        }
        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) {
            CountrysideDays.LOGGER.error("Failed to create countryside NPC {}", name);
            return null;
        }
        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setInvulnerable(true);
        if (!level.addFreshEntity(villager)) {
            CountrysideDays.LOGGER.error("Failed to add countryside NPC {}", name);
            return null;
        }
        return villager;
    }

    private static void spawnAnimal(
            ServerLevel level,
            String id,
            BlockPos pos,
            CountrysideWorldData.PlayerEstate estate
    ) {
        Identifier identifier = Identifier.fromNamespaceAndPath("minecraft", id);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
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

    private static Optional<CountrysideWorldData.PlayerEstate> estateByCustomerName(
            CountrysideWorldData data,
            String name
    ) {
        return data.estates().stream().filter(estate -> customerName(estate).equals(name)).findFirst();
    }

    private static String customerName(CountrysideWorldData.PlayerEstate estate) {
        return estate.ownerName() + CUSTOMER_SUFFIX;
    }

    private static NpcDefinition definitionByName(BlockPos origin, String name) {
        for (NpcDefinition definition : publicDefinitions(origin)) {
            if (definition.name().equals(name)) return definition;
        }
        return null;
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
}
