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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
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
            if (!exists(level, definition.name(), definition.home())) {
                spawnVillager(level, definition.name(), definition.home());
            }
        }
    }

    public static void ensureEstateAnimals(ServerLevel level, CountrysideWorldData.PlayerEstate estate) {
        BlockPos origin = estate.originPos();
        String customerName = customerName(estate);
        if (!exists(level, customerName, PlayerEstateLayout.restaurant(origin))) {
            spawnVillager(level, customerName, PlayerEstateLayout.restaurantDoor(origin));
        }

        AABB ranch = estateRanchBounds(origin);
        if (level.getEntitiesOfClass(Mob.class, ranch, mob -> mob.getType().toString().contains("cow")).isEmpty()) {
            spawnAnimal(level, "cow", origin.offset(12, 1, 20));
            spawnAnimal(level, "cow", origin.offset(20, 1, 23));
        }
        if (level.getEntitiesOfClass(Mob.class, ranch, mob -> mob.getType().toString().contains("sheep")).isEmpty()) {
            spawnAnimal(level, "sheep", origin.offset(15, 1, 18));
            spawnAnimal(level, "sheep", origin.offset(24, 1, 17));
        }
    }

    /** Legacy alias retained for old callers. */
    public static void ensureForHomestead(ServerLevel level, BlockPos origin) {
        ensurePublicVillage(level, origin);
        CountrysideWorldData.get(level.getServer()).estates()
                .forEach(estate -> ensureEstateAnimals(level, estate));
    }

    public static void tickVillage(ServerLevel level, BlockPos villageOrigin) {
        long time = Math.floorMod(level.getGameTime(), 24000L);
        boolean open = time >= OPEN_TIME && time < CLOSE_TIME;
        AABB village = new AABB(villageOrigin).inflate(600.0, 24.0, 600.0);
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());

        for (Villager villager : level.getEntitiesOfClass(Villager.class, village)) {
            NpcDefinition publicDefinition = definitionByName(villageOrigin, villager.getName().getString());
            if (publicDefinition != null) {
                navigate(villager, open ? publicDefinition.work() : publicDefinition.home(), open ? 0.48 : 0.42);
                continue;
            }

            Optional<CountrysideWorldData.PlayerEstate> estate = estateByCustomerName(data, villager.getName().getString());
            if (estate.isEmpty()) continue;
            BlockPos target = open
                    ? PlayerEstateLayout.restaurant(estate.get().originPos())
                    : villageOrigin.offset(0, 1, 8);
            navigate(villager, target, open ? 0.50 : 0.45);
        }
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)) return;

        String name = villager.getName().getString();
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        Optional<CountrysideWorldData.PlayerEstate> customerEstate = estateByCustomerName(data, name);
        boolean publicNpc = isPublicNpc(name);
        if (!publicNpc && customerEstate.isEmpty()) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (customerEstate.isPresent()) {
            handleCustomer(player, customerEstate.get());
            return;
        }

        switch (name) {
            case RESIDENT_NAME -> handleResident(player);
            case FARMER_NAME -> player.sendSystemMessage(Component.translatable("message.countrysidedays.farmer_guidance"));
            case RANCHER_NAME -> player.sendSystemMessage(Component.translatable("message.countrysidedays.rancher_guidance"));
            case HALL_KEEPER_NAME -> player.sendSystemMessage(Component.translatable("message.countrysidedays.hall_guidance"));
            default -> {
            }
        }
    }

    private static boolean isPublicNpc(String name) {
        return RESIDENT_NAME.equals(name)
                || FARMER_NAME.equals(name)
                || RANCHER_NAME.equals(name)
                || HALL_KEEPER_NAME.equals(name);
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
        long time = Math.floorMod(level.getGameTime(), 24000L);
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
        long day = Math.max(0L, level.getGameTime() / 24000L);
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
        if (villager.blockPosition().distSqr(target) <= 5.0) return;
        villager.getNavigation().moveTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                speed
        );
    }

    private static boolean exists(ServerLevel level, String name, BlockPos centre) {
        AABB area = new AABB(centre).inflate(96.0, 18.0, 96.0);
        return !level.getEntitiesOfClass(
                Villager.class,
                area,
                villager -> name.equals(villager.getName().getString())
        ).isEmpty();
    }

    private static void spawnVillager(ServerLevel level, String name, BlockPos pos) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) {
            CountrysideDays.LOGGER.error("Minecraft villager entity type is unavailable");
            return;
        }

        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) {
            CountrysideDays.LOGGER.error("Failed to create countryside NPC {}", name);
            return;
        }

        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setInvulnerable(true);
        if (!level.addFreshEntity(villager)) {
            CountrysideDays.LOGGER.error("Failed to add countryside NPC {}", name);
        }
    }

    private static void spawnAnimal(ServerLevel level, String id, BlockPos pos) {
        Identifier identifier = Identifier.fromNamespaceAndPath("minecraft", id);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
        if (type == null) return;
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (!(entity instanceof Mob mob)) return;
        mob.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    private static AABB estateRanchBounds(BlockPos origin) {
        return new AABB(
                origin.getX() + 6.0,
                origin.getY(),
                origin.getZ() + 2.0,
                origin.getX() + 29.0,
                origin.getY() + 9.0,
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
                new NpcDefinition(RESIDENT_NAME, origin.offset(-37, 1, -17), origin.offset(-10, 1, 8)),
                new NpcDefinition(FARMER_NAME, origin.offset(37, 1, -17), origin.offset(18, 1, 8)),
                new NpcDefinition(RANCHER_NAME, origin.offset(-37, 1, 21), origin.offset(-18, 1, 8)),
                new NpcDefinition(HALL_KEEPER_NAME, origin.offset(37, 1, 21), origin.offset(0, 1, -31))
        );
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private record NpcDefinition(String name, BlockPos home, BlockPos work) {
    }
}
