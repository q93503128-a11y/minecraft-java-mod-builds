#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/survivalascension"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 anchor, found {count}")
    return text.replace(old, new, 1)


bulk = r'''package kr.moonseungjun.survivalascension.mining;

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
'''
write(JAVA / "mining/BulkMiningService.java", bulk)

# Version and event registration.
props_path = ROOT / "gradle.properties"
props = read(props_path)
props = replace_once(props, "mod_version=0.61.20-alpha.1", "mod_version=0.61.21-alpha.1", "gradle version")
write(props_path, props)

main_path = JAVA / "SurvivalAscension.java"
main = read(main_path)
main = replace_once(main, "import kr.moonseungjun.survivalascension.mining.BoreMiningService;\n",
                    "import kr.moonseungjun.survivalascension.mining.BoreMiningService;\nimport kr.moonseungjun.survivalascension.mining.BulkMiningService;\n", "bulk import")
main = replace_once(main, 'public static final String VERSION = "0.61.20-alpha.1";',
                    'public static final String VERSION = "0.61.21-alpha.1";', "source version")
main = replace_once(main, "        NeoForge.EVENT_BUS.addListener(MiningProgression::onBlockBreak);\n",
                    "        NeoForge.EVENT_BUS.addListener(MiningProgression::onBlockBreak);\n        NeoForge.EVENT_BUS.addListener(BulkMiningService::onServerTick);\n", "bulk tick listener")
main = replace_once(main, "        NeoForge.EVENT_BUS.addListener(BoreMiningService::onServerStopping);\n",
                    "        NeoForge.EVENT_BUS.addListener(BoreMiningService::onServerStopping);\n        NeoForge.EVENT_BUS.addListener(BulkMiningService::onServerStopping);\n", "bulk stop listener")
write(main_path, main)

# Mining moves all high-volume automatic destroys into the tick-drained service.
mining_path = JAVA / "mining/MiningProgression.java"
mining = read(mining_path)
mining = mining.replace("if (AREA_BREAK_GUARD.contains(player.getUUID()) || player.isShiftKeyDown()) return;",
                        "if (BulkMiningService.isInternal(player) || AREA_BREAK_GUARD.contains(player.getUUID()) || player.isShiftKeyDown()) return;")
if mining.count("BulkMiningService.isInternal(player)") != 2:
    raise SystemExit("mining bulk recursion guard anchors drifted")
mining = mining.replace("breakConnectedOre(player, level, center, centerState, veinLimit)",
                        "BulkMiningService.scheduleConnectedOre(player, level, center, centerState, veinLimit)")
mining = mining.replace("breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)))",
                        "BulkMiningService.schedulePickaxeArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)))")
mining = mining.replace("extractMatchingOre(player, level, center, centerState, veinLimit)",
                        "BulkMiningService.scheduleExtract(player, level, center, centerState, veinLimit)")
mining = mining.replace("breakShovelArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)))",
                        "BulkMiningService.scheduleShovelArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)))")
for needle in ("scheduleConnectedOre", "schedulePickaxeArea", "scheduleExtract", "scheduleShovelArea"):
    if f"BulkMiningService.{needle}" not in mining:
        raise SystemExit(f"mining scheduler replacement missing: {needle}")
start = mining.find("    private static void breakConnectedOre(")
end = mining.find("    public static int areaSize(int level)")
if start < 0 or end < 0 or end <= start:
    raise SystemExit("retired synchronous mining method block anchors missing")
mining = mining[:start] + mining[end:]
for old_import in (
    "import net.minecraft.world.phys.Vec3;\n",
    "import java.util.ArrayDeque;\n",
    "import java.util.ArrayList;\n",
    "import java.util.Comparator;\n",
    "import java.util.List;\n",
    "import java.util.Queue;\n",
):
    mining = mining.replace(old_import, "")
mining = mining.replace("    private static final int EXTRACT_RADIUS_XZ = 12;\n    private static final int EXTRACT_RADIUS_Y = 12;\n", "")
write(mining_path, mining)

