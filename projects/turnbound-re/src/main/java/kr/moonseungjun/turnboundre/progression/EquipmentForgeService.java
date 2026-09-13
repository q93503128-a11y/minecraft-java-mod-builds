package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.data.EquipmentDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Production bridge between Minecraft inventory and TURNBOUND equipment progression.
 * Material extraction is transactional and progression replacement is compare-and-set against
 * the exact server-owned state used to calculate the cost.
 */
public final class EquipmentForgeService {
    public static final String CODE_STALE = "STALE";
    public static final String CODE_FORGE_UNAVAILABLE = "FORGE_UNAVAILABLE";
    public static final String CODE_INVALID_MATERIAL = "INVALID_MATERIAL";
    private static final int FORGE_HORIZONTAL_RANGE = 4;
    private static final int FORGE_VERTICAL_RANGE = 2;

    public record Result(String code, PlayerProgress state, String detail) {
        public Result {
            if (code == null || state == null) throw new IllegalArgumentException("code/state required");
            detail = detail == null ? "" : detail;
        }
        public boolean accepted() { return "ACCEPTED".equals(code); }
    }

    private final DefinitionRepository definitions;
    private final PlayerProgressStore progressStore;

    public EquipmentForgeService(DefinitionRepository definitions, PlayerProgressStore progressStore) {
        if (definitions == null || progressStore == null) throw new IllegalArgumentException("definitions/progressStore required");
        this.definitions = definitions;
        this.progressStore = progressStore;
    }

    public Result craft(ServerPlayer player, String equipmentId, int expectedLevel) {
        return forge(player, equipmentId, expectedLevel, true);
    }

    public Result upgrade(ServerPlayer player, String equipmentId, int expectedLevel) {
        return forge(player, equipmentId, expectedLevel, false);
    }

    public Result equip(
            ServerPlayer player,
            String characterId,
            String equipmentId,
            int expectedLevel,
            String expectedEquippedId
    ) {
        Context context = context(player);
        PlayerProgress current = context.progress();
        if (!matchesEquipmentLevel(current, equipmentId, expectedLevel)
                || !current.equippedEquipment().getOrDefault(characterId, "").equals(safe(expectedEquippedId))) {
            return new Result(CODE_STALE, current, equipmentId);
        }
        EquipmentProgressionService.Result planned = new EquipmentProgressionService(context.definitions())
                .equip(current, characterId, equipmentId);
        if (!planned.accepted()) return from(planned);
        if (!progressStore.replaceIfCurrent(context.server(), player.getUUID(), current, planned.state())) {
            return new Result(CODE_STALE, progressStore.getOrCreate(context.server(), player.getUUID()), equipmentId);
        }
        return from(planned);
    }

    public Result unequip(
            ServerPlayer player,
            String characterId,
            String equipmentId,
            int expectedLevel,
            String expectedEquippedId
    ) {
        Context context = context(player);
        PlayerProgress current = context.progress();
        if (!matchesEquipmentLevel(current, equipmentId, expectedLevel)
                || !current.equippedEquipment().getOrDefault(characterId, "").equals(safe(expectedEquippedId))) {
            return new Result(CODE_STALE, current, equipmentId);
        }
        EquipmentProgressionService.Result planned = new EquipmentProgressionService(context.definitions())
                .unequip(current, characterId);
        if (!planned.accepted()) return from(planned);
        if (!progressStore.replaceIfCurrent(context.server(), player.getUUID(), current, planned.state())) {
            return new Result(CODE_STALE, progressStore.getOrCreate(context.server(), player.getUUID()), equipmentId);
        }
        return from(planned);
    }

