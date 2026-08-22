package kr.moonseungjun.survivalascension.mining;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.progress.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class MiningProgression {
    private static final TagKey<Block> VALUABLE_ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "valuable_ores"));
    private static final Set<UUID> AREA_BREAK_GUARD = new HashSet<>();
    private static final String MODE_KEY = "survivalascension_mining_mode";
    private static final int EXTRACT_RADIUS_XZ = 12;
    private static final int EXTRACT_RADIUS_Y = 12;

    private MiningProgression() {}

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SkillProgressData data = SkillProgressData.get(player);
        data.ensureProfile(player);
        SkillProgressionService.syncAll(player);
        if (data.markIntroduced(player)) {
            player.sendSystemMessage(Component.literal("§6[Survival Ascension] §f행동으로 숙련을 올리면 작업 규모 자체가 커집니다."));
            player.sendSystemMessage(Component.literal("§b채굴 §fLv.10 3×3 · Lv.30 광맥 · Lv.60 7×7 · Lv.90 9×9+추출 모드"));
            player.sendSystemMessage(Component.literal("§a벌목 §fLv.10부터 연쇄 벌목  §7| §2농사 §fLv.10부터 광역 수확"));
            player.sendSystemMessage(Component.literal("§7M 메뉴에서 채굴 모드를 고를 수 있고, 웅크리면 항상 1×1 정밀 작업합니다."));
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SkillProgressionService.syncAll(player);
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        BlockState state = event.getState();
        if (tool.is(ItemTags.PICKAXES) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            int level = player instanceof ServerPlayer sp ? SkillProgressData.get(sp).level(sp, SkillType.MINING) : SkillClientBridge.level(SkillType.MINING);
            event.setNewSpeed((float) (event.getOriginalSpeed() * SkillTuning.miningSpeedMultiplier(level) * AscensionAffixes.toolSpeedMultiplier(tool)));
            return;
        }
        if (tool.is(ItemTags.AXES) && state.is(BlockTags.LOGS)) {
            int level = player instanceof ServerPlayer sp ? SkillProgressData.get(sp).level(sp, SkillType.WOODCUTTING) : SkillClientBridge.level(SkillType.WOODCUTTING);
            event.setNewSpeed((float) (event.getOriginalSpeed() * SkillTuning.woodcuttingSpeedMultiplier(level) * AscensionAffixes.toolSpeedMultiplier(tool)));
        }
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockState centerState = event.getState();
        BlockPos center = event.getPos();
        ItemStack tool = player.getMainHandItem();
        if (!isValidPickaxeBreak(player, level, center, centerState, tool)) return;
        if (!player.isCreative() && !player.isSpectator()) {
            int xp = Math.max(1, (int) Math.ceil(xpForBlock(centerState, level, center) * AscensionAffixes.xpMultiplier(tool)));
            announceMilestones(player, SkillProgressionService.award(player, SkillType.MINING, xp));
        }
        if (AREA_BREAK_GUARD.contains(player.getUUID()) || player.isShiftKeyDown()) return;

        int miningLevel = SkillProgressData.get(player).level(player, SkillType.MINING);
        MiningMode mode = effectiveMode(player, miningLevel);
        int areaSize = AscensionAffixes.adjustMiningArea(tool, SkillTuning.miningAreaSize(miningLevel));
        int veinLimit = AscensionAffixes.adjustMiningVeinLimit(tool, SkillTuning.miningVeinLimit(miningLevel));

        AREA_BREAK_GUARD.add(player.getUUID());
        try {
            switch (mode) {
                case AUTO -> {
                    if (centerState.is(VALUABLE_ORES) && veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);
                    else if (areaSize > 1) breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)));
                }
                case PLANE -> {
                    if (areaSize > 1) breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)));
                }
                case VEIN -> {
                    if (centerState.is(VALUABLE_ORES) && veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);
                }
                case EXTRACT -> {
                    if (centerState.is(VALUABLE_ORES) && veinLimit > 1) extractMatchingOre(player, level, center, centerState, veinLimit);
                }
            }
        } finally {
            AREA_BREAK_GUARD.remove(player.getUUID());
        }
    }

    public static void setMode(ServerPlayer player, MiningMode mode) {
        int level = SkillProgressData.get(player).level(player, SkillType.MINING);
        if (level < mode.requiredLevel()) {
            player.sendSystemMessage(Component.literal("§c[채굴] §f" + mode.koreanName() + " 모드는 채굴 Lv." + mode.requiredLevel() + " 필요"));
            return;
        }
        player.getPersistentData().putString(MODE_KEY, mode.id());
        player.sendSystemMessage(Component.literal("§b[채굴 모드] §f" + mode.koreanName() + "§f 선택"));
    }

    public static MiningMode getMode(ServerPlayer player) {
        return MiningMode.fromId(player.getPersistentData().getStringOr(MODE_KEY, MiningMode.AUTO.id()));
    }

    private static MiningMode effectiveMode(ServerPlayer player, int level) {
        MiningMode selected = getMode(player);
        return level >= selected.requiredLevel() ? selected : MiningMode.AUTO;
    }

    private static boolean isValidPickaxeBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        if (state.isAir() || tool.isEmpty() || !tool.is(ItemTags.PICKAXES) || !state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        return state.canHarvestBlock(level, pos, player);
    }

    private static int xpForBlock(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.is(VALUABLE_ORES)) return 20;
        float hardness = Math.max(0.0F, state.getDestroySpeed(level, pos));
        return Math.max(1, Math.min(6, (int) Math.ceil(hardness)));
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§b[채굴 해금] §f3×3 굴착 + M→채굴→굴착 모드"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§b[채굴 해금] §f5×5 + 연결 광맥 24 + 광맥 전용 모드"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§b[채굴 해금] §f7×7 + 연결 광맥 64"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§b[채굴 해금] §f9×9 + 광맥 128 + §e추출 모드§f: 반경 내 같은 광석을 비연결 상태에서도 탐색"));
    }

    private static void breakConnectedOre(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState originState, int limit) {
        OreVeinMatcher matcher = OreVeinMatcher.forOrigin(originState);
        Queue<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(origin.immutable());
        visited.add(origin.immutable());
        int broken = 1;
        while (!frontier.isEmpty() && broken < limit) {
            BlockPos current = frontier.remove();
            for (int dx = -1; dx <= 1 && broken < limit; dx++) for (int dy = -1; dy <= 1 && broken < limit; dy++) for (int dz = -1; dz <= 1 && broken < limit; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos next = current.offset(dx, dy, dz).immutable();
                if (!visited.add(next)) continue;
                if (Math.abs(next.getX() - origin.getX()) > 12 || Math.abs(next.getY() - origin.getY()) > 24 || Math.abs(next.getZ() - origin.getZ()) > 12) continue;
                BlockState state = level.getBlockState(next);
                if (!state.is(VALUABLE_ORES) || !matcher.matches(state)) continue;
                if (level.getBlockEntity(next) != null || !isValidPickaxeBreak(player, level, next, state, player.getMainHandItem())) continue;
                frontier.add(next);
                if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;
                if (player.gameMode.destroyBlock(next)) broken++;
            }
        }
    }

    /*
     * Target-filtered bounded search is adapted from the Digital Miner design in Mekanism (MIT).
     * No Mekanism assets, machine implementation, energy system, filters, or GUI are bundled.
     * Survival Ascension searches only already-loaded nearby blocks, requires Lv.90, keeps normal destroyBlock handling,
     * tool harvest checks, block-entity exclusion, skill XP, durability, enchantment/loot behavior, and a hard block limit.
     */
    private static void extractMatchingOre(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState originState, int limit) {
        OreVeinMatcher matcher = OreVeinMatcher.forOrigin(originState);
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -EXTRACT_RADIUS_XZ; dx <= EXTRACT_RADIUS_XZ; dx++) {
            for (int dy = -EXTRACT_RADIUS_Y; dy <= EXTRACT_RADIUS_Y; dy++) {
                for (int dz = -EXTRACT_RADIUS_XZ; dz <= EXTRACT_RADIUS_XZ; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos next = origin.offset(dx, dy, dz).immutable();
                    if (!level.hasChunkAt(next)) continue;
                    BlockState state = level.getBlockState(next);
                    if (!state.is(VALUABLE_ORES) || !matcher.matches(state)) continue;
                    if (level.getBlockEntity(next) != null || !isValidPickaxeBreak(player, level, next, state, player.getMainHandItem())) continue;
                    candidates.add(next);
                }
            }
        }
        candidates.sort(Comparator.comparingLong(pos -> distanceSq(origin, pos)));
        int broken = 1;
        for (BlockPos target : candidates) {
            if (broken >= limit || !player.getMainHandItem().is(ItemTags.PICKAXES)) break;
            BlockState state = level.getBlockState(target);
            if (!state.is(VALUABLE_ORES) || !matcher.matches(state)) continue;
            if (player.gameMode.destroyBlock(target)) broken++;
        }
    }

    private static long distanceSq(BlockPos a, BlockPos b) {
        long dx = (long) b.getX() - a.getX();
        long dy = (long) b.getY() - a.getY();
        long dz = (long) b.getZ() - a.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static void breakArea(ServerPlayer player, ServerLevel level, BlockPos center, int size, float centerHardness) {
        int radius = size / 2;
        Vec3 look = player.getLookAngle();
        double ax = Math.abs(look.x), ay = Math.abs(look.y), az = Math.abs(look.z);
        for (int a = -radius; a <= radius; a++) for (int b = -radius; b <= radius; b++) {
            if (a == 0 && b == 0) continue;
            if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;
            BlockPos target = ay >= ax && ay >= az ? center.offset(a, 0, b) : (ax >= az ? center.offset(0, a, b) : center.offset(a, b, 0));
            BlockState targetState = level.getBlockState(target);
            if (!isValidPickaxeBreak(player, level, target, targetState, player.getMainHandItem())) continue;
            if (level.getBlockEntity(target) != null) continue;
            float targetHardness = targetState.getDestroySpeed(level, target);
            if (centerHardness > 0.0F && targetHardness > centerHardness * 1.5F + 1.0F) continue;
            player.gameMode.destroyBlock(target);
        }
    }

    public static int areaSize(int level) { return SkillTuning.miningAreaSize(level); }
    public static int veinLimit(int level) { return SkillTuning.miningVeinLimit(level); }
    public static double speedMultiplier(int level) { return SkillTuning.miningSpeedMultiplier(level); }
    public static String formatMultiplier(double value) { return String.format(Locale.ROOT, "%.2f×", value); }
}
