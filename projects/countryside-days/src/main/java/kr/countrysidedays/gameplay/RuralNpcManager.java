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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public final class RuralNpcManager {
    public static final String RESIDENT_NAME = "복순 할머니";
    public static final String CUSTOMER_NAME = "나들이 손님 민수";
    public static final int DAILY_REWARD_COINS = 4;

    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");

    private RuralNpcManager() {
    }

    public static void ensureForHomestead(ServerLevel level, BlockPos origin) {
        for (NpcDefinition definition : definitions(origin)) {
            if (!exists(level, definition)) {
                spawn(level, definition);
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
        if (!RESIDENT_NAME.equals(name) && !CUSTOMER_NAME.equals(name)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (RESIDENT_NAME.equals(name)) {
            handleResident(player);
            return;
        }

        handleCustomer(player);
    }

    private static void handleResident(ServerPlayer player) {
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        if (data.customersServed() == 0) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.resident_first_guidance"));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.resident_progress",
                    data.customersServed(),
                    data.villageCoinsEarned()
            ));
        }
    }

    private static void handleCustomer(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.COUNTRY_STEW.get())) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_order"));
            return;
        }

        ServerLevel level = player.level();
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        long day = Math.max(0L, level.getDayTime() / 24000L);
        if (!data.recordCustomerService(day, DAILY_REWARD_COINS)) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.customer_already_served"));
            return;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        giveOrDrop(player, new ItemStack(ModItems.VILLAGE_COIN.get(), DAILY_REWARD_COINS));
        player.giveExperiencePoints(8);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.customer_served",
                DAILY_REWARD_COINS,
                data.customersServed()
        ));
    }

    private static boolean exists(ServerLevel level, NpcDefinition definition) {
        AABB area = new AABB(
                definition.position().getX() - 6.0,
                definition.position().getY() - 3.0,
                definition.position().getZ() - 6.0,
                definition.position().getX() + 6.0,
                definition.position().getY() + 5.0,
                definition.position().getZ() + 6.0
        );
        return !level.getEntitiesOfClass(
                Villager.class,
                area,
                villager -> definition.name().equals(villager.getName().getString())
        ).isEmpty();
    }

    private static void spawn(ServerLevel level, NpcDefinition definition) {
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

        BlockPos pos = definition.position();
        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setCustomName(Component.literal(definition.name()));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setInvulnerable(true);
        villager.setNoAi(true);
        if (!level.addFreshEntity(villager)) {
            CountrysideDays.LOGGER.error("Failed to add countryside NPC {}", definition.name());
        }
    }

    private static List<NpcDefinition> definitions(BlockPos origin) {
        return List.of(
                new NpcDefinition(RESIDENT_NAME, origin.offset(-3, 1, 4)),
                new NpcDefinition(CUSTOMER_NAME, origin.offset(-5, 1, -2))
        );
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private record NpcDefinition(String name, BlockPos position) {
    }
}