    private Result forge(ServerPlayer player, String equipmentId, int expectedLevel, boolean craft) {
        Context context = context(player);
        PlayerProgress current = context.progress();
        if (!forgeAvailable(player)) return new Result(CODE_FORGE_UNAVAILABLE, current, equipmentId);
        if (!matchesEquipmentLevel(current, equipmentId, expectedLevel)) {
            return new Result(CODE_STALE, current, equipmentId);
        }

        EquipmentDefinition definition = context.definitions().equipment().get(equipmentId);
        if (definition == null) {
            return new Result(EquipmentProgressionService.ResultCode.UNKNOWN_EQUIPMENT.name(), current, equipmentId);
        }
        Item material = resolveItem(definition.ingredientItem());
        if (material == null) return new Result(CODE_INVALID_MATERIAL, current, definition.ingredientItem());

        ResourceHandler<ItemResource> inventory = PlayerInventoryWrapper.of(player).getMainSlots();
        int available = countItem(inventory, material);
        EquipmentProgressionService service = new EquipmentProgressionService(context.definitions());
        EquipmentProgressionService.Result planned = craft
                ? service.craft(current, equipmentId, available)
                : service.upgrade(current, equipmentId, available);
        if (!planned.accepted()) return from(planned);

        int required = planned.materialCost().count();
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = extractItem(inventory, material, required, transaction);
            if (extracted != required) {
                return new Result(EquipmentProgressionService.ResultCode.INSUFFICIENT_MATERIAL.name(), current,
                        extracted + "/" + required);
            }
            if (!progressStore.replaceIfCurrent(context.server(), player.getUUID(), current, planned.state())) {
                return new Result(CODE_STALE, progressStore.getOrCreate(context.server(), player.getUUID()), equipmentId);
            }
            try {
                transaction.commit();
            } catch (RuntimeException commitFailure) {
                boolean restored = progressStore.replaceIfCurrent(context.server(), player.getUUID(), planned.state(), current);
                if (!restored) {
                    TurnboundRe.LOGGER.error("TURNBOUND equipment forge inventory commit failed after progression write and rollback lost CAS for {}",
                            player.getUUID(), commitFailure);
                }
                throw commitFailure;
            }
        }
        return from(planned);
    }

    public Map<String, Integer> materialCounts(ServerPlayer player, DefinitionRegistry registry) {
        if (player == null || registry == null) throw new IllegalArgumentException("player/registry required");
        ResourceHandler<ItemResource> inventory = PlayerInventoryWrapper.of(player).getMainSlots();
        Map<String, Integer> out = new LinkedHashMap<>();
        for (EquipmentDefinition definition : registry.equipment().values()) {
            Item item = resolveItem(definition.ingredientItem());
            out.put(definition.ingredientItem(), item == null ? 0 : countItem(inventory, item));
        }
        return Map.copyOf(out);
    }

    /** Physical production gate. Final authored Hub forge art/placement can replace this vanilla workstation bridge later. */
    public static boolean forgeAvailable(ServerPlayer player) {
        if (player == null) return false;
        BlockPos origin = player.blockPosition();
        BlockPos min = origin.offset(-FORGE_HORIZONTAL_RANGE, -FORGE_VERTICAL_RANGE, -FORGE_HORIZONTAL_RANGE);
        BlockPos max = origin.offset(FORGE_HORIZONTAL_RANGE, FORGE_VERTICAL_RANGE, FORGE_HORIZONTAL_RANGE);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (player.level().getBlockState(pos).is(Blocks.SMITHING_TABLE)) return true;
        }
        return false;
    }

    static int countItem(ResourceHandler<ItemResource> inventory, Item item) {
        if (inventory == null || item == null || item == Items.AIR) return 0;
        long total = 0L;
        for (int index = 0; index < inventory.size(); index++) {
            ItemResource resource = inventory.getResource(index);
            if (!resource.isEmpty() && resource.value() == item) {
                total += inventory.getAmountAsLong(index);
                if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    static int extractItem(
            ResourceHandler<ItemResource> inventory,
            Item item,
            int amount,
            TransactionContext transaction
    ) {
        if (inventory == null || item == null || transaction == null) throw new IllegalArgumentException("inventory/item/transaction required");
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        int extracted = 0;
        for (int index = 0; index < inventory.size() && extracted < amount; index++) {
            ItemResource resource = inventory.getResource(index);
            if (resource.isEmpty() || resource.value() != item) continue;
            extracted += inventory.extract(index, resource, amount - extracted, transaction);
        }
        return extracted;
    }

    private static Item resolveItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (RuntimeException invalidId) {
            return null;
        }
    }

    private Context context(ServerPlayer player) {
        if (player == null) throw new IllegalArgumentException("player required");
        MinecraftServer server = player.level().getServer();
        if (server == null) throw new IllegalStateException("server unavailable");
        DefinitionRegistry registry = definitions.snapshot().registry();
        PlayerProgress progress = progressStore.getOrCreate(server, player.getUUID());
        return new Context(server, registry, progress);
    }

    private static boolean matchesEquipmentLevel(PlayerProgress progress, String equipmentId, int expectedLevel) {
        if (expectedLevel < 0) return false;
        EquipmentProgress equipment = progress.equipment().get(equipmentId);
        int actual = equipment == null ? 0 : equipment.level();
        return actual == expectedLevel;
    }

    private static Result from(EquipmentProgressionService.Result result) {
        return new Result(result.code().name(), result.state(), result.detail());
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private record Context(MinecraftServer server, DefinitionRegistry definitions, PlayerProgress progress) {}
}
