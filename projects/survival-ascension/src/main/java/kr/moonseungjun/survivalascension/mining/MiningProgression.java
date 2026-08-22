package kr.moonseungjun.survivalascension.mining;

import kr.moonseungjun.survivalascension.SurvivalAscension;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class MiningProgression {
    private static final TagKey<Block> VALUABLE_ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "valuable_ores"));
    private static final Set<UUID> AREA_BREAK_GUARD = new HashSet<>();
    private MiningProgression() {}

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SkillProgressData data = SkillProgressData.get(player);
        data.ensureProfile(player);
        SkillProgressionService.syncAll(player);
        if (data.markIntroduced(player)) {
            player.sendSystemMessage(Component.literal("§6[Survival Ascension] §f행동으로 숙련을 올리면 작업 규모 자체가 커집니다."));
            player.sendSystemMessage(Component.literal("§b채굴 §fLv.10 3×3 · Lv.30 5×5+광맥24 · Lv.60 7×7+광맥64 · Lv.90 9×9+광맥128"));
            player.sendSystemMessage(Component.literal("§a벌목 §fLv.10부터 연쇄 벌목  §7| §2농사 §fLv.10부터 광역 수확"));
            player.sendSystemMessage(Component.literal("§7웅크리면 광역/연쇄 기능을 끄고 정밀 작업합니다."));
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
            event.setNewSpeed((float) (event.getOriginalSpeed() * SkillTuning.miningSpeedMultiplier(level)));
            return;
        }
        if (tool.is(ItemTags.AXES) && state.is(BlockTags.LOGS)) {
            int level = player instanceof ServerPlayer sp ? SkillProgressData.get(sp).level(sp, SkillType.WOODCUTTING) : SkillClientBridge.level(SkillType.WOODCUTTING);
            event.setNewSpeed((float) (event.getOriginalSpeed() * SkillTuning.woodcuttingSpeedMultiplier(level)));
        }
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockState centerState = event.getState();
        BlockPos center = event.getPos();
        if (!isValidPickaxeBreak(player, level, center, centerState, player.getMainHandItem())) return;
        if (!player.isCreative() && !player.isSpectator()) {
            announceMilestones(player, SkillProgressionService.award(player, SkillType.MINING, xpForBlock(centerState, level, center)));
        }
        if (AREA_BREAK_GUARD.contains(player.getUUID()) || player.isShiftKeyDown()) return;

        int miningLevel = SkillProgressData.get(player).level(player, SkillType.MINING);
        if (centerState.is(VALUABLE_ORES)) {
            int veinLimit = SkillTuning.miningVeinLimit(miningLevel);
            if (veinLimit > 1) {
                AREA_BREAK_GUARD.add(player.getUUID());
                try { breakConnectedOre(player, level, center, centerState, veinLimit); }
                finally { AREA_BREAK_GUARD.remove(player.getUUID()); }
                return;
            }
        }

        int size = SkillTuning.miningAreaSize(miningLevel);
        if (size <= 1) return;
        float centerHardness = Math.max(0.0F, centerState.getDestroySpeed(level, center));
        AREA_BREAK_GUARD.add(player.getUUID());
        try { breakArea(player, level, center, size, centerHardness); }
        finally { AREA_BREAK_GUARD.remove(player.getUUID()); }
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
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§b[채굴 해금] §f3×3 광역 채굴"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§b[채굴 해금] §f5×5 광역 채굴 + 같은 광맥 최대 24개 추적"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§b[채굴 해금] §f7×7 광역 채굴 + 같은 광맥 최대 64개 추적"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§b[채굴 해금] §f9×9 광역 채굴 + 같은 광맥 최대 128개 추적"));
    }

    /*
     * Bounded 26-connected flood-fill adapted from Veinminer++ (MIT, Kestalkayden 2026).
     * Survival Ascension keeps its own skill gates, ore eligibility, normal destroy path and XP rules.
     */
    private static void breakConnectedOre(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState originState, int limit) {
        OreVeinMatcher matcher = OreVeinMatcher.forOrigin(originState);
        Queue<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(origin.immutable());
        visited.add(origin.immutable());
        int broken = 1;

        while (!frontier.isEmpty() && broken < limit) {
            BlockPos current = frontier.remove();
            for (int dx = -1; dx <= 1 && broken < limit; dx++) {
                for (int dy = -1; dy <= 1 && broken < limit; dy++) {
                    for (int dz = -1; dz <= 1 && broken < limit; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = current.offset(dx, dy, dz).immutable();
                        if (!visited.add(next)) continue;
                        if (Math.abs(next.getX() - origin.getX()) > 12
                                || Math.abs(next.getY() - origin.getY()) > 24
                                || Math.abs(next.getZ() - origin.getZ()) > 12) continue;
                        BlockState state = level.getBlockState(next);
                        if (!state.is(VALUABLE_ORES) || !matcher.matches(state)) continue;
                        if (level.getBlockEntity(next) != null) continue;
                        if (!isValidPickaxeBreak(player, level, next, state, player.getMainHandItem())) continue;
                        frontier.add(next);
                        if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return;
                        if (player.gameMode.destroyBlock(next)) broken++;
                    }
                }
            }
        }
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
