package kr.moonseungjun.survivalascension.harvesting;

/* Mature-crop classification follows Skill Proficiencies MIT. */

import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.expedition.ExpeditionProgression;
import kr.moonseungjun.survivalascension.progress.SkillClientBridge;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HarvestingProgression {
    private static final int GLOBAL_HARVEST_BUDGET_PER_TICK = 64;
    private static final int LOCAL_HARVEST_BUDGET_PER_TICK = 12;
    private static final int MAX_PENDING_PER_PLAYER = 384;
    private static final int XP_PER_HARVEST = 15;
    private static final Set<UUID> AREA_GUARD = new HashSet<>();
    private static final Map<UUID, HarvestJob> JOBS = new HashMap<>();

    private HarvestingProgression() {}

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        if (!tool.is(ItemTags.HOES) || !isHarvestableBlock(event.getState())) return;
        int level = player instanceof ServerPlayer sp ? SkillProgressData.get(sp).level(sp, SkillType.HARVESTING) : SkillClientBridge.level(SkillType.HARVESTING);
        event.setNewSpeed((float) (event.getOriginalSpeed() * SkillTuning.harvestingSpeedMultiplier(level) * AscensionAffixes.toolSpeedMultiplier(tool)));
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos center = event.getPos();
        BlockState state = event.getState();
        if (!isMatureHarvest(state)) return;
        ItemStack tool = player.getMainHandItem();
        if (tool.is(ItemTags.HOES)) IrrigationReplantService.scheduleIfEligible(player, level, center, state);
        if (!player.isCreative() && !player.isSpectator()) {
            int xp = Math.max(1, (int) Math.ceil(XP_PER_HARVEST * AscensionAffixes.xpMultiplier(tool)));
            announceMilestones(player, SkillProgressionService.award(player, SkillType.HARVESTING, xp));
        }
        if (AREA_GUARD.contains(player.getUUID()) || player.isShiftKeyDown() || !tool.is(ItemTags.HOES)) return;
        if (JOBS.containsKey(player.getUUID())) return;

        int skillLevel = SkillProgressData.get(player).level(player, SkillType.HARVESTING);
        int baseSize = SkillTuning.harvestingAreaSize(skillLevel);
        if (skillLevel >= 100 && ExpeditionProgression.hasFieldMastery(player)) baseSize = 13;
        int size = AscensionAffixes.adjustHarvestArea(tool, baseSize);
        if (size <= 1) return;
        scheduleHarvestArea(player, level, center, size);
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (JOBS.isEmpty()) return;
        int globalBudget = GLOBAL_HARVEST_BUDGET_PER_TICK;
        var iterator = JOBS.entrySet().iterator();
        while (iterator.hasNext() && globalBudget > 0) {
            Map.Entry<UUID, HarvestJob> entry = iterator.next();
            HarvestJob job = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            ServerLevel level = event.getServer().getLevel(job.dimension);
            if (player == null || level == null || player.isSpectator() || player.level() != level || !player.getMainHandItem().is(ItemTags.HOES)) {
                iterator.remove();
                continue;
            }

            int localBudget = Math.min(LOCAL_HARVEST_BUDGET_PER_TICK, globalBudget);
            AREA_GUARD.add(player.getUUID());
            try {
                while (localBudget-- > 0 && globalBudget-- > 0 && !job.targets.isEmpty()) {
                    BlockPos target = job.targets.removeFirst();
                    if (!level.hasChunkAt(target) || level.getBlockEntity(target) != null) continue;
                    BlockState targetState = level.getBlockState(target);
                    if (!isMatureHarvest(targetState) || targetState.getDestroySpeed(level, target) < 0.0F || !targetState.canHarvestBlock(level, target, player)) continue;
                    player.gameMode.destroyBlock(target);
                    if (!player.getMainHandItem().is(ItemTags.HOES)) break;
                }
            } finally {
                AREA_GUARD.remove(player.getUUID());
            }
            if (job.targets.isEmpty() || !player.getMainHandItem().is(ItemTags.HOES)) iterator.remove();
        }
    }

    private static void scheduleHarvestArea(ServerPlayer player, ServerLevel level, BlockPos center, int size) {
        int radius = size / 2;
        Deque<BlockPos> targets = new ArrayDeque<>();
        outer:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos target = center.offset(dx, 0, dz).immutable();
                if (!level.hasChunkAt(target)) continue;
                BlockState targetState = level.getBlockState(target);
                if (!isMatureHarvest(targetState) || level.getBlockEntity(target) != null) continue;
                targets.addLast(target);
                if (targets.size() >= MAX_PENDING_PER_PLAYER) break outer;
            }
        }
        if (!targets.isEmpty()) JOBS.put(player.getUUID(), new HarvestJob(level.dimension(), targets));
    }

    private static boolean isHarvestableBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CropBlock || block instanceof NetherWartBlock || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    private static boolean isMatureHarvest(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        return state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§a[농사] §f3×3 광역 수확 해금! 웅크리면 1×1로 수확합니다."));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§a[농사] §f광역 수확이 §e5×5§f로 확장됩니다. 관개 시설 완공 시 씨앗 소비 자동 재파종도 활성화됩니다."));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§a[농사] §f광역 수확이 §e7×7§f로 확장됩니다."));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§a[농사] §f광역 수확이 §e9×9§f로 확장됩니다."));
        if (oldLevel < 100 && newLevel >= 100) {
            String cap = ExpeditionProgression.hasFieldMastery(player) ? "13×13" : "11×11";
            player.sendSystemMessage(Component.literal("§a[농사 숙련 VI] §f광역 수확 " + cap + " · 대형 수확은 서버 틱 분산"));
        }
    }

    private static final class HarvestJob {
        private final ResourceKey<Level> dimension;
        private final Deque<BlockPos> targets;

        private HarvestJob(ResourceKey<Level> dimension, Deque<BlockPos> targets) {
            this.dimension = dimension;
            this.targets = targets;
        }
    }
}
