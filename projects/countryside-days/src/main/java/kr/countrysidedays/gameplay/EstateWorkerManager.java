package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Player-hired farm and ranch workers with a daily wage paid from estate storage. */
public final class EstateWorkerManager {
    public static final String FARM_ROLE = "farm";
    public static final String RANCH_ROLE = "ranch";

    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final String CONTRACT_TAG = "cd_worker_contract_v2";
    private static final String OWNER_PREFIX = "cd_worker_owner_";
    private static final String ROLE_PREFIX = "cd_worker_role_";
    private static final String PAID_DAY_PREFIX = "cd_worker_paid_day_";
    private static final String CHECK_DAY_PREFIX = "cd_worker_check_day_";
    private static final String MISSED_PREFIX = "cd_worker_missed_";

    public static final int HIRING_FEE = 12;
    public static final int DAILY_WAGE = 2;
    public static final int MAX_MISSED_WAGES = 3;

    private EstateWorkerManager() {
    }

    /** Handles hiring from the public farmer/rancher and status or dismissal on a hired worker. */
    public static boolean handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Villager villager)) return false;

        if (villager.entityTags().contains(CONTRACT_TAG)) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
            handleContractWorker(player, villager);
            return true;
        }

        String role = roleOfferedBy(villager.getName().getString());
        if (role == null || !player.isShiftKeyDown()) return false;

        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
        hire(player, role);
        return true;
    }

    public static void maintain(
            ServerLevel level,
            BlockPos villageOrigin,
            CountrysideWorldData data
    ) {
        long day = gameDay(level);
        AABB search = new AABB(villageOrigin).inflate(1400.0, 64.0, 1400.0);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, search)) {
            boolean looksLikeOldWorker = hasPrefix(villager, OWNER_PREFIX) || hasPrefix(villager, ROLE_PREFIX);
            if (looksLikeOldWorker && !villager.entityTags().contains(CONTRACT_TAG)) {
                villager.discard();
                continue;
            }
            if (!villager.entityTags().contains(CONTRACT_TAG)) continue;

            UUID owner = ownerUuid(villager).orElse(null);
            String role = role(villager).orElse(null);
            CountrysideWorldData.PlayerEstate estate = owner == null ? null : data.estate(owner).orElse(null);
            if (estate == null || role == null) {
                villager.discard();
                continue;
            }

            long checkedDay = getLongTag(villager, CHECK_DAY_PREFIX, -1L);
            if (checkedDay >= day) {
                refreshName(villager, estate, role, getIntTag(villager, MISSED_PREFIX, 0));
                continue;
            }
            setLongTag(villager, CHECK_DAY_PREFIX, day);

            if (consumeDailyWage(level, estate, role)) {
                setLongTag(villager, PAID_DAY_PREFIX, day);
                setIntTag(villager, MISSED_PREFIX, 0);
                refreshName(villager, estate, role, 0);
                continue;
            }

            int missed = getIntTag(villager, MISSED_PREFIX, 0) + 1;
            setIntTag(villager, MISSED_PREFIX, missed);
            refreshName(villager, estate, role, missed);
            if (missed >= MAX_MISSED_WAGES) villager.discard();
        }
    }

    public static boolean isActive(Villager villager, long day) {
        return villager.entityTags().contains(CONTRACT_TAG)
                && getLongTag(villager, PAID_DAY_PREFIX, -1L) >= day
                && getIntTag(villager, MISSED_PREFIX, 0) == 0;
    }

    public static Optional<Villager> findWorker(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            String role
    ) {
        String ownerTag = OWNER_PREFIX + estate.ownerUuid();
        String roleTag = ROLE_PREFIX + role;
        return level.getEntitiesOfClass(
                Villager.class,
                new AABB(estate.originPos()).inflate(220.0, 32.0, 220.0),
                villager -> villager.entityTags().contains(CONTRACT_TAG)
                        && villager.entityTags().contains(ownerTag)
                        && villager.entityTags().contains(roleTag)
        ).stream().findFirst();
    }

    private static void hire(ServerPlayer player, String role) {
        ServerLevel level = player.level();
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        CountrysideWorldData.PlayerEstate estate = data.estate(player.getUUID()).orElse(null);
        if (estate == null) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.worker_no_estate"));
            return;
        }
        if (findWorker(level, estate, role).isPresent()) {
            player.sendSystemMessage(Component.translatable("message.countrysidedays.worker_already_hired"));
            return;
        }
        if (!removeCoins(player, HIRING_FEE)) {
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.worker_hire_need_coins",
                    HIRING_FEE
            ));
            return;
        }

        BlockPos spawn = FARM_ROLE.equals(role)
                ? PlayerEstateLayout.farmGate(estate.originPos()).above()
                : PlayerEstateLayout.ranchGate(estate.originPos()).above();
        Villager worker = spawnWorker(level, estate, role, spawn);
        if (worker == null) {
            giveCoins(player, HIRING_FEE);
            player.sendSystemMessage(Component.translatable("message.countrysidedays.worker_hire_failed"));
            return;
        }

        long day = gameDay(level);
        setLongTag(worker, PAID_DAY_PREFIX, day);
        setLongTag(worker, CHECK_DAY_PREFIX, day);
        setIntTag(worker, MISSED_PREFIX, 0);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.worker_hired",
                roleDisplay(role), HIRING_FEE, DAILY_WAGE
        ));
    }

    private static void handleContractWorker(ServerPlayer player, Villager worker) {
        UUID owner = ownerUuid(worker).orElse(null);
        String role = role(worker).orElse(FARM_ROLE);
        if (owner == null || !owner.equals(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.worker_not_employer"));
            return;
        }

        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            worker.discard();
            player.sendSystemMessage(Component.translatable(
                    "message.countrysidedays.worker_dismissed",
                    roleDisplay(role)
            ));
            return;
        }

        long day = gameDay(player.level());
        boolean paid = isActive(worker, day);
        int missed = getIntTag(worker, MISSED_PREFIX, 0);
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.worker_status",
                roleDisplay(role),
                paid
                        ? Component.translatable("message.countrysidedays.worker_paid")
                        : Component.translatable("message.countrysidedays.worker_unpaid"),
                DAILY_WAGE,
                missed
        ));
    }

    private static Villager spawnWorker(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            String role,
            BlockPos pos
    ) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (type == null) return null;
        Entity created = type.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return null;

        villager.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        villager.setPersistenceRequired();
        villager.setInvulnerable(true);
        villager.setCustomNameVisible(false);
        villager.addTag(CONTRACT_TAG);
        villager.addTag(OWNER_PREFIX + estate.ownerUuid());
        villager.addTag(ROLE_PREFIX + role);
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(
                        level.registryAccess(),
                        FARM_ROLE.equals(role) ? VillagerProfession.FARMER : VillagerProfession.SHEPHERD
                )
                .withLevel(2));
        refreshName(villager, estate, role, 0);
        return level.addFreshEntity(villager) ? villager : null;
    }

    private static boolean consumeDailyWage(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            String role
    ) {
        BlockPos storagePos = FARM_ROLE.equals(role)
                ? PlayerEstateLayout.farmStorageBarrel(estate.originPos())
                : PlayerEstateLayout.ranchSupplyBarrel(estate.originPos());
        if (!(level.getBlockEntity(storagePos) instanceof Container container)) return false;
        if (countCoins(container) < DAILY_WAGE) return false;
        removeCoins(container, DAILY_WAGE);
        return true;
    }

    private static int countCoins(Container container) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(ModItems.VILLAGE_COIN.get())) count += stack.getCount();
        }
        return count;
    }

    private static void removeCoins(Container container, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.is(ModItems.VILLAGE_COIN.get())) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        container.setChanged();
    }

    private static boolean removeCoins(ServerPlayer player, int amount) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.VILLAGE_COIN.get())) count += stack.getCount();
        }
        if (count < amount) return false;

        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(ModItems.VILLAGE_COIN.get())) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        return true;
    }

    private static void giveCoins(ServerPlayer player, int amount) {
        ItemStack coins = new ItemStack(ModItems.VILLAGE_COIN.get(), amount);
        if (!player.getInventory().add(coins)) player.drop(coins, false);
    }

    private static String roleOfferedBy(String name) {
        if (RuralNpcManager.FARMER_NAME.equals(name)) return FARM_ROLE;
        if (RuralNpcManager.RANCHER_NAME.equals(name)) return RANCH_ROLE;
        return null;
    }

    private static Optional<UUID> ownerUuid(Villager villager) {
        return villager.entityTags().stream()
                .filter(tag -> tag.startsWith(OWNER_PREFIX))
                .map(tag -> tag.substring(OWNER_PREFIX.length()))
                .flatMap(value -> {
                    try {
                        return java.util.stream.Stream.of(UUID.fromString(value));
                    } catch (IllegalArgumentException ignored) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .findFirst();
    }

    private static Optional<String> role(Villager villager) {
        return villager.entityTags().stream()
                .filter(tag -> tag.startsWith(ROLE_PREFIX))
                .map(tag -> tag.substring(ROLE_PREFIX.length()))
                .filter(value -> FARM_ROLE.equals(value) || RANCH_ROLE.equals(value))
                .findFirst();
    }

    private static boolean hasPrefix(Villager villager, String prefix) {
        return villager.entityTags().stream().anyMatch(tag -> tag.startsWith(prefix));
    }

    private static void refreshName(
            Villager villager,
            CountrysideWorldData.PlayerEstate estate,
            String role,
            int missed
    ) {
        String suffix = missed <= 0 ? "" : " [임금 미지급 " + missed + "일]";
        villager.setCustomName(Component.literal(
                roleDisplay(role) + " · " + estate.ownerName() + suffix
        ));
    }

    private static String roleDisplay(String role) {
        return FARM_ROLE.equals(role) ? "농장 일꾼 새봄" : "목장 일꾼 태호";
    }

    private static long gameDay(ServerLevel level) {
        return Math.max(0L, level.getGameTime() / 24000L);
    }

    private static int getIntTag(Villager villager, String prefix, int fallback) {
        return (int) getLongTag(villager, prefix, fallback);
    }

    private static long getLongTag(Villager villager, String prefix, long fallback) {
        for (String tag : villager.entityTags()) {
            if (!tag.startsWith(prefix)) continue;
            try {
                return Long.parseLong(tag.substring(prefix.length()));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static void setIntTag(Villager villager, String prefix, int value) {
        setLongTag(villager, prefix, value);
    }

    private static void setLongTag(Villager villager, String prefix, long value) {
        Set<String> tags = Set.copyOf(villager.entityTags());
        for (String tag : tags) if (tag.startsWith(prefix)) villager.removeTag(tag);
        villager.addTag(prefix + value);
    }
}
