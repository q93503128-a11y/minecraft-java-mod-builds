package dev.moonseungjun.augmentationsurvival.augment.resource;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative resource and farming runtime. */
public final class ResourceHarvestService {
    private static final String BUMPER_CROP_ID = id("bumper_crop");
    private static final String SEED_SAVER_ID = id("seed_saver");
    private static final String EXTRA_PIECE_ID = id("extra_piece");
    private static final String REPLANT_ID = id("replant");
    private static final String SWEEPING_HARVEST_ID = id("sweeping_harvest");
    private static final String TREE_FELLING_ID = id("tree_felling");
    private static final String TUNNELING_ID = id("tunneling");

    private static final ThreadLocal<DerivedBreakMode> DERIVED_BREAK_MODE =
            ThreadLocal.withInitial(() -> DerivedBreakMode.NONE);
    private static final Map<UUID, BreakSnapshot> BREAK_SNAPSHOTS = new HashMap<>();
    private static final List<PendingReplant> PENDING_REPLANTS = new ArrayList<>();
    private static final Map<UUID, Double> TUNNEL_DURABILITY_REFUND_CREDIT = new HashMap<>();
    private static final Map<Block, Item> PLANTING_ITEM_CACHE = new IdentityHashMap<>();

    private ResourceHarvestService() {
    }

