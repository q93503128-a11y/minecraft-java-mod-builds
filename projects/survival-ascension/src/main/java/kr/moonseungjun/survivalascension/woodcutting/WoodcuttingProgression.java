package kr.moonseungjun.survivalascension.woodcutting;

/*
 * Connected-log collection, smart-tree leaf safety and tick-drained work are adapted from
 * Veinminer++ (Kestalkayden, MIT). Survival Ascension keeps its own skill/affix limits and XP rules.
 */

import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.expedition.ExpeditionProgression;
import kr.moonseungjun.survivalascension.progress.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class WoodcuttingProgression {
    private static final int GLOBAL_LOG_BUDGET_PER_TICK = 64;
    private static final int LOCAL_LOG_BUDGET_PER_TICK = 12;
    private static final int FIELD_MASTERY_LOG_LIMIT = 448;
    private static final Set<UUID> CHAIN_GUARD = new HashSet<>();
    private static final Map<UUID, FellJob> JOBS = new HashMap<>();

    private WoodcuttingProgression() {}

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos center = event.getPos();
        BlockState centerState = event.getState();
        ItemStack tool = player.getMainHandItem();
        if (!isValidLogBreak(player, level, center, centerState, tool)) return;
        if (!player.isCreative() && !player.isSpectator()) {
            int xp = Math.max(1, (int) Math.ceil(xpForLog(centerState, level, center) * AscensionAffixes.xpMultiplier(tool)));
            announceMilestones(player, SkillProgressionService.award(player, SkillType.WOODCUTTING, xp));
        }
        if (CHAIN_GUARD.contains(player.getUUID()) || player.isShiftKeyDown()) return;
        int skillLevel = SkillProgressData.get(player).level(player, SkillType.WOODCUTTING);
        int baseLimit = SkillTuning.woodcuttingLogLimit(skillLevel);
        if (skillLevel >= 100 && ExpeditionProgression.hasFieldMastery(player)) baseLimit = FIELD_MASTERY_LOG_LIMIT;
        int limit = AscensionAffixes.adjustWoodcuttingLimit(tool, baseLimit);
        if (limit <= 1 || JOBS.containsKey(player.getUUID())) return;
        scheduleNaturalTree(player, level, center, limit);
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (JOBS.isEmpty()) return;
        int globalBudget = GLOBAL_LOG_BUDGET_PER_TICK;
        var iterator = JOBS.entrySet().iterator();
        while (iterator.hasNext() && globalBudget > 0) {
            Map.Entry<UUID, FellJob> entry = iterator.next();
            FellJob job = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            ServerLevel level = event.getServer().getLevel(job.dimension);
            if (player == null || level == null || player.isSpectator() || player.level() != level) {
                iterator.remove();
                continue;
            }
            if (!player.getMainHandItem().is(ItemTags.AXES)) {
                iterator.remove();
                continue;
            }

            int localBudget = Math.min(LOCAL_LOG_BUDGET_PER_TICK, globalBudget);
            CHAIN_GUARD.add(player.getUUID());
            try {
                while (localBudget-- > 0 && globalBudget-- > 0 && !job.targets.isEmpty()) {
                    BlockPos target = job.targets.removeFirst();
                    BlockState state = level.getBlockState(target);
                    if (!state.is(BlockTags.LOGS) || level.getBlockEntity(target) != null) continue;
                    if (!isValidLogBreak(player, level, target, state, player.getMainHandItem())) continue;
                    player.gameMode.destroyBlock(target);
                    if (!player.getMainHandItem().is(ItemTags.AXES)) break;
                }
            } finally {
                CHAIN_GUARD.remove(player.getUUID());
            }
            if (job.targets.isEmpty() || !player.getMainHandItem().is(ItemTags.AXES)) iterator.remove();
        }
    }

    private static void scheduleNaturalTree(ServerPlayer player, ServerLevel level, BlockPos origin, int limit) {
        Set<BlockPos> gathered = gatherConnectedLogs(level, origin, limit);
        gathered.remove(origin.immutable());
        if (gathered.isEmpty()) return;

        // Veinminer++ smart-tree rule: connected logs are only bulk-felled when foliage is actually
        // attached to the origin or one of the collected logs. Plain log buildings stay single-block.
        if (!hasLeavesNearby(level, origin, gathered)) return;

        Deque<BlockPos> targets = new ArrayDeque<>(gathered);
        JOBS.put(player.getUUID(), new FellJob(level.dimension(), targets));
    }

    private static Set<BlockPos> gatherConnectedLogs(ServerLevel level, BlockPos origin, int limit) {
        Set<BlockPos> gathered = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = origin.immutable();
        gathered.add(start);
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty() && gathered.size() < limit) {
            BlockPos current = queue.remove();
            for (int dx = -1; dx <= 1 && gathered.size() < limit; dx++) {
                for (int dy = -1; dy <= 1 && gathered.size() < limit; dy++) {
                    for (int dz = -1; dz <= 1 && gathered.size() < limit; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = current.offset(dx, dy, dz).immutable();
                        if (!visited.add(next)) continue;
                        int rx = Math.abs(next.getX() - origin.getX());
                        int ry = Math.abs(next.getY() - origin.getY());
                        int rz = Math.abs(next.getZ() - origin.getZ());
                        if (rx > 12 || ry > 32 || rz > 12) continue;
                        if (!level.getBlockState(next).is(BlockTags.LOGS)) continue;
                        gathered.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        return gathered;
    }

    private static boolean hasLeavesNearby(ServerLevel level, BlockPos origin, Set<BlockPos> logs) {
        if (hasAdjacentLeaf(level, origin)) return true;
        for (BlockPos log : logs) if (hasAdjacentLeaf(level, log)) return true;
        return false;
    }

    private static boolean hasAdjacentLeaf(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }

    private static boolean isValidLogBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        if (state.isAir() || tool.isEmpty() || !tool.is(ItemTags.AXES) || !state.is(BlockTags.LOGS)) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        return state.canHarvestBlock(level, pos, player);
    }

    private static int xpForLog(BlockState state, ServerLevel level, BlockPos pos) {
        float hardness = Math.max(0.0F, state.getDestroySpeed(level, pos));
        return Math.max(2, Math.min(8, 2 + (int) Math.ceil(hardness)));
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§a[벌목 해금] §f자연 나무 연결 로그 최대 16개 일괄 벌목"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§a[벌목 해금] §f자연 나무 연결 로그 최대 48개 일괄 벌목"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§a[벌목 해금] §f자연 나무 연결 로그 최대 128개 일괄 벌목"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§a[벌목 해금] §f자연 나무 연결 로그 최대 256개 · 대형 작업은 서버 틱 분산"));
        if (oldLevel < 100 && newLevel >= 100) {
            String cap = ExpeditionProgression.hasFieldMastery(player) ? "448" : "384";
            player.sendSystemMessage(Component.literal("§a[벌목 숙련 VI] §f자연 나무 연결 로그 최대 " + cap + "개 · 서버 틱 분산"));
        }
    }

    private static final class FellJob {
        private final ResourceKey<Level> dimension;
        private final Deque<BlockPos> targets;
        private FellJob(ResourceKey<Level> dimension, Deque<BlockPos> targets) {
            this.dimension = dimension;
            this.targets = targets;
        }
    }
}