# Capture a damage-normalized tool component profile so queued work cannot be started with a
# high-tier affixed tool and then paid with a different disposable tool.
auto_path = JAVA / "progress/AutomatedToolBreak.java"
auto = read(auto_path)
anchor = "    private static TimedBreakResult timed(boolean broken, long start, long destroyNanos) {\n"
profile_methods = '''    public static ItemStack captureToolProfile(ItemStack stack) {\n        if (stack.isEmpty()) return ItemStack.EMPTY;\n        ItemStack copy = stack.copyWithCount(1);\n        if (copy.isDamageableItem()) copy.setDamageValue(0);\n        return copy;\n    }\n\n    public static boolean matchesToolProfile(ItemStack current, ItemStack profile) {\n        if (current.isEmpty() || profile.isEmpty()) return false;\n        return ItemStack.isSameItemSameComponents(captureToolProfile(current), profile);\n    }\n\n'''
auto = replace_once(auto, anchor, profile_methods + anchor, "tool profile helpers")
write(auto_path, auto)

# Existing woodcutting and harvesting queues get the same anti-swap invariant.
wood_path = JAVA / "woodcutting/WoodcuttingProgression.java"
wood = read(wood_path)
wood = replace_once(wood,
    "            if (!player.getMainHandItem().is(ItemTags.AXES)) {\n                iterator.remove();\n                continue;\n            }\n",
    "            if (!matchesJobTool(player, job)) {\n                iterator.remove();\n                continue;\n            }\n", "wood initial tool guard")
wood = replace_once(wood,
    "                    AutomatedToolBreak.destroyWithReducedWear(player, target);\n                    if (!player.getMainHandItem().is(ItemTags.AXES)) break;\n",
    "                    AutomatedToolBreak.destroyWithReducedWear(player, target);\n                    if (!matchesJobTool(player, job)) break;\n", "wood loop tool guard")
wood = replace_once(wood,
    "            if (job.targets.isEmpty() || !player.getMainHandItem().is(ItemTags.AXES)) iterator.remove();\n",
    "            if (job.targets.isEmpty() || !matchesJobTool(player, job)) iterator.remove();\n", "wood final tool guard")
wood = replace_once(wood,
    "        JOBS.put(player.getUUID(), new FellJob(level.dimension(), targets));\n",
    "        JOBS.put(player.getUUID(), new FellJob(level.dimension(), targets,\n                AutomatedToolBreak.captureToolProfile(player.getMainHandItem())));\n", "wood snapshot")
wood_class_anchor = "    private static final class FellJob {\n"
wood_helper = '''    private static boolean matchesJobTool(ServerPlayer player, FellJob job) {\n        ItemStack held = player.getMainHandItem();\n        return held.is(ItemTags.AXES) && AutomatedToolBreak.matchesToolProfile(held, job.toolProfile);\n    }\n\n'''
wood = replace_once(wood, wood_class_anchor, wood_helper + wood_class_anchor, "wood profile helper")
wood = replace_once(wood,
    "        private final Deque<BlockPos> targets;\n        private FellJob(ResourceKey<Level> dimension, Deque<BlockPos> targets) {\n            this.dimension = dimension;\n            this.targets = targets;\n        }\n",
    "        private final Deque<BlockPos> targets;\n        private final ItemStack toolProfile;\n        private FellJob(ResourceKey<Level> dimension, Deque<BlockPos> targets, ItemStack toolProfile) {\n            this.dimension = dimension;\n            this.targets = targets;\n            this.toolProfile = toolProfile;\n        }\n", "wood job profile")
write(wood_path, wood)

harvest_path = JAVA / "harvesting/HarvestingProgression.java"
harvest = read(harvest_path)
harvest = replace_once(harvest,
    "            if (player == null || level == null || player.isSpectator() || player.level() != level || !player.getMainHandItem().is(ItemTags.HOES)) {\n",
    "            if (player == null || level == null || player.isSpectator() || player.level() != level || !matchesJobTool(player, job)) {\n", "harvest initial tool guard")
harvest = replace_once(harvest,
    "                    AutomatedToolBreak.destroyWithReducedWear(player, target);\n                    if (!player.getMainHandItem().is(ItemTags.HOES)) break;\n",
    "                    AutomatedToolBreak.destroyWithReducedWear(player, target);\n                    if (!matchesJobTool(player, job)) break;\n", "harvest loop tool guard")
