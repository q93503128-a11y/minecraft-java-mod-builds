package kr.moonseungjun.survivalascension.mining;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.progress.AutomatedToolBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Tick-drained scheduler for Mining's area, shovel, connected-vein and extract work.
 * The manual center block remains vanilla-authoritative; only the automatic extra targets live here.
 * Every extra block still travels through ServerPlayerGameMode.destroyBlock via AutomatedToolBreak,
 * so protection hooks, loot/enchantments, drops, stats and client synchronization are preserved.
 */
public final class BulkMiningService {
    private static final TagKey<Block> VALUABLE_ORES = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "valuable_ores"));
    private static final int GLOBAL_BREAK_BUDGET_PER_TICK = 48;
    private static final int LOCAL_BREAK_BUDGET_PER_TICK = 12;
    private static final long GLOBAL_SOFT_TIME_BUDGET_NANOS = 5_000_000L;
    private static final long LOCAL_SOFT_TIME_BUDGET_NANOS = 3_000_000L;
    // Max live design: shovel 21x21 => 440 extras; Lv100 Mythic pickaxe vein 192+256 => 448 total.
    private static final int MAX_PENDING_PER_PLAYER = 512;
    private static final int EXTRACT_RADIUS_XZ = 12;
    private static final int EXTRACT_RADIUS_Y = 12;

    private static final Map<UUID, BreakJob> JOBS = new HashMap<>();
    private static final Set<UUID> INTERNAL = new HashSet<>();

    private BulkMiningService() {}

    public static boolean isInternal(ServerPlayer player) {
        return INTERNAL.contains(player.getUUID());
    }

    public static void schedulePickaxeArea(ServerPlayer player, ServerLevel level, BlockPos center,
                                           int size, float centerHardness) {
        if (size <= 1 || JOBS.containsKey(player.getUUID())) return;
        Deque<BlockPos> targets = planeTargets(player, level, center, size, centerHardness, ToolKind.PICKAXE);
        enqueue(player, level, targets, ToolKind.PICKAXE, null, centerHardness);
    }

    public static void scheduleShovelArea(ServerPlayer player, ServerLevel level, BlockPos center,
                                          int size, float centerHardness) {
        if (size <= 1 || JOBS.containsKey(player.getUUID())) return;
        Deque<BlockPos> targets = planeTargets(player, level, center, size, centerHardness, ToolKind.SHOVEL);
        enqueue(player, level, targets, ToolKind.SHOVEL, null, centerHardness);
    }

    public static void scheduleConnectedOre(ServerPlayer player, ServerLevel level, BlockPos origin,
                                            BlockState originState, int limit) {
        if (limit <= 1 || JOBS.containsKey(player.getUUID())) return;
        OreVeinMatcher matcher = OreVeinMatcher.forOrigin(originState);
        int maxTargets = Math.min(MAX_PENDING_PER_PLAYER, Math.max(0, limit - 1));
        Deque<BlockPos> targets = new ArrayDeque<>();
        Queue<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos start = origin.immutable();
        frontier.add(start);
        visited.add(start);
        while (!frontier.isEmpty() && targets.size() < maxTargets) {
            BlockPos current = frontier.remove();
            for (int dx = -1; dx <= 1 && targets.size() < maxTargets; dx++) {
                for (int dy = -1; dy <= 1 && targets.size() < maxTargets; dy++) {
                    for (int dz = -1; dz <= 1 && targets.size() < maxTargets; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = current.offset(dx, dy, dz).immutable();
                        if (!visited.add(next)) continue;
                        if (Math.abs(next.getX() - origin.getX()) > 12
                                || Math.abs(next.getY() - origin.getY()) > 24
                                || Math.abs(next.getZ() - origin.getZ()) > 12) continue;
                        if (!level.hasChunkAt(next) || level.getBlockEntity(next) != null) continue;
                        BlockState state = level.getBlockState(next);
                        if (!state.is(VALUABLE_ORES) || !matcher.matches(state)) continue;
                        if (!MiningProgression.isValidPickaxeBreak(player, level, next, state, player.getMainHandItem())) continue;
                        frontier.add(next);
                        targets.addLast(next);
                    }
                }
            }
        }
        enqueue(player, level, targets, ToolKind.ORE, matcher, 0.0F);
    }

    public static void scheduleExtract(ServerPlayer player, ServerLevel level, BlockPos origin,
                                       BlockState originState, int limit) {
        if (limit <= 1 || JOBS.containsKey(player.getUUID())) return;
        OreVeinMatcher matcher = OreVeinMatcher.forOrigin(originState);
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -EXTRACT_RADIUS_XZ; dx <= EXTRACT_RADIUS_XZ; dx++) {
            for (int dy = -EXTRACT_RADIUS_Y; dy <= EXTRACT_RADIUS_Y; dy++) {
                for (int dz = -EXTRACT_RADIUS_XZ; dz <= EXTRACT_RADIUS_XZ; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos next = origin.offset(dx, dy, dz).immutable();
                    if (!level.hasChunkAt(next) || level.getBlockEntity(next) != null) continue;
                    BlockState state = level.getBlockState(next);
                    if (!state.is(VALUABLE_ORES) || !matcher.matches(state)) continue;
                    if (!MiningProgression.isValidPickaxeBreak(player, level, next, state, player.getMainHandItem())) continue;
                    candidates.add(next);
                }
            }
        }
        candidates.sort(Comparator.comparingLong(pos -> distanceSq(origin, pos)));
        int maxTargets = Math.min(MAX_PENDING_PER_PLAYER, Math.max(0, limit - 1));
        Deque<BlockPos> targets = new ArrayDeque<>();
        for (int i = 0; i < candidates.size() && i < maxTargets; i++) targets.addLast(candidates.get(i));
        enqueue(player, level, targets, ToolKind.ORE, matcher, 0.0F);
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (JOBS.isEmpty()) return;
        long globalStart = System.nanoTime();
        int globalBudget = GLOBAL_BREAK_BUDGET_PER_TICK;
        var iterator = JOBS.entrySet().iterator();
        while (iterator.hasNext() && globalBudget > 0
                && System.nanoTime() - globalStart < GLOBAL_SOFT_TIME_BUDGET_NANOS) {
            Map.Entry<UUID, BreakJob> entry = iterator.next();
            BreakJob job = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            ServerLevel level = event.getServer().getLevel(job.dimension);
            if (player == null || level == null || player.isSpectator() || player.level() != level
                    || !matchesJobTool(player, job)) {
                iterator.remove();
                continue;
            }

            long localStart = System.nanoTime();
            int localBudget = Math.min(LOCAL_BREAK_BUDGET_PER_TICK, globalBudget);
            INTERNAL.add(player.getUUID());
            try {
                while (localBudget > 0 && globalBudget > 0 && !job.targets.isEmpty()) {
                    if (System.nanoTime() - localStart >= LOCAL_SOFT_TIME_BUDGET_NANOS
                            || System.nanoTime() - globalStart >= GLOBAL_SOFT_TIME_BUDGET_NANOS) break;
                    BlockPos target = job.targets.removeFirst();
                    if (!targetStillValid(player, level, job, target)) continue;
                    localBudget--;
                    globalBudget--;
                    AutomatedToolBreak.destroyWithReducedWear(player, target);
                    if (!matchesJobTool(player, job)) break;
                }
            } finally {
                INTERNAL.remove(player.getUUID());
            }
            if (job.targets.isEmpty() || !matchesJobTool(player, job)) iterator.remove();
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
        INTERNAL.clear();
    }

    private static Deque<BlockPos> planeTargets(ServerPlayer player, ServerLevel level, BlockPos center,
                                                 int size, float centerHardness, ToolKind kind) {
        int radius = size / 2;
        Deque<BlockPos> targets = new ArrayDeque<>();
        Vec3 look = player.getLookAngle();
        double ax = Math.abs(look.x), ay = Math.abs(look.y), az = Math.abs(look.z);
        for (int a = -radius; a <= radius && targets.size() < MAX_PENDING_PER_PLAYER; a++) {
            for (int b = -radius; b <= radius && targets.size() < MAX_PENDING_PER_PLAYER; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos target = ay >= ax && ay >= az ? center.offset(a, 0, b)
                        : (ax >= az ? center.offset(0, a, b) : center.offset(a, b, 0));
                if (!level.hasChunkAt(target) || level.getBlockEntity(target) != null) continue;
                BlockState state = level.getBlockState(target);
                boolean valid = kind == ToolKind.SHOVEL
                        ? MiningProgression.isValidShovelBreak(player, level, target, state, player.getMainHandItem())
                        : MiningProgression.isValidPickaxeBreak(player, level, target, state, player.getMainHandItem());
                if (!valid) continue;
                float targetHardness = state.getDestroySpeed(level, target);
                if (centerHardness > 0.0F && targetHardness > centerHardness * 1.5F + 1.0F) continue;
                targets.addLast(target.immutable());
            }
        }
        return targets;
    }

    private static void enqueue(ServerPlayer player, ServerLevel level, Deque<BlockPos> targets,
                                ToolKind kind, OreVeinMatcher matcher, float centerHardness) {
        if (targets.isEmpty() || JOBS.containsKey(player.getUUID())) return;
        ItemStack toolProfile = AutomatedToolBreak.captureToolProfile(player.getMainHandItem());
        JOBS.put(player.getUUID(), new BreakJob(level.dimension(), targets, kind, matcher, centerHardness, toolProfile));
    }

    private static boolean targetStillValid(ServerPlayer player, ServerLevel level, BreakJob job, BlockPos target) {
        if (!level.hasChunkAt(target) || level.getBlockEntity(target) != null) return false;
        BlockState state = level.getBlockState(target);
        if (job.kind == ToolKind.ORE) {
            return state.is(VALUABLE_ORES) && job.matcher != null && job.matcher.matches(state)
                    && MiningProgression.isValidPickaxeBreak(player, level, target, state, player.getMainHandItem());
        }
        boolean valid = job.kind == ToolKind.SHOVEL
                ? MiningProgression.isValidShovelBreak(player, level, target, state, player.getMainHandItem())
                : MiningProgression.isValidPickaxeBreak(player, level, target, state, player.getMainHandItem());
        if (!valid) return false;
        float targetHardness = state.getDestroySpeed(level, target);
        return job.centerHardness <= 0.0F || targetHardness <= job.centerHardness * 1.5F + 1.0F;
    }

    private static boolean matchesJobTool(ServerPlayer player, BreakJob job) {
        ItemStack held = player.getMainHandItem();
        boolean rightClass = job.kind == ToolKind.SHOVEL ? held.is(ItemTags.SHOVELS) : held.is(ItemTags.PICKAXES);
        return rightClass && AutomatedToolBreak.matchesToolProfile(held, job.toolProfile);
    }

    private static long distanceSq(BlockPos a, BlockPos b) {
        long dx = (long) b.getX() - a.getX();
        long dy = (long) b.getY() - a.getY();
        long dz = (long) b.getZ() - a.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private enum ToolKind { PICKAXE, SHOVEL, ORE }

    private static final class BreakJob {
        private final ResourceKey<Level> dimension;
        private final Deque<BlockPos> targets;
        private final ToolKind kind;
        private final OreVeinMatcher matcher;
        private final float centerHardness;
        private final ItemStack toolProfile;

        private BreakJob(ResourceKey<Level> dimension, Deque<BlockPos> targets, ToolKind kind,
                         OreVeinMatcher matcher, float centerHardness, ItemStack toolProfile) {
            this.dimension = dimension;
            this.targets = targets;
            this.kind = kind;
            this.matcher = matcher;
            this.centerHardness = centerHardness;
            this.toolProfile = toolProfile;
        }
    }
}
