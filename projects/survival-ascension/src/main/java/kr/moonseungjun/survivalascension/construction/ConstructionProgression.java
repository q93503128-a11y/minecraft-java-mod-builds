package kr.moonseungjun.survivalascension.construction;

/*
 * Placement validation/order is independently implemented from patterns studied in
 * Building Gadgets 2 (Direwolf20-MC, MIT): material checks, mayInteract protection,
 * NeoForge placement hooks, and tick-budgeted bulk work.
 */

import kr.moonseungjun.survivalascension.expedition.ExpeditionProgression;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ConstructionProgression {
    private static final int GLOBAL_BLOCK_BUDGET_PER_TICK = 64;
    private static final int MAX_PENDING_BLOCKS_PER_PLAYER = 512;
    private static final Map<UUID, ConstructionMode> MODES = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_COUNTS = new HashMap<>();
    private static final Deque<BuildJob> JOBS = new ArrayDeque<>();
    private static final Set<UUID> INTERNAL_PLACE_GUARD = new HashSet<>();

    private ConstructionProgression() {}

    public static void setMode(ServerPlayer player, ConstructionMode requested) {
        int level = SkillProgressData.get(player).level(player, SkillType.CONSTRUCTION);
        ConstructionMode resolved = requested.requiredLevel() <= level ? requested : ConstructionMode.SINGLE;
        if (resolved == ConstructionMode.VOLUME && !InfrastructureData.get(player).isComplete(InfrastructureProject.BUILDER_FOUNDRY)) {
            resolved = ConstructionMode.SINGLE;
            player.sendSystemMessage(Component.literal("§6[건축] §f입체 모드는 §eM→인프라→건축 공방§f 완공이 필요합니다."));
        }
        MODES.put(player.getUUID(), resolved);
        if (resolved != requested) {
            if (requested != ConstructionMode.VOLUME || level < requested.requiredLevel()) {
                player.sendSystemMessage(Component.literal("§6[건축] §f" + requested.koreanName() + " 모드는 Lv." + requested.requiredLevel() + "부터 사용할 수 있습니다."));
            }
        } else {
            player.sendSystemMessage(Component.literal("§6[건축] §f배치 모드: §e" + resolved.koreanName()));
        }
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (INTERNAL_PLACE_GUARD.contains(uuid)) return;
        if (event instanceof BlockEvent.EntityMultiPlaceEvent) return;
        if (player.isCreative() || player.isSpectator()) return;

        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock().asItem() instanceof BlockItem) || state.hasBlockEntity()) return;

        announceMilestones(player, SkillProgressionService.award(player, SkillType.CONSTRUCTION, 2L));
        if (player.isShiftKeyDown()) return;

        int level = SkillProgressData.get(player).level(player, SkillType.CONSTRUCTION);
        ConstructionMode mode = MODES.getOrDefault(uuid, ConstructionMode.SINGLE);
        if (mode == ConstructionMode.VOLUME && !InfrastructureData.get(player).isComplete(InfrastructureProject.BUILDER_FOUNDRY)) mode = ConstructionMode.SINGLE;
        if (mode == ConstructionMode.SINGLE || mode.requiredLevel() > level) return;

        List<BlockPos> targets = computeTargets(player, event.getPos(), mode, level);
        if (targets.isEmpty()) return;
        int pending = PENDING_COUNTS.getOrDefault(uuid, 0);
        int allowance = Math.max(0, MAX_PENDING_BLOCKS_PER_PLAYER - pending);
        if (allowance <= 0) {
            player.sendSystemMessage(Component.literal("§6[건축] §f대기 중인 대량 건축 작업이 너무 많습니다."));
            return;
        }
        if (targets.size() > allowance) targets = new ArrayList<>(targets.subList(0, allowance));
        JOBS.addLast(new BuildJob(uuid, player.level().dimension(), state, targets));
        PENDING_COUNTS.put(uuid, pending + targets.size());
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        int budget = GLOBAL_BLOCK_BUDGET_PER_TICK;
        int rotations = JOBS.size();
        while (budget > 0 && rotations-- > 0 && !JOBS.isEmpty()) {
            BuildJob job = JOBS.removeFirst();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(job.playerId);
            ServerLevel level = event.getServer().getLevel(job.dimension);
            if (player == null || level == null || player.isSpectator()) {
                removePending(job.playerId, job.remaining());
                continue;
            }

            int localBudget = Math.min(8, budget);
            while (localBudget-- > 0 && budget-- > 0 && job.hasNext()) {
                BlockPos target = job.next();
                removePending(job.playerId, 1);
                PlaceResult result = tryPlace(player, level, job.state, target);
                if (result == PlaceResult.PLACED) job.placed++;
                if (result == PlaceResult.OUT_OF_MATERIAL) {
                    removePending(job.playerId, job.remaining());
                    job.clear();
                    player.sendSystemMessage(Component.literal("§6[건축] §f재료가 부족해 대량 배치를 중단했습니다."));
                    break;
                }
            }

            if (job.hasNext()) {
                JOBS.addLast(job);
            } else if (job.placed > 0) {
                announceMilestones(player, SkillProgressionService.award(player, SkillType.CONSTRUCTION, job.placed));
            }
        }
    }

    private static PlaceResult tryPlace(ServerPlayer player, ServerLevel level, BlockState state, BlockPos target) {
        if (!level.mayInteract(player, target)) return PlaceResult.SKIPPED;
        if (!level.getBlockState(target).canBeReplaced()) return PlaceResult.SKIPPED;
        if (!state.canSurvive(level, target)) return PlaceResult.SKIPPED;

        Item item = state.getBlock().asItem();
        if (item == Items.AIR || !(item instanceof BlockItem)) return PlaceResult.SKIPPED;
        if (!player.isCreative() && !hasMaterial(player, item)) return PlaceResult.OUT_OF_MATERIAL;

        UUID uuid = player.getUUID();
        INTERNAL_PLACE_GUARD.add(uuid);
        boolean denied;
        try {
            denied = EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, target), Direction.UP);
        } finally {
            INTERNAL_PLACE_GUARD.remove(uuid);
        }
        if (denied) return PlaceResult.SKIPPED;
        if (!level.setBlockAndUpdate(target, state)) return PlaceResult.SKIPPED;
        if (!player.isCreative()) consumeOne(player, item);
        return PlaceResult.PLACED;
    }

    private static boolean hasMaterial(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) return true;
        }
        return false;
    }

    private static void consumeOne(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            stack.shrink(1);
            player.getInventory().setChanged();
            return;
        }
    }

    private static List<BlockPos> computeTargets(ServerPlayer player, BlockPos center, ConstructionMode mode, int level) {
        List<BlockPos> targets = new ArrayList<>();
        double xLook = Math.abs(player.getLookAngle().x);
        double zLook = Math.abs(player.getLookAngle().z);
        boolean lookingMostlyX = xLook >= zLook;
        boolean fieldMastery = level >= 100 && ExpeditionProgression.hasFieldMastery(player);

        if (mode == ConstructionMode.LINE) {
            int size = fieldMastery ? 65 : SkillTuning.constructionLineLength(level);
            int half = size / 2;
            for (int offset = -half; offset <= half; offset++) {
                BlockPos pos = lookingMostlyX ? center.offset(0, 0, offset) : center.offset(offset, 0, 0);
                if (!pos.equals(center)) targets.add(pos);
            }
            return targets;
        }

        if (mode == ConstructionMode.VOLUME) {
            int volumeSize = level >= 100 ? 7 : 5;
            int half = volumeSize / 2;
            for (int dx = -half; dx <= half; dx++) {
                for (int dy = -half; dy <= half; dy++) {
                    for (int dz = -half; dz <= half; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!pos.equals(center)) targets.add(pos);
                    }
                }
            }
            return targets;
        }

        int size = fieldMastery ? 13 : SkillTuning.constructionPlaneSize(level);
        if (size <= 1) return targets;
        int half = size / 2;
        if (mode == ConstructionMode.FLOOR) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    BlockPos pos = center.offset(dx, 0, dz);
                    if (!pos.equals(center)) targets.add(pos);
                }
            }
        } else if (mode == ConstructionMode.WALL) {
            for (int horizontal = -half; horizontal <= half; horizontal++) {
                for (int dy = 0; dy < size; dy++) {
                    BlockPos pos = lookingMostlyX
                            ? center.offset(0, dy, horizontal)
                            : center.offset(horizontal, dy, 0);
                    if (!pos.equals(center)) targets.add(pos);
                }
            }
        }
        return targets;
    }

    private static void removePending(UUID uuid, int amount) {
        if (amount <= 0) return;
        int next = Math.max(0, PENDING_COUNTS.getOrDefault(uuid, 0) - amount);
        if (next == 0) PENDING_COUNTS.remove(uuid);
        else PENDING_COUNTS.put(uuid, next);
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel();
        int newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§6[건축 해금] §f선 배치 · 최대 5블록"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§6[건축 해금] §f벽/바닥 3×3 · 선 배치 9블록"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§6[건축 해금] §f벽/바닥 5×5 · 선 배치 17블록"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§6[건축 해금] §f벽/바닥 9×9 · 선 33블록. 건축 공방 완공 시 입체 5×5×5 추가."));
        if (oldLevel < 100 && newLevel >= 100) {
            String field = ExpeditionProgression.hasFieldMastery(player) ? "선 65 · 벽/바닥 13×13" : "선 49 · 벽/바닥 11×11";
            player.sendSystemMessage(Component.literal("§6[건축 숙련 VI] §f" + field + " · 건축 공방 입체 7×7×7"));
        }
    }

    private enum PlaceResult { PLACED, SKIPPED, OUT_OF_MATERIAL }

    private static final class BuildJob {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final BlockState state;
        private final Deque<BlockPos> targets;
        private int placed;

        private BuildJob(UUID playerId, ResourceKey<Level> dimension, BlockState state, List<BlockPos> targets) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.state = state;
            this.targets = new ArrayDeque<>(targets);
        }

        private boolean hasNext() { return !targets.isEmpty(); }
        private BlockPos next() { return targets.removeFirst(); }
        private int remaining() { return targets.size(); }
        private void clear() { targets.clear(); }
    }
}