harvest = replace_once(harvest,
    "            if (job.targets.isEmpty() || !player.getMainHandItem().is(ItemTags.HOES)) iterator.remove();\n",
    "            if (job.targets.isEmpty() || !matchesJobTool(player, job)) iterator.remove();\n", "harvest final tool guard")
harvest = replace_once(harvest,
    "            JOBS.put(player.getUUID(), new HarvestJob(level.dimension(), targets));\n",
    "            JOBS.put(player.getUUID(), new HarvestJob(level.dimension(), targets,\n                    AutomatedToolBreak.captureToolProfile(player.getMainHandItem())));\n", "harvest snapshot")
harvest_class_anchor = "    private static final class HarvestJob {\n"
harvest_helper = '''    private static boolean matchesJobTool(ServerPlayer player, HarvestJob job) {\n        ItemStack held = player.getMainHandItem();\n        return held.is(ItemTags.HOES) && AutomatedToolBreak.matchesToolProfile(held, job.toolProfile);\n    }\n\n'''
harvest = replace_once(harvest, harvest_class_anchor, harvest_helper + harvest_class_anchor, "harvest profile helper")
harvest = replace_once(harvest,
    "        private final Deque<BlockPos> targets;\n        private HarvestJob(ResourceKey<Level> dimension, Deque<BlockPos> targets) {\n            this.dimension = dimension;\n            this.targets = targets;\n        }\n",
    "        private final Deque<BlockPos> targets;\n        private final ItemStack toolProfile;\n        private HarvestJob(ResourceKey<Level> dimension, Deque<BlockPos> targets, ItemStack toolProfile) {\n            this.dimension = dimension;\n            this.targets = targets;\n            this.toolProfile = toolProfile;\n        }\n", "harvest job profile")
write(harvest_path, harvest)

# Current verifier and historical wrapper version authority.
check_path = ROOT / "tools/test_current_source.py"
check = read(check_path)
check = replace_once(check, 'mod_version=0.61.20-alpha.1', 'mod_version=0.61.21-alpha.1', "verifier gradle version")
check = replace_once(check, 'VERSION = "0.61.20-alpha.1"', 'VERSION = "0.61.21-alpha.1"', "verifier source version")
check = replace_once(check,
    '    "MiningProgression::onBlockBreak",\n',
    '    "MiningProgression::onBlockBreak",\n    "BulkMiningService::onServerTick",\n', "verifier bulk listener")
check = replace_once(check,
    'require("level.getBlockEntity(target) != null" in mining, "bulk mining no longer protects block entities")\n',
    'bulk_mining = text(JAVA / "mining/BulkMiningService.java")\n'
    'require("level.getBlockEntity(target) != null" in bulk_mining, "bulk mining no longer protects block entities")\n'
    'require("GLOBAL_BREAK_BUDGET_PER_TICK = 48" in bulk_mining and "LOCAL_BREAK_BUDGET_PER_TICK = 12" in bulk_mining,\n'
    '        "bulk mining lost bounded per-tick drain budgets")\n'
    'require("GLOBAL_SOFT_TIME_BUDGET_NANOS = 5_000_000L" in bulk_mining and "LOCAL_SOFT_TIME_BUDGET_NANOS = 3_000_000L" in bulk_mining,\n'
    '        "bulk mining lost server-thread soft time budgets")\n'
    'require("MAX_PENDING_PER_PLAYER = 512" in bulk_mining and "JOBS.clear()" in bulk_mining and "INTERNAL.clear()" in bulk_mining,\n'
    '        "bulk mining queue bound/cleanup missing")\n'
    'require("BulkMiningService::onServerStopping" in main and "BulkMiningService.isInternal(player)" in mining,\n'
    '        "bulk mining lifecycle/recursion guard missing")\n'
    'for schedule_call in ("schedulePickaxeArea", "scheduleShovelArea", "scheduleConnectedOre", "scheduleExtract"):\n'
    '    require(f"BulkMiningService.{schedule_call}" in mining, f"mining still bypasses tick-drained scheduler: {schedule_call}")\n'
    'require("AutomatedToolBreak.destroyWithReducedWear(player, target)" in bulk_mining\n'
    '        and "AutomatedToolBreak.destroyWithReducedWear(player, target)" not in mining,\n'
    '        "bulk mining destroy pipeline is not centralized in the bounded scheduler")\n'
    'for forbidden in ("setChunkForced", "addRegionTicket", "getChunk("):\n'
    '    require(forbidden not in bulk_mining, f"bulk mining may force-load/generate chunks: {forbidden}")\n', "verifier bulk block entity anchor")
