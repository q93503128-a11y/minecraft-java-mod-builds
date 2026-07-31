package kr.countrysidedays.gameplay;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
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

public final class RuralNpcManager {
    public static final String RESIDENT_NAME = "복순 할머니";
    public static final String FARMER_NAME = "농부 한결";
    public static final String RANCHER_NAME = "목장지기 소미";
    public static final String HALL_KEEPER_NAME = "회관지기 도윤";
    public static final String CUSTOMER_NAME = "나들이 손님 민수";
    public static final int DAILY_REWARD_COINS = 4;

    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final long OPEN_TIME = 1000L;
    private static final long CLOSE_TIME = 11500L;

    private RuralNpcManager() {
    }

    public static void ensureForHomestead(ServerLevel level, BlockPos origin) {
        for (NpcDefinition definition : definitions(origin)) {
            if (!exists(level, definition)) spawnVillager(level, definition);
        }
        ensureRanchAnimals(level, origin);
    }

    public static void tickVillage(ServerLevel level, BlockPos origin) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        boolean open = time >= OPEN_TIME && time < CLOSE_TIME;
        AABB village = new AABB(origin).inflate(96.0, 18.0, 96.0);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, village)) {
            NpcDefinition definition = definitionByName(origin, villager.getName().getString());
            if (definition == null) continue;
            BlockPos target = open ? definition.work() : definition.home();
            if (villager.blockPosition().distSqr(target) > 5.0) {
                villager.getNavigation().moveTo(
                        target.getX() + 0.5,
                        target.getY(),
                        target.getZ() + 0.5,
                        open ? 0.48 : 0.42
                );
            }
        }
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)) {
            return;
        }

        String name = villager.getName().getString();
        if (!isCountrysideNpc(name)) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        switch (name) {
            case RESIDENT_NAME -> handleResident(player);
            case FARMER_NAME -> player.sendSystemMessage(Component.translatable("message.countrysidedays.farmer_guidance"));
            case RANCHER_NAME -> player.sendSystemMessage(Component.translatable("message.countrysidedays.rancher_guidance"));
            case HALL_KEEPER_NAME -> player.sendSystemMessage(Component.translatable("message.countrysidedays.hall_guidance"));
            case CUSTOMER_NAME -> handleCustomer(player);
            default -> {
            }
        }
    }

    private static boolean isCountrysideNpc(String name) {
        return RESIDENT_NAME.equals(name)
                || FARMER_NAME.equals(name)
                || RANCHER_NAME.equals(name)
                || HALL_KEEPER_NAME.equals(name)
                || CUSTOMER_NAME.equals(name);
    }

    private static void handleResident(ServerPlayer player) {
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        if (data.customersServed() == 0) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.resident_first_guidance"));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.resident_progress",
                    data.customersServed(), data.villageCoinsEarned()
            ));
        }
    }

    private static void handleCustomer(ServerPlayer player) {
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
        long day = Math.max(0L, level.getGameTime() / 24000L);
        if (!data.recordCustomerService(day, DAILY_REWARD_COINS)) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_already_served"));
            return;
        }

        if (!player.getAbilities().instabuild) held.shrink(1);
        giveOrDrop(player, new ItemStack(ModItems.VILLAGE_COIN.get(), DAILY_REWARD_COINS));
        player.giveExperiencePoints(8);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.customer_served",
                DAILY_REWARD_COINS, data.customersServed()
        ));
    }

    private static boolean exists(ServerLevel level, NpcDefinition definition) {
        AABB area = new AABB(definition.home()).inflate(90.0, 12.0, 90.0);
        return !level.getEntitiesOfClass(
                Villager.class,
                area,
                villager -> definition.name().equals(villager.getName().getString())
        ).isEmpty();
    }

    private static void spawnVillager(ServerLevel level, NpcDefinition definition) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) {
            CountrysideDays.LOGGER.error("Minecraft villager entity type is unavailable");
            return;
        }

        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) {
            CountrysideDays.LOGGER.error("Failed to create countryside NPC {}", definition.name());
            return;
        }

        BlockPos pos = definition.home();
        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setCustomName(Component.literal(definition.name()));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setInvulnerable(true);
        if (!level.addFreshEntity(villager)) {
            CountrysideDays.LOGGER.error("Failed to add countryside NPC {}", definition.name());
        }
    }

    private static void ensureRanchAnimals(ServerLevel level, BlockPos origin) {
        AABB ranch = new AABB(origin.offset(10, 0, 44), origin.offset(27, 6, 66));
        if (level.getEntitiesOfClass(Mob.class, ranch, mob -> mob.getType().toString().contains("cow")).isEmpty()) {
            spawnAnimal(level, "cow", origin.offset(16, 1, 51));
            spawnAnimal(level, "cow", origin.offset(21, 1, 58));
        }
        if (level.getEntitiesOfClass(Mob.class, ranch, mob -> mob.getType().toString().contains("sheep")).isEmpty()) {
            spawnAnimal(level, "sheep", origin.offset(18, 1, 61));
            spawnAnimal(level, "sheep", origin.offset(23, 1, 53));
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

    private static NpcDefinition definitionByName(BlockPos origin, String name) {
        for (NpcDefinition definition : definitions(origin)) {
            if (definition.name().equals(name)) return definition;
        }
        return null;
    }

    private static List<NpcDefinition> definitions(BlockPos origin) {
        return List.of(
                new NpcDefinition(RESIDENT_NAME, origin.offset(29, 1, -20), origin.offset(-2, 1, 26)),
                new NpcDefinition(FARMER_NAME, origin.offset(-36, 1, 36), origin.offset(10, 1, -4)),
                new NpcDefinition(RANCHER_NAME, origin.offset(28, 1, 37), origin.offset(18, 1, 53)),
                new NpcDefinition(HALL_KEEPER_NAME, origin.offset(0, 1, 37), origin.offset(0, 1, 37)),
                new NpcDefinition(CUSTOMER_NAME, origin.offset(8, 1, 35), origin.offset(-5, 1, -2))
        );
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private record NpcDefinition(String name, BlockPos home, BlockPos work) {
    }
}
