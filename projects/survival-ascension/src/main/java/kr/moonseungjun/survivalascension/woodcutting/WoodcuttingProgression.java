package kr.moonseungjun.survivalascension.woodcutting;

import kr.moonseungjun.survivalascension.progress.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import java.util.*;

public final class WoodcuttingProgression {
    private static final Set<UUID> CHAIN_GUARD = new HashSet<>();
    private WoodcuttingProgression() {}

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos center = event.getPos();
        BlockState centerState = event.getState();
        if (!isValidLogBreak(player, level, center, centerState, player.getMainHandItem())) return;
        if (!player.isCreative() && !player.isSpectator()) announceMilestones(player, SkillProgressionService.award(player, SkillType.WOODCUTTING, xpForLog(centerState, level, center)));
        if (CHAIN_GUARD.contains(player.getUUID()) || player.isShiftKeyDown()) return;
        int skillLevel = SkillProgressData.get(player).level(player, SkillType.WOODCUTTING);
        int limit = SkillTuning.woodcuttingLogLimit(skillLevel);
        if (limit <= 1) return;
        CHAIN_GUARD.add(player.getUUID());
        try { fellConnectedLogs(player, level, center, limit); }
        finally { CHAIN_GUARD.remove(player.getUUID()); }
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
    private static void fellConnectedLogs(ServerPlayer player, ServerLevel level, BlockPos origin, int limit) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin); visited.add(origin);
        int broken = 1;
        while (!queue.isEmpty() && broken < limit) {
            BlockPos current = queue.remove();
            for (int dx = -1; dx <= 1 && broken < limit; dx++) for (int dy = -1; dy <= 1 && broken < limit; dy++) for (int dz = -1; dz <= 1 && broken < limit; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos next = current.offset(dx, dy, dz);
                if (!visited.add(next)) continue;
                int rx = Math.abs(next.getX() - origin.getX()), ry = Math.abs(next.getY() - origin.getY()), rz = Math.abs(next.getZ() - origin.getZ());
                if (rx > 12 || ry > 32 || rz > 12) continue;
                BlockState state = level.getBlockState(next);
                if (!state.is(BlockTags.LOGS)) continue;
                queue.add(next);
                if (!player.getMainHandItem().is(ItemTags.AXES)) return;
                if (level.getBlockEntity(next) != null || !isValidLogBreak(player, level, next, state, player.getMainHandItem())) continue;
                if (player.gameMode.destroyBlock(next)) broken++;
            }
        }
    }
    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§a[벌목 해금] §f연결 로그 최대 16개 일괄 벌목"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§a[벌목 해금] §f연결 로그 최대 48개 일괄 벌목"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§a[벌목 해금] §f연결 로그 최대 128개 일괄 벌목"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§a[벌목 해금] §f연결 로그 최대 256개 일괄 벌목"));
    }
}