check = replace_once(check,
    'require("CHAIN_GUARD" in woodcutting and "JOBS.clear()" in woodcutting, "woodcutting queue/recursion cleanup missing")\n',
    'require("CHAIN_GUARD" in woodcutting and "JOBS.clear()" in woodcutting, "woodcutting queue/recursion cleanup missing")\n'
    'require("captureToolProfile" in automated_break and "matchesToolProfile" in automated_break,\n'
    '        "queued work tool-profile authority missing")\n'
    'require("toolProfile" in woodcutting and "matchesJobTool(player, job)" in woodcutting,\n'
    '        "woodcutting queue can be started with a strong affixed tool then paid with another tool")\n', "verifier wood profile")
check = replace_once(check,
    'require("AREA_GUARD" in harvesting and "MAX_PENDING_PER_PLAYER" in harvesting and "JOBS.clear()" in harvesting,\n        "harvesting queue bounds/cleanup missing")\n',
    'require("AREA_GUARD" in harvesting and "MAX_PENDING_PER_PLAYER" in harvesting and "JOBS.clear()" in harvesting,\n        "harvesting queue bounds/cleanup missing")\n'
    'require("toolProfile" in harvesting and "matchesJobTool(player, job)" in harvesting,\n'
    '        "harvesting queue can be started with a strong affixed tool then paid with another tool")\n', "verifier harvest profile")
check = replace_once(check,
    'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.20 equipment economy + distinct rerolls + harvest queue + full skill/runtime invariants")',
    'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.21 tick-drained mining + queued-tool authority + equipment economy/full runtime invariants")',
    "verifier summary")
write(check_path, check)

release_path = ROOT / "tools/test_release_source.py"
release = read(release_path)
release = replace_once(release, 'CURRENT_VERSION = "0.61.0-alpha.1"', 'CURRENT_VERSION = "0.61.21-alpha.1"', "historical wrapper current version")
write(release_path, release)

changelog_path = ROOT / "CHANGELOG.md"
changelog = read(changelog_path)
section = '''# Changelog\n\n## 0.61.21-alpha.1\n- Mining 2D pickaxe areas, shovel earthworks, connected veins and Extract no longer execute hundreds of full vanilla/NeoForge block-break pipelines inside the initiating break event. Automatic extras are queued at 12 per player / 48 global per tick with additional 3 ms local / 5 ms global soft server-thread budgets.\n- The manual center block remains vanilla-authoritative and every queued extra still uses ServerPlayerGameMode.destroyBlock through AutomatedToolBreak, preserving protection cancellation, Fortune/Silk Touch/loot, drops, stats, durability policy and client synchronization. No chunk force-loading is added.\n- Maximum pending Mining work is 512 targets, above the current 440-extra shovel plane and 448-total Mythic pickaxe vein ceilings, so the performance guard does not silently truncate current equipment power.\n- Mining, Woodcutting and Harvesting queued jobs now capture a damage-normalized ItemStack component profile. Swapping from the high-tier affixed tool that created a large job to a different disposable tool cancels that queued work; normal durability changes on the original tool do not.\n- No SavedData schema, packet or network protocol changes. Network protocol remains 15.\n\n'''
if not changelog.startswith("# Changelog\n\n## 0.61.20-alpha.1"):
    raise SystemExit("changelog head drifted")
changelog = section + changelog[len("# Changelog\n\n"):]
write(changelog_path, changelog)

print("Applied Survival Ascension 0.61.21 tick-drained mining and queued-tool authority")