    public static void initialize() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                BREAK_SNAPSHOTS.put(
                        serverPlayer.getUUID(),
                        new BreakSnapshot(copyPos(pos), serverPlayer.getMainHandItem().copy())
                );
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer && world instanceof ServerLevel serverLevel) {
                afterBlockBreak(serverLevel, serverPlayer, pos, state, blockEntity);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(ResourceHarvestService::tickPending);
        ServerPlayerEvents.JOIN.register(ResourceHarvestService::clearPlayerTransient);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> clearPlayerTransient(newPlayer)
        );
    }

    public static boolean isDerivedBreak() {
        return DERIVED_BREAK_MODE.get() != DerivedBreakMode.NONE;
    }

    private static void afterBlockBreak(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        AugmentationPlayerState augments = AugmentationPlayerStateService.state(player);
        DerivedBreakMode mode = DERIVED_BREAK_MODE.get();
        ItemStack tool = consumeToolSnapshot(player, pos);
        List<ItemStack> baseDrops = Block.getDrops(state, level, pos, blockEntity, player, tool);

        CropBlock crop = state.getBlock() instanceof CropBlock cropBlock ? cropBlock : null;
        boolean matureCrop = crop != null && crop.isMaxAge(state);
        if (matureCrop) {
            applyCropBonusDrops(level, player, pos, state, baseDrops, augments);
            boolean forceSweepReplant = mode == DerivedBreakMode.SWEEP_CROP;
            if (forceSweepReplant || boundedLevel(augments, REPLANT_ID) > 0) {
                scheduleReplant(level, player, pos, crop, state, forceSweepReplant);
            }
        }

        applyOreBonus(level, player, pos, state, baseDrops, augments);
        if (mode != DerivedBreakMode.NONE) {
            return;
        }
        if (matureCrop) {
            triggerSweepingHarvest(level, player, pos, state, crop, augments);
        }
        if (state.is(BlockTags.LOGS)) {
            triggerTreeFelling(player, pos, augments);
        }
        triggerTunneling(level, player, pos, state, augments);
    }

    private static void applyCropBonusDrops(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            List<ItemStack> baseDrops,
            AugmentationPlayerState augments
    ) {
        Item plantingItem = plantingItem(state.getBlock());
        int bumper = boundedLevel(augments, BUMPER_CROP_ID);
        if (bumper > 0
                && player.getRandom().nextDouble()
                < ResourceHarvestTuning.current().bumperCropChancePerLevel() * bumper) {
            Item cropProduct = cropProduct(baseDrops, plantingItem);
            if (cropProduct != null) {
                Block.popResource(level, pos, new ItemStack(cropProduct));
            }
        }

        int seedSaver = boundedLevel(augments, SEED_SAVER_ID);
        if (seedSaver > 0 && plantingItem != null
                && player.getRandom().nextDouble()
                < ResourceHarvestTuning.current().seedSaverChancePerLevel() * seedSaver) {
            Block.popResource(level, pos, new ItemStack(plantingItem));
        }
    }

    private static void applyOreBonus(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            List<ItemStack> baseDrops,
            AugmentationPlayerState augments
    ) {
        if (!state.is(ConventionalBlockTags.ORES)) {
            return;
        }
        int levelValue = boundedLevel(augments, EXTRA_PIECE_ID);
        if (levelValue <= 0
                || player.getRandom().nextDouble()
                >= ResourceHarvestTuning.current().extraPieceChancePerLevel() * levelValue) {
            return;
        }
        ItemStack primary = firstOrdinaryDrop(baseDrops);
        if (!primary.isEmpty()) {
            ItemStack one = primary.copy();
            one.setCount(1);
            Block.popResource(level, pos, one);
        }
    }

    private static void scheduleReplant(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            CropBlock crop,
            BlockState harvestedState,
            boolean forceSweepReplant
    ) {
        Item planting = plantingItem(harvestedState.getBlock());
        if (planting == null) {
            return;
        }
        long readyTick = level.getGameTime() + ResourceHarvestTuning.current().replantDelayTicks();
        PENDING_REPLANTS.add(new PendingReplant(
                level.dimension(),
                copyPos(pos),
                crop.getStateForAge(0),
                planting,
                player.getUUID(),
                readyTick,
                forceSweepReplant
        ));
    }

    private static void triggerSweepingHarvest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            BlockState rootState,
            CropBlock crop,
            AugmentationPlayerState augments
    ) {
        int augmentLevel = boundedLevel(augments, SWEEPING_HARVEST_ID);
        if (augmentLevel <= 0) {
            return;
        }
        ResourceHarvestTuning.Snapshot tuning = ResourceHarvestTuning.current();
        int horizontal = tuning.sweepingHarvestSearchRadius();
        int vertical = tuning.sweepingHarvestVerticalRadius();
        List<BlockPos> candidates = new ArrayList<>();
        for (int dy = -vertical; dy <= vertical; dy++) {
            for (int dx = -horizontal; dx <= horizontal; dx++) {
                for (int dz = -horizontal; dz <= horizontal; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos candidatePos = origin.offset(dx, dy, dz);
                    BlockState candidate = level.getBlockState(candidatePos);
                    if (candidate.getBlock() == rootState.getBlock() && crop.isMaxAge(candidate)) {
                        candidates.add(copyPos(candidatePos));
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingLong(candidate -> distanceSquared(origin, candidate)));
        int limit = Math.min(tuning.sweepingHarvestAdditional(augmentLevel), candidates.size());
        for (int index = 0; index < limit; index++) {
            destroyDerived(player, candidates.get(index), DerivedBreakMode.SWEEP_CROP);
        }
    }

    private static void triggerTreeFelling(
            ServerPlayer player,
            BlockPos origin,
            AugmentationPlayerState augments
    ) {
        int augmentLevel = boundedLevel(augments, TREE_FELLING_ID);
        if (augmentLevel <= 0) {
            return;
        }
        ResourceHarvestTuning.Snapshot tuning = ResourceHarvestTuning.current();
        int limit = tuning.treeFellingAdditional(augmentLevel);
        int maxNodes = tuning.treeFellingMaxSearchNodes();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        visited.add(origin.asLong());
        for (Direction direction : Direction.values()) {
            queue.add(copyPos(origin.relative(direction)));
        }

        int broken = 0;
        int inspected = 0;
        while (!queue.isEmpty() && broken < limit && inspected < maxNodes) {
            BlockPos candidate = queue.removeFirst();
            if (!visited.add(candidate.asLong())) {
                continue;
            }
            inspected++;
            BlockState state = player.level().getBlockState(candidate);
            if (!state.is(BlockTags.LOGS)) {
                continue;
            }
            if (destroyDerived(player, candidate, DerivedBreakMode.TREE_FELLING)) {
                broken++;
                for (Direction direction : Direction.values()) {
                    queue.add(copyPos(candidate.relative(direction)));
                }
            }
        }
    }

    private static void triggerTunneling(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            BlockState rootState,
            AugmentationPlayerState augments
    ) {
        int augmentLevel = boundedLevel(augments, TUNNELING_ID);
        if (augmentLevel <= 0
                || rootState.is(ConventionalBlockTags.ORES)
                || rootState.is(BlockTags.LOGS)
                || rootState.getBlock() instanceof CropBlock
                || rootState.hasBlockEntity()) {
            return;
        }

        int[] step = miningDirection(player, origin);
        int limit = ResourceHarvestTuning.current().tunnelingAdditional(augmentLevel);
        for (int index = 1; index <= limit; index++) {
            BlockPos target = origin.offset(step[0] * index, step[1] * index, step[2] * index);
            BlockState targetState = level.getBlockState(target);
            if (targetState.getBlock() != rootState.getBlock()
                    || targetState.is(ConventionalBlockTags.ORES)
                    || targetState.hasBlockEntity()) {
                break;
            }
            ItemStack before = player.getMainHandItem();
            Item beforeItem = before.isEmpty() ? null : before.getItem();
            int beforeDamage = before.isDamageableItem() ? before.getDamageValue() : 0;
            if (!destroyDerived(player, target, DerivedBreakMode.TUNNELING)) {
                break;
            }
            applyTunnelingDurabilityRefund(player, beforeItem, beforeDamage);
        }
    }

    private static void applyTunnelingDurabilityRefund(ServerPlayer player, Item originalItem, int beforeDamage) {
        if (originalItem == null) {
            return;
        }
        ItemStack after = player.getMainHandItem();
        if (after.isEmpty() || !after.is(originalItem) || !after.isDamageableItem()) {
            return;
        }
        int delta = after.getDamageValue() - beforeDamage;
        if (delta <= 0) {
            return;
        }
        double refundFraction = 1.0D - ResourceHarvestTuning.current().tunnelingDurabilityMultiplier();
        double credit = TUNNEL_DURABILITY_REFUND_CREDIT.getOrDefault(player.getUUID(), 0.0D)
                + delta * refundFraction;
        int refund = (int) Math.floor(credit);
        if (refund > 0) {
            after.setDamageValue(Math.max(0, after.getDamageValue() - refund));
            credit -= refund;
        }
        if (credit > 0.0D) {
            TUNNEL_DURABILITY_REFUND_CREDIT.put(player.getUUID(), credit);
        } else {
            TUNNEL_DURABILITY_REFUND_CREDIT.remove(player.getUUID());
        }
    }

    private static void tickPending(MinecraftServer server) {
        tickReplants(server);
    }

    private static void tickReplants(MinecraftServer server) {
        Iterator<PendingReplant> iterator = PENDING_REPLANTS.iterator();
        while (iterator.hasNext()) {
            PendingReplant pending = iterator.next();
            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null || level.getGameTime() < pending.readyTick()) {
                continue;
            }
            iterator.remove();
            if (!level.getBlockState(pending.pos()).isAir()) {
                continue;
            }
            if (!consumeNearbyPlantingItem(level, pending.pos(), pending.plantingItem())) {
                continue;
            }
            level.setBlock(pending.pos(), pending.replantedState(), Block.UPDATE_ALL);
        }
    }

    private static boolean consumeNearbyPlantingItem(ServerLevel level, BlockPos pos, Item plantingItem) {
        double radius = ResourceHarvestTuning.current().replantPickupRadius();
        AABB box = new AABB(
                pos.getX() - radius,
                pos.getY() - radius,
                pos.getZ() - radius,
                pos.getX() + 1.0D + radius,
                pos.getY() + 1.0D + radius,
                pos.getZ() + 1.0D + radius
        );
        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class,
                box,
                item -> !item.getItem().isEmpty() && item.getItem().is(plantingItem)
        );
        if (items.isEmpty()) {
            return false;
        }
        ItemEntity item = items.getFirst();
        ItemStack stack = item.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        return true;
    }

    private static ItemStack consumeToolSnapshot(ServerPlayer player, BlockPos pos) {
        BreakSnapshot snapshot = BREAK_SNAPSHOTS.remove(player.getUUID());
        if (snapshot != null && snapshot.pos().equals(pos)) {
            return snapshot.tool();
        }
        return player.getMainHandItem().copy();
    }

    private static Item plantingItem(Block block) {
        if (PLANTING_ITEM_CACHE.containsKey(block)) {
            return PLANTING_ITEM_CACHE.get(block);
        }
        Item found = null;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof BlockItem blockItem && blockItem.getBlock() == block) {
                found = item;
                break;
            }
        }
        PLANTING_ITEM_CACHE.put(block, found);
        return found;
    }

    private static Item cropProduct(List<ItemStack> drops, Item plantingItem) {
        for (ItemStack stack : drops) {
            if (!stack.isEmpty() && stack.getItem() != plantingItem) {
                return stack.getItem();
            }
        }
        return plantingItem;
    }

    private static ItemStack firstOrdinaryDrop(List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (!stack.isEmpty() && !isCore(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isCore(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (AugmentTier tier : AugmentTier.values()) {
            if (stack.is(AugmentCoreItems.item(tier))) {
                return true;
            }
        }
        return false;
    }

    private static boolean destroyDerived(ServerPlayer player, BlockPos pos, DerivedBreakMode mode) {
        DerivedBreakMode previous = DERIVED_BREAK_MODE.get();
        DERIVED_BREAK_MODE.set(mode);
        try {
            return player.gameMode.destroyBlock(pos);
        } finally {
            DERIVED_BREAK_MODE.set(previous);
        }
    }

    private static int[] miningDirection(ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        double dx = pos.getX() + 0.5D - eye.x;
        double dy = pos.getY() + 0.5D - eye.y;
        double dz = pos.getZ() + 0.5D - eye.z;
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ax >= ay && ax >= az) {
            return new int[]{dx >= 0.0D ? 1 : -1, 0, 0};
        }
        if (ay >= ax && ay >= az) {
            return new int[]{0, dy >= 0.0D ? 1 : -1, 0};
        }
        return new int[]{0, 0, dz >= 0.0D ? 1 : -1};
    }

    private static void clearPlayerTransient(ServerPlayer player) {
        BREAK_SNAPSHOTS.remove(player.getUUID());
        TUNNEL_DURABILITY_REFUND_CREDIT.remove(player.getUUID());
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static long distanceSquared(BlockPos first, BlockPos second) {
        long dx = first.getX() - second.getX();
        long dy = first.getY() - second.getY();
        long dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static BlockPos copyPos(BlockPos pos) {
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private enum DerivedBreakMode {
        NONE,
        SWEEP_CROP,
        TREE_FELLING,
        TUNNELING
    }

    private record BreakSnapshot(BlockPos pos, ItemStack tool) {
    }

    private record PendingReplant(
            ResourceKey<Level> dimension,
            BlockPos pos,
            BlockState replantedState,
            Item plantingItem,
            UUID playerId,
            long readyTick,
            boolean forcedBySweep
    ) {
    }
}
