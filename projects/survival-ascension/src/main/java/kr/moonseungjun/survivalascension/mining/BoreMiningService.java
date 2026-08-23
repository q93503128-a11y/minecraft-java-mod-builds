package kr.moonseungjun.survivalascension.mining;

import kr.moonseungjun.survivalascension.expedition.ExpeditionProgression;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BoreMiningService {
    private static final int GLOBAL_BLOCK_BUDGET_PER_TICK = 64;
    private static final int LOCAL_BLOCK_BUDGET_PER_TICK = 12;
    private static final int MAX_PENDING_PER_PLAYER = 640;
    private static final Map<UUID, Integer> PENDING_COUNTS = new HashMap<>();
    private static final Deque<BoreJob> JOBS = new ArrayDeque<>();
    private static final Set<UUID> INTERNAL_BREAK_GUARD = new HashSet<>();

    private BoreMiningService() {}

    public static boolean schedule(ServerPlayer player, ServerLevel level, BlockPos center, float originHardness) {
        int skillLevel = SkillProgressData.get(player).level(player, SkillType.MINING);
        if (skillLevel < 90) return false;
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.QUARRY_NETWORK)) {
            player.sendSystemMessage(Component.literal("§6[채굴] §f터널 모드는 공동 인프라 §e채석장 네트워크§f 완공이 필요합니다."));
            return false;
        }
        UUID uuid = player.getUUID();
        if (PENDING_COUNTS.getOrDefault(uuid, 0) > 0) {
            player.sendSystemMessage(Component.literal("§6[채굴] §f기존 터널 굴착 작업이 끝난 뒤 다시 사용하세요."));
            return false;
        }

        boolean fieldMastery = skillLevel >= 100 && ExpeditionProgression.hasFieldMastery(player);
        int crossSection = skillLevel >= 100 ? 7 : 5;
        int depthLimit = fieldMastery ? 12 : (skillLevel >= 100 ? 10 : 8);
        int half = crossSection / 2;
        Direction direction = horizontalDirection(player);
        Deque<BlockPos> targets = new ArrayDeque<>();
        for (int depth = 0; depth < depthLimit; depth++) {
            BlockPos base = center.relative(direction, depth);
            for (int horizontal = -half; horizontal <= half; horizontal++) {
                for (int vertical = -half; vertical <= half; vertical++) {
                    BlockPos target = direction.getAxis() == Direction.Axis.X
                            ? base.offset(0, vertical, horizontal)
                            : base.offset(horizontal, vertical, 0);
                    if (!target.equals(center)) targets.addLast(target.immutable());
                }
            }
        }
        while (targets.size() > MAX_PENDING_PER_PLAYER) targets.removeLast();
        if (targets.isEmpty()) return false;

        float maxHardness = originHardness <= 0.0F ? 6.0F : originHardness * 1.75F + 1.0F;
        JOBS.addLast(new BoreJob(uuid, level.dimension(), targets, maxHardness));
        PENDING_COUNTS.put(uuid, targets.size());
        player.sendSystemMessage(Component.literal("§b[터널 굴착] §f" + crossSection + "×" + crossSection + "×" + depthLimit + " 작업을 서버 틱에 분산 처리합니다."));
        return true;
    }

    public static boolean isInternal(ServerPlayer player) { return INTERNAL_BREAK_GUARD.contains(player.getUUID()); }

    public static void onServerTick(ServerTickEvent.Pre event) {
        int globalBudget = GLOBAL_BLOCK_BUDGET_PER_TICK;
        int rotations = JOBS.size();
        while (globalBudget > 0 && rotations-- > 0 && !JOBS.isEmpty()) {
            BoreJob job = JOBS.removeFirst();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(job.playerId);
            ServerLevel level = event.getServer().getLevel(job.dimension);
            if (player == null || level == null || player.isSpectator()) {
                removePending(job.playerId, job.targets.size());
                continue;
            }
            if (!InfrastructureData.get(player).isComplete(InfrastructureProject.QUARRY_NETWORK)) {
                removePending(job.playerId, job.targets.size());
                continue;
            }

            int localBudget = Math.min(LOCAL_BLOCK_BUDGET_PER_TICK, globalBudget);
            while (localBudget-- > 0 && globalBudget-- > 0 && !job.targets.isEmpty()) {
                BlockPos target = job.targets.removeFirst();
                removePending(job.playerId, 1);
                tryBreak(player, level, target, job.maxHardness);
            }
            if (!job.targets.isEmpty()) JOBS.addLast(job);
        }
    }

    private static void tryBreak(ServerPlayer player, ServerLevel level, BlockPos target, float maxHardness) {
        if (!level.hasChunkAt(target) || level.getBlockEntity(target) != null) return;
        if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;
        BlockState state = level.getBlockState(target);
        if (!MiningProgression.isValidPickaxeBreak(player, level, target, state, player.getMainHandItem())) return;
        float hardness = state.getDestroySpeed(level, target);
        if (hardness > maxHardness) return;

        UUID uuid = player.getUUID();
        INTERNAL_BREAK_GUARD.add(uuid);
        try {
            player.gameMode.destroyBlock(target);
        } finally {
            INTERNAL_BREAK_GUARD.remove(uuid);
        }
    }

    private static Direction horizontalDirection(ServerPlayer player) {
        double x = player.getLookAngle().x;
        double z = player.getLookAngle().z;
        if (Math.abs(x) >= Math.abs(z)) return x >= 0.0D ? Direction.EAST : Direction.WEST;
        return z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private static void removePending(UUID uuid, int amount) {
        if (amount <= 0) return;
        int next = Math.max(0, PENDING_COUNTS.getOrDefault(uuid, 0) - amount);
        if (next == 0) PENDING_COUNTS.remove(uuid);
        else PENDING_COUNTS.put(uuid, next);
    }

    private static final class BoreJob {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final Deque<BlockPos> targets;
        private final float maxHardness;

        private BoreJob(UUID playerId, ResourceKey<Level> dimension, Deque<BlockPos> targets, float maxHardness) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.targets = targets;
            this.maxHardness = maxHardness;
        }
    }
}
