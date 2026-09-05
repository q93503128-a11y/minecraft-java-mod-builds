#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
SA = ROOT / "projects/survival-ascension"
FR = ROOT / "projects/frontier-settlement"


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(path, old, new):
    text = read(path)
    if old not in text:
        raise SystemExit(f"missing patch anchor in {path}: {old[:120]!r}")
    if text.count(old) != 1:
        raise SystemExit(f"non-unique patch anchor in {path}: count={text.count(old)}")
    write(path, text.replace(old, new, 1))


BORE = r'''package kr.moonseungjun.survivalascension.mining;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.expedition.ExpeditionProgression;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.progress.AutomatedToolBreak;
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
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-thread bulk tunnel mining.
 *
 * The break itself deliberately stays ServerPlayerGameMode-authoritative so NeoForge cancellation,
 * loot/enchantments, drops, durability semantics, stats/advancements and normal world/client updates
 * remain observable. The scheduler therefore controls latency by elapsed server-thread time rather
 * than pretending every destroyBlock call has equal cost.
 */
public final class BoreMiningService {
    /** Secondary safety caps. Time, not block count, is the primary scheduler authority. */
    private static final int GLOBAL_HARD_BLOCK_CAP_PER_TICK = 48;
    private static final int LOCAL_HARD_BLOCK_CAP_PER_TICK = 12;
    private static final long GLOBAL_SOFT_TIME_BUDGET_NANOS = 6_000_000L;
    private static final long LOCAL_SOFT_TIME_BUDGET_NANOS = 4_000_000L;
    private static final long DEFAULT_PREDICTED_PIPELINE_NANOS = 750_000L;
    private static final long MIN_PREDICTED_PIPELINE_NANOS = 200_000L;
    private static final long MAX_PREDICTED_PIPELINE_NANOS = 8_000_000L;
    private static final int MAX_PENDING_PER_PLAYER = 640;

    private static final Map<UUID, Integer> PENDING_COUNTS = new HashMap<>();
    private static final Deque<BoreJob> JOBS = new ArrayDeque<>();
    private static final Set<UUID> INTERNAL_BREAK_GUARD = new HashSet<>();
    private static final Map<UUID, ProfileSnapshot> LAST_PROFILES = new HashMap<>();

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

        long generationStart = System.nanoTime();
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

        long generationNanos = Math.max(0L, System.nanoTime() - generationStart);
        float maxHardness = originHardness <= 0.0F ? 6.0F : originHardness * 1.75F + 1.0F;
        BoreJob job = new BoreJob(uuid, level.dimension(), targets, maxHardness,
                new JobProfile(crossSection, depthLimit, targets.size(), generationNanos));
        JOBS.addLast(job);
        PENDING_COUNTS.put(uuid, targets.size());
        player.sendSystemMessage(Component.literal("§b[터널 굴착] §f" + crossSection + "×" + crossSection + "×" + depthLimit
                + " 작업을 서버 틱 시간 예산에 맞춰 분산 처리합니다."));
        return true;
    }

    public static boolean isInternal(ServerPlayer player) {
        return INTERNAL_BREAK_GUARD.contains(player.getUUID());
    }

    /** Latest completed/aborted job profile. This is runtime evidence, not a synthetic estimate. */
    public static List<String> profileLines(ServerPlayer player) {
        ProfileSnapshot snapshot = LAST_PROFILES.get(player.getUUID());
        if (snapshot == null) {
            int pending = PENDING_COUNTS.getOrDefault(player.getUUID(), 0);
            return pending > 0
                    ? List.of("현재 터널 굴착 진행 중 · 남은 대상 " + pending + " · 완료 후 다시 확인하세요.")
                    : List.of("이 세션에서 완료된 터널 굴착 프로파일이 없습니다.");
        }
        return snapshot.lines();
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (JOBS.isEmpty()) return;
        final long globalStart = System.nanoTime();
        final long globalDeadline = globalStart + GLOBAL_SOFT_TIME_BUDGET_NANOS;
        int globalHardBudget = GLOBAL_HARD_BLOCK_CAP_PER_TICK;
        int rotations = JOBS.size();

        while (globalHardBudget > 0 && rotations-- > 0 && !JOBS.isEmpty()) {
            if (System.nanoTime() >= globalDeadline) break;
            BoreJob job = JOBS.removeFirst();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(job.playerId);
            ServerLevel level = event.getServer().getLevel(job.dimension);
            if (player == null || level == null || player.isSpectator() || player.level() != level) {
                removePending(job.playerId, job.targets.size());
                finishProfile(job, "중단: 플레이어/차원 상태 변경");
                continue;
            }
            if (!InfrastructureData.get(player).isComplete(InfrastructureProject.QUARRY_NETWORK)) {
                removePending(job.playerId, job.targets.size());
                finishProfile(job, "중단: 채석장 네트워크 권한 상실");
                continue;
            }

            long sliceStart = System.nanoTime();
            long localDeadline = Math.min(globalDeadline, sliceStart + LOCAL_SOFT_TIME_BUDGET_NANOS);
            int localAttempts = 0;
            int removed = 0;

            while (localAttempts < LOCAL_HARD_BLOCK_CAP_PER_TICK
                    && globalHardBudget > 0 && !job.targets.isEmpty()) {
                long now = System.nanoTime();
                long predicted = clampPrediction(job.predictedPipelineNanos);
                // Always permit one target so the queue cannot starve. After that, do not knowingly
                // start a full vanilla destroy pipeline that is predicted to exceed this slice's budget.
                if (localAttempts > 0 && now + predicted > localDeadline) break;

                BlockPos target = job.targets.removeFirst();
                localAttempts++;
                removed++;
                globalHardBudget--;
                BreakAttempt attempt = tryBreak(player, level, target, job.maxHardness);
                job.profile.record(attempt);
                if (attempt.pipelineNanos > 0L) {
                    job.predictedPipelineNanos = ewma(job.predictedPipelineNanos, attempt.pipelineNanos);
                }
                if (System.nanoTime() >= localDeadline) break;
            }

            // One map mutation per scheduler slice instead of one per target.
            removePending(job.playerId, removed);
            job.profile.recordSlice(Math.max(0L, System.nanoTime() - sliceStart), localAttempts);
            if (job.targets.isEmpty()) finishProfile(job, "완료");
            else JOBS.addLast(job);
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
        PENDING_COUNTS.clear();
        INTERNAL_BREAK_GUARD.clear();
        LAST_PROFILES.clear();
    }

    private static BreakAttempt tryBreak(ServerPlayer player, ServerLevel level, BlockPos target, float maxHardness) {
        long validationStart = System.nanoTime();
        if (!level.hasChunkAt(target)) return BreakAttempt.skipped(System.nanoTime() - validationStart, Skip.UNLOADED_CHUNK);
        if (level.getBlockEntity(target) != null) return BreakAttempt.skipped(System.nanoTime() - validationStart, Skip.BLOCK_ENTITY);
        if (!player.getMainHandItem().is(ItemTags.PICKAXES)) return BreakAttempt.skipped(System.nanoTime() - validationStart, Skip.TOOL_MISSING);
        BlockState state = level.getBlockState(target);
        if (!MiningProgression.isValidPickaxeBreak(player, level, target, state, player.getMainHandItem())) {
            return BreakAttempt.skipped(System.nanoTime() - validationStart, Skip.INVALID_TARGET);
        }
        float hardness = state.getDestroySpeed(level, target);
        if (hardness > maxHardness) return BreakAttempt.skipped(System.nanoTime() - validationStart, Skip.HARDNESS_LIMIT);
        long validationNanos = Math.max(0L, System.nanoTime() - validationStart);

        UUID uuid = player.getUUID();
        INTERNAL_BREAK_GUARD.add(uuid);
        try {
            AutomatedToolBreak.TimedBreakResult result = AutomatedToolBreak.destroyWithReducedWearTimed(player, target);
            return new BreakAttempt(validationNanos, result.bookkeepingNanos(), result.destroyPipelineNanos(), result.broken(), Skip.NONE);
        } finally {
            INTERNAL_BREAK_GUARD.remove(uuid);
        }
    }

    private static void finishProfile(BoreJob job, String status) {
        ProfileSnapshot snapshot = job.profile.snapshot(status);
        LAST_PROFILES.put(job.playerId, snapshot);
        SurvivalAscension.LOGGER.info("[bore-profile] {}", snapshot.oneLine());
    }

    private static long clampPrediction(long value) {
        return Math.max(MIN_PREDICTED_PIPELINE_NANOS, Math.min(MAX_PREDICTED_PIPELINE_NANOS, value));
    }

    private static long ewma(long previous, long sample) {
        if (previous <= 0L) return sample;
        return Math.max(1L, (previous * 3L + sample) / 4L);
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

    private enum Skip { NONE, UNLOADED_CHUNK, BLOCK_ENTITY, TOOL_MISSING, INVALID_TARGET, HARDNESS_LIMIT }

    private record BreakAttempt(long validationNanos, long bookkeepingNanos, long pipelineNanos,
                                boolean broken, Skip skip) {
        static BreakAttempt skipped(long validationNanos, Skip skip) {
            return new BreakAttempt(Math.max(0L, validationNanos), 0L, 0L, false, skip);
        }
    }

    public record ProfileSnapshot(
            String status,
            int crossSection,
            int depth,
            int targetCount,
            int attempts,
            int destroyCalls,
            int broken,
            int unloaded,
            int blockEntities,
            int invalid,
            int toolMissing,
            int hardnessRejected,
            long generationNanos,
            long validationNanos,
            long bookkeepingNanos,
            long pipelineNanos,
            long pipelineP95Nanos,
            long pipelineP99Nanos,
            long pipelineMaxNanos,
            long sliceP95Nanos,
            long sliceP99Nanos,
            long sliceMaxNanos
    ) {
        public String oneLine() {
            return String.format(Locale.ROOT,
                    "%s %dx%dx%d targets=%d attempts=%d destroyed=%d generation=%.3fms validation=%.3fms durability=%.3fms vanilla_pipeline_total=%.3fms pipeline_p95=%.3fms p99=%.3fms max=%.3fms slice_p95=%.3fms p99=%.3fms max=%.3fms skipped[unloaded=%d,be=%d,invalid=%d,tool=%d,hard=%d]",
                    status, crossSection, crossSection, depth, targetCount, attempts, broken,
                    ms(generationNanos), ms(validationNanos), ms(bookkeepingNanos), ms(pipelineNanos),
                    ms(pipelineP95Nanos), ms(pipelineP99Nanos), ms(pipelineMaxNanos),
                    ms(sliceP95Nanos), ms(sliceP99Nanos), ms(sliceMaxNanos),
                    unloaded, blockEntities, invalid, toolMissing, hardnessRejected);
        }

        public List<String> lines() {
            return List.of(
                    "§b[터널 프로파일] §f" + status + " · " + crossSection + "×" + crossSection + "×" + depth
                            + " · 대상 " + targetCount + " / 파괴 " + broken,
                    String.format(Locale.ROOT, "§7탐색/생성 %.3fms · 검증 합계 %.3fms · 내구도 bookkeeping %.3fms",
                            ms(generationNanos), ms(validationNanos), ms(bookkeepingNanos)),
                    String.format(Locale.ROOT, "§7vanilla destroy pipeline p95 %.3fms · p99 %.3fms · max %.3fms · 호출 %d",
                            ms(pipelineP95Nanos), ms(pipelineP99Nanos), ms(pipelineMaxNanos), destroyCalls),
                    String.format(Locale.ROOT, "§7굴착 scheduler slice p95 %.3fms · p99 %.3fms · max %.3fms",
                            ms(sliceP95Nanos), ms(sliceP99Nanos), ms(sliceMaxNanos)),
                    "§8vanilla pipeline에는 NeoForge break hook, loot/drop, block/neighbor/light/fluid update, entity spawn, stat/advancement, client sync 비용이 함께 포함됩니다.",
                    "§8skip: unloaded=" + unloaded + " blockEntity=" + blockEntities + " invalid=" + invalid
                            + " tool=" + toolMissing + " hardness=" + hardnessRejected
            );
        }
    }

    private static final class JobProfile {
        private final int crossSection;
        private final int depth;
        private final int targetCount;
        private final long generationNanos;
        private final List<Long> pipelineSamples = new ArrayList<>();
        private final List<Long> sliceSamples = new ArrayList<>();
        private int attempts;
        private int destroyCalls;
        private int broken;
        private int unloaded;
        private int blockEntities;
        private int invalid;
        private int toolMissing;
        private int hardnessRejected;
        private long validationNanos;
        private long bookkeepingNanos;
        private long pipelineNanos;

        private JobProfile(int crossSection, int depth, int targetCount, long generationNanos) {
            this.crossSection = crossSection;
            this.depth = depth;
            this.targetCount = targetCount;
            this.generationNanos = generationNanos;
        }

        private void record(BreakAttempt attempt) {
            attempts++;
            validationNanos += Math.max(0L, attempt.validationNanos);
            bookkeepingNanos += Math.max(0L, attempt.bookkeepingNanos);
            pipelineNanos += Math.max(0L, attempt.pipelineNanos);
            if (attempt.pipelineNanos > 0L) {
                destroyCalls++;
                pipelineSamples.add(attempt.pipelineNanos);
            }
            if (attempt.broken) broken++;
            switch (attempt.skip) {
                case UNLOADED_CHUNK -> unloaded++;
                case BLOCK_ENTITY -> blockEntities++;
                case TOOL_MISSING -> toolMissing++;
                case INVALID_TARGET -> invalid++;
                case HARDNESS_LIMIT -> hardnessRejected++;
                default -> { }
            }
        }

        private void recordSlice(long nanos, int attempts) {
            if (attempts > 0) sliceSamples.add(Math.max(0L, nanos));
        }

        private ProfileSnapshot snapshot(String status) {
            return new ProfileSnapshot(status, crossSection, depth, targetCount, attempts, destroyCalls, broken,
                    unloaded, blockEntities, invalid, toolMissing, hardnessRejected,
                    generationNanos, validationNanos, bookkeepingNanos, pipelineNanos,
                    percentile(pipelineSamples, 0.95D), percentile(pipelineSamples, 0.99D), max(pipelineSamples),
                    percentile(sliceSamples, 0.95D), percentile(sliceSamples, 0.99D), max(sliceSamples));
        }
    }

    private static long percentile(List<Long> samples, double percentile) {
        if (samples.isEmpty()) return 0L;
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
    }

    private static long max(List<Long> samples) {
        long max = 0L;
        for (long value : samples) max = Math.max(max, value);
        return max;
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000.0D;
    }

    private static final class BoreJob {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final Deque<BlockPos> targets;
        private final float maxHardness;
        private final JobProfile profile;
        private long predictedPipelineNanos = DEFAULT_PREDICTED_PIPELINE_NANOS;

        private BoreJob(UUID playerId, ResourceKey<Level> dimension, Deque<BlockPos> targets,
                        float maxHardness, JobProfile profile) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.targets = targets;
            this.maxHardness = maxHardness;
            this.profile = profile;
        }
    }
}
'''

AUTO = r'''package kr.moonseungjun.survivalascension.progress;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Skill-expanded work keeps vanilla block-break hooks and still wears tools, but automatic
 * extra blocks only pay one normal vanilla durability roll per four successful extra blocks.
 * The player's original manual break always remains vanilla-authoritative.
 */
public final class AutomatedToolBreak {
    private static final String WEAR_BANK_KEY = "survivalascension_bulk_tool_wear_bank";
    private static final String WEAR_TOOL_KEY = "survivalascension_bulk_tool_wear_tool";
    private static final int AUTOMATIC_BLOCKS_PER_WEAR = 4;

    private AutomatedToolBreak() {}

    public record TimedBreakResult(boolean broken, long bookkeepingNanos, long destroyPipelineNanos) {}

    public static boolean destroyWithReducedWear(ServerPlayer player, BlockPos target) {
        return destroyWithReducedWearTimed(player, target).broken();
    }

    /**
     * Same mutation path as destroyWithReducedWear, with nanosecond accounting around the one
     * ServerPlayerGameMode.destroyBlock call. The destroy bucket intentionally contains all vanilla
     * and NeoForge break semantics rather than bypassing them for a synthetic fast path.
     */
    public static TimedBreakResult destroyWithReducedWearTimed(ServerPlayer player, BlockPos target) {
        long start = System.nanoTime();
        long destroyNanos = 0L;
        ItemStack tool = player.getMainHandItem();
        if (player.isCreative() || tool.isEmpty() || !tool.isDamageableItem()) {
            long destroyStart = System.nanoTime();
            boolean broken = player.gameMode.destroyBlock(target);
            destroyNanos = Math.max(0L, System.nanoTime() - destroyStart);
            return timed(broken, start, destroyNanos);
        }

        String toolId = BuiltInRegistries.ITEM.getKey(tool.getItem()).toString();
        String bankToolId = player.getPersistentData().getStringOr(WEAR_TOOL_KEY, "");
        if (!toolId.equals(bankToolId)) {
            player.getPersistentData().putString(WEAR_TOOL_KEY, toolId);
            player.getPersistentData().putInt(WEAR_BANK_KEY, 0);
        }
        int bank = Math.max(0, player.getPersistentData().getIntOr(WEAR_BANK_KEY, 0));
        if (bank >= AUTOMATIC_BLOCKS_PER_WEAR - 1) {
            long destroyStart = System.nanoTime();
            boolean broken = player.gameMode.destroyBlock(target);
            destroyNanos = Math.max(0L, System.nanoTime() - destroyStart);
            if (broken) player.getPersistentData().putInt(WEAR_BANK_KEY, 0);
            return timed(broken, start, destroyNanos);
        }

        int damageBefore = tool.getDamageValue();
        tool.setDamageValue(0);
        boolean broken;
        try {
            long destroyStart = System.nanoTime();
            broken = player.gameMode.destroyBlock(target);
            destroyNanos = Math.max(0L, System.nanoTime() - destroyStart);
        } finally {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() == tool.getItem() && held.isDamageableItem()) {
                held.setDamageValue(damageBefore);
            }
        }
        if (broken) player.getPersistentData().putInt(WEAR_BANK_KEY, bank + 1);
        return timed(broken, start, destroyNanos);
    }

    private static TimedBreakResult timed(boolean broken, long start, long destroyNanos) {
        long total = Math.max(0L, System.nanoTime() - start);
        return new TimedBreakResult(broken, Math.max(0L, total - destroyNanos), destroyNanos);
    }
}
'''

write(SA / "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java", BORE)
write(SA / "src/main/java/kr/moonseungjun/survivalascension/progress/AutomatedToolBreak.java", AUTO)

# Survival version + runtime profile command.
replace_once(SA / "gradle.properties", "mod_version=0.61.16-alpha.1", "mod_version=0.61.17-alpha.1")
replace_once(SA / "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
             'public static final String VERSION = "0.61.16-alpha.1";',
             'public static final String VERSION = "0.61.17-alpha.1";')
commands = SA / "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java"
replace_once(commands,
             "import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;" if False else "import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;",
             "import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;\nimport kr.moonseungjun.survivalascension.mining.BoreMiningService;")
replace_once(commands,
             '.then(Commands.literal("content").executes(context -> showContent(context.getSource().getPlayerOrException())))',
             '.then(Commands.literal("content").executes(context -> showContent(context.getSource().getPlayerOrException())))\n                .then(Commands.literal("borestats").executes(context -> showBoreStats(context.getSource().getPlayerOrException())))')
replace_once(commands,
             "    private static int showContent(ServerPlayer player) {",
             "    private static int showBoreStats(ServerPlayer player) {\n        for (String line : BoreMiningService.profileLines(player)) player.sendSystemMessage(Component.literal(line));\n        return 1;\n    }\n\n    private static int showContent(ServerPlayer player) {")

# Frontier: eliminate repeated full-plan construction for unrelated break events while preserving exact protection inside envelopes.
construction = FR / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java"
replace_once(construction,
             "        BlockState current = level.getBlockState(pos);\n\n        if ((pos.equals(supply) && current.is(Blocks.BARREL))",
             "        BlockState current = level.getBlockState(pos);\n        if (!pos.equals(supply) && !withinConstructionProtectionEnvelope(construction, type, rotation, pos)) return;\n\n        if ((pos.equals(supply) && current.is(Blocks.BARREL))")
replace_once(construction,
             "    public static void onBreakBlock(BreakBlockEvent event) {",
             "    private static boolean withinConstructionProtectionEnvelope(ConstructionState construction, BuildingType type,\n                                                               BuildingRotation rotation, BlockPos pos) {\n        int width = rotation.rotatedWidth(type), depth = rotation.rotatedDepth(type);\n        BlockPos origin = construction.origin();\n        if (pos.getX() < origin.getX() - SITE_WORK_MARGIN || pos.getX() > origin.getX() + width + SITE_WORK_MARGIN) return false;\n        if (pos.getZ() < origin.getZ() - SITE_WORK_MARGIN || pos.getZ() > origin.getZ() + depth + SITE_WORK_MARGIN) return false;\n        return pos.getY() >= origin.getY() - MAX_GRADE_FILL_DEPTH - 2\n                && pos.getY() <= origin.getY() + type.clearHeight() + MAX_SCAFFOLD_STEP + 2;\n    }\n\n    public static void onBreakBlock(BreakBlockEvent event) {")

road = FR / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementRoadService.java"
replace_once(road,
             "        RoadConstructionState road = data.roadConstruction();\n        if (!road.active()) return;\n\n        BlockPos pos = event.getPos();",
             "        RoadConstructionState road = data.roadConstruction();\n        if (!road.active()) return;\n\n        BlockPos pos = event.getPos();\n        if (!withinActiveRoadProtectionEnvelope(road, pos)) return;")
replace_once(road,
             "    public static void onBreakBlock(BreakBlockEvent event) {",
             "    private static boolean withinActiveRoadProtectionEnvelope(RoadConstructionState road, BlockPos pos) {\n        List<Integer> path = road.path();\n        if (path != null && path.size() >= 3) {\n            for (int i = 0; i + 2 < path.size(); i += 3) {\n                if (Math.abs(pos.getX() - path.get(i)) <= 4\n                        && Math.abs(pos.getZ() - path.get(i + 2)) <= 4\n                        && Math.abs(pos.getY() - path.get(i + 1)) <= 12) return true;\n            }\n        } else {\n            for (int i = 0; i < road.length(); i++) {\n                int x = road.startX() + road.directionX() * i, z = road.startZ() + road.directionZ() * i;\n                if (Math.abs(pos.getX() - x) <= 4 && Math.abs(pos.getZ() - z) <= 4\n                        && Math.abs(pos.getY() - road.startY()) <= 12) return true;\n            }\n        }\n        List<Integer> supports = road.bridgeSupports();\n        if (supports != null) {\n            for (int i = 0; i + 2 < supports.size(); i += 3) {\n                if (Math.abs(pos.getX() - supports.get(i)) <= 2\n                        && Math.abs(pos.getZ() - supports.get(i + 2)) <= 2\n                        && Math.abs(pos.getY() - supports.get(i + 1)) <= 16) return true;\n            }\n        }\n        return false;\n    }\n\n    public static void onBreakBlock(BreakBlockEvent event) {")

outpost = FR / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementOutpostService.java"
replace_once(outpost,
             "        BlockPos pos = event.getPos();\n        BlockState current = level.getBlockState(pos);\n        List<OutpostBlueprints.Placement> plan = OutpostBlueprints.create(state);",
             "        BlockPos pos = event.getPos();\n        if (Math.abs(pos.getX() - state.gateX()) > 16 || Math.abs(pos.getZ() - state.gateZ()) > 16\n                || Math.abs(pos.getY() - state.gateY()) > 20) return;\n        BlockState current = level.getBlockState(pos);\n        List<OutpostBlueprints.Placement> plan = OutpostBlueprints.create(state);")

core = FR / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementCoreService.java"
replace_once(core,
             "        // Protect the matching civic state from every tier, not only the current tier. A temporary",
             "        BlockPos center = data.centerPos();\n        if (Math.abs(pos.getX() - center.getX()) > 6 || Math.abs(pos.getZ() - center.getZ()) > 6\n                || pos.getY() < center.getY() - 1 || pos.getY() > center.getY() + 5) return;\n\n        // Protect the matching civic state from every tier, not only the current tier. A temporary")

waterfront = FR / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWaterfrontService.java"
replace_once(waterfront,
             "        SettlementData settlement = SettlementData.get(server);\n        if (!settlement.founded()) return;\n        BlockPos pos = event.getPos();",
             "        BlockPos pos = event.getPos();\n        BlockState current = level.getBlockState(pos);\n        if (!current.is(Blocks.SPRUCE_SLAB) && !current.is(Blocks.BARREL) && !current.is(Blocks.OAK_FENCE)) return;\n        SettlementData settlement = SettlementData.get(server);\n        if (!settlement.founded()) return;")

# Barrel/light-only protection handlers should reject the common stone tunnel case before SavedData lookup.
for rel, marker in [
    ("SettlementMarketService.java", "Blocks.BARREL"),
    ("SettlementWorkshopService.java", "Blocks.BARREL"),
    ("SettlementAdvancedWorkshopService.java", "Blocks.BARREL"),
    ("SettlementCartStationService.java", "Blocks.BARREL"),
]:
    p = FR / "src/main/java/kr/moonseungjun/frontiersettlement/settlement" / rel
    text = read(p)
    old = "        SettlementData data = SettlementData.get(server);\n"
    if old not in text:
        raise SystemExit(f"missing SavedData anchor in {rel}")
    # Only rewrite the onBreakBlock occurrence: locate method and operate in its tail.
    idx = text.index("public static void onBreakBlock")
    head, tail = text[:idx], text[idx:]
    old_tail = "        SettlementData data = SettlementData.get(server);\n"
    if old_tail not in tail:
        raise SystemExit(f"missing onBreak SavedData anchor in {rel}")
    tail = tail.replace(old_tail,
        f"        BlockPos eventPos = event.getPos();\n        if (!level.getBlockState(eventPos).is({marker})) return;\n        SettlementData data = SettlementData.get(server);\n", 1)
    # Avoid duplicate local pos declarations where present.
    tail = tail.replace("        BlockPos pos = event.getPos();\n        if (!level.getBlockState(pos).is(" + marker + ")) return;\n",
                        "        BlockPos pos = eventPos;\n", 1)
    tail = tail.replace("        if (!data.founded() || !level.getBlockState(event.getPos()).is(" + marker + ")) return;\n\n        BlockPos pos = event.getPos();",
                        "        if (!data.founded()) return;\n\n        BlockPos pos = eventPos;", 1)
    write(p, head + tail)

# Tier road lamps: same cheap block gate before SavedData/road traversal.
tierinfra = FR / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementTierInfrastructureService.java"
replace_once(tierinfra,
             "        SettlementData data=SettlementData.get(server); if(!data.founded())return; BlockPos pos=event.getPos(); Block block=level.getBlockState(pos).getBlock();\n        if(block!=Blocks.OAK_FENCE&&block!=Blocks.LANTERN)return;",
             "        BlockPos pos=event.getPos(); Block block=level.getBlockState(pos).getBlock();\n        if(block!=Blocks.OAK_FENCE&&block!=Blocks.LANTERN)return;\n        SettlementData data=SettlementData.get(server); if(!data.founded())return;")

# Versions and canonical notes.
replace_once(FR / "gradle.properties", "mod_version=0.1.0-alpha.112", "mod_version=0.1.0-alpha.113")
with (FR / "gradle.properties").open("a", encoding="utf-8") as f:
    f.write("\n# Alpha.113 bulk-break event cost: unrelated break events are rejected by cheap physical envelopes/type gates before rebuilding settlement protection plans.\n")

lock_path = FR / "COMPANION_LOCK.json"
lock = json.loads(read(lock_path))
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.112":
    raise SystemExit("unexpected Frontier lock version")
lock["target"]["frontier_settlement"] = "0.1.0-alpha.113"
lock.setdefault("notes", []).append(
    "Alpha.113 keeps all infrastructure break protection semantics but adds cheap type/geometric rejection before expensive construction/road/outpost/core plan reconstruction, preventing unrelated Survival bulk-mining BreakBlockEvents from multiplying settlement CPU/allocation cost."
)
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

# Changelog + performance runbook.
changelog = SA / "CHANGELOG.md"
text = read(changelog)
anchor = "# Changelog\n\n"
entry = """## 0.61.17-alpha.1\n- Replaced the fixed-count-only tunnel scheduler with a 6 ms global / 4 ms per-job soft server-thread time budget plus EWMA prediction before starting another full vanilla break pipeline. The existing 12-target local hard cap remains only a secondary safety ceiling.\n- Kept `ServerPlayerGameMode.destroyBlock` authoritative for every eligible tunnel block, preserving NeoForge break cancellation, Silk Touch/Fortune/loot, item/XP drops, durability policy, stats/advancements, normal neighbor/light/fluid behavior and client synchronization. No chunk force-loading was added.\n- Added per-job runtime profiling for target generation, validation, reduced-wear bookkeeping, the complete vanilla/NeoForge destroy pipeline, and scheduler-slice p95/p99/max. `/ascension borestats` reports the latest completed job.\n- Frontier Settlement Alpha.113 adds cheap physical envelope/type gates before expensive settlement break-protection plan reconstruction, removing a confirmed cross-mod amplification path triggered once per automatically mined block.\n- Network protocol remains 15. Tunnel geometry, hardness gate, drops, enchantment semantics and the one-normal-wear-per-four-successful-extra-block policy are unchanged.\n\n"""
if anchor not in text or "## 0.61.17-alpha.1" in text:
    raise SystemExit("unexpected changelog state")
write(changelog, text.replace(anchor, anchor + entry, 1))

perf = SA / "TUNNEL_PERFORMANCE.md"
write(perf, """# Tunnel bulk-mining server-tick performance\n\n## Reported integrated-server baseline\n\nMinecraft Java 26.2 / NeoForge 26.2 / Java 25, shaders off: ordinary play averaged about 8 ms with a roughly 16 ms max tick. A 7x7x10 tunnel job averaged about 62 ms and showed a roughly 790 ms max server tick while client FPS stayed near 60. These are human F3 measurements and are the acceptance baseline, not CI-generated numbers.\n\n## Current-main root cause audit\n\nThe queue itself was already a deque and target discovery was performed once at scheduling time. The problem was the cost authority: one job could still start twelve `ServerPlayerGameMode.destroyBlock` calls in the same tick regardless of how expensive the previous call was. Each call deliberately enters vanilla/NeoForge break semantics (events, loot/enchantments, drops, world updates, stats/advancements and client synchronization).\n\nThe same break event also fanned out into Frontier Settlement. Several protection listeners reconstructed/scanned complete plans for an unrelated world position: civic-core plans for every settlement tier on every break, and active building/road/outpost plans while those projects existed. That made one legitimate vanilla break pipeline pay avoidable cross-mod CPU/allocation work many times during a bulk job.\n\n## Alpha.113 / 0.61.17 controls\n\n- 6 ms global and 4 ms per-job soft bore budgets, checked using `System.nanoTime()`.\n- EWMA prediction prevents knowingly starting another full destroy pipeline when the remaining slice is smaller than the recent cost. A first target is always permitted so jobs cannot starve.\n- Existing loaded-chunk checks remain fail-closed; the bore still never force-loads or generates a target chunk.\n- The full manual-equivalent `gameMode.destroyBlock` path remains intact. There is no raw `setBlock(AIR)` fast path and no drop/enchantment/event bypass.\n- Pending-count mutation is once per scheduler slice instead of once per target.\n- Frontier break-protection handlers use exact block-type gates or conservative physical envelopes before any expensive plan reconstruction. Exact protection logic still runs for a candidate position inside those envelopes.\n\n## Runtime profiler\n\nAfter a tunnel job completes, run `/ascension borestats`. The log also contains `[bore-profile]`. Reported buckets are target generation, validation, durability bookkeeping, full vanilla destroy pipeline p95/p99/max, and scheduler-slice p95/p99/max. The vanilla pipeline bucket intentionally includes NeoForge subscriber time, loot/drop, block/neighbor/light/fluid work, item/XP entity creation, stats/advancements and network synchronization; splitting those internals further would require invasive Minecraft/NeoForge instrumentation and is not used as a production optimization shortcut.\n\n## Required real-play acceptance\n\nThe original integrated-server pack must be re-run because CI/build success cannot prove F3 MSPT. Test stone, ore/mixed, fluid and gravity adjacency, block entities, chunk boundaries, enchanted/Silk Touch/Fortune tools, full inventory, LAN, player lifecycle changes, save/rejoin, and overlapping requests. Compare F3 avg/max plus `/ascension borestats` p95/p99/max against the reported 8/16 ms idle and 62/790 ms bore baseline. The patch is not considered human-performance accepted until the repeated >50 ms and hundreds-of-ms spikes are absent in that environment.\n""")

# Source-contract regression checks.
sa_test = SA / "tools/test_current_source.py"
replace_once(sa_test, 'require("mod_version=0.61.16-alpha.1" in props, "Survival Ascension version drift")',
             'require("mod_version=0.61.17-alpha.1" in props, "Survival Ascension version drift")')
replace_once(sa_test, 'require(\'VERSION = "0.61.16-alpha.1"\' in main, "source version drift")',
             'require(\'VERSION = "0.61.17-alpha.1"\' in main, "source version drift")')
replace_once(sa_test,
             "warband = text(JAVA / \"elite/WarbandDirector.java\")",
             "bore = text(JAVA / \"mining/BoreMiningService.java\")\n"
             "automated_break = text(JAVA / \"progress/AutomatedToolBreak.java\")\n"
             "commands = text(JAVA / \"command/AscensionCommands.java\")\n"
             "require(\"GLOBAL_SOFT_TIME_BUDGET_NANOS = 6_000_000L\" in bore and \"LOCAL_SOFT_TIME_BUDGET_NANOS = 4_000_000L\" in bore, \"bore time budget missing\")\n"
             "require(\"LOCAL_HARD_BLOCK_CAP_PER_TICK = 12\" in bore and \"now + predicted > localDeadline\" in bore, \"adaptive predictive stop missing\")\n"
             "require(\"removePending(job.playerId, removed)\" in bore and \"removePending(job.playerId, 1)\" not in bore, \"pending-count batching regressed\")\n"
             "require(\"TimedBreakResult\" in automated_break and \"player.gameMode.destroyBlock(target)\" in automated_break, \"manual-equivalent vanilla destroy path/profiler missing\")\n"
             "require(\"setBlock(target\" not in bore and \"setChunkForced\" not in bore and \"addRegionTicket\" not in bore, \"bore bypasses vanilla break or force-loads\")\n"
             "require(\"pipelineP95Nanos\" in bore and \"sliceP99Nanos\" in bore and \"/ascension\" not in bore, \"bore percentile profiler missing\")\n"
             "require(\"borestats\" in commands and \"BoreMiningService.profileLines\" in commands, \"bore runtime profile command missing\")\n\n"
             "warband = text(JAVA / \"elite/WarbandDirector.java\")")
replace_once(sa_test,
             'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.16 soft TBOS shrine locator + protocol15 + prior runtime invariants")',
             'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.17 adaptive bore budget/profiling + protocol15 + prior runtime invariants")')

fr_test = FR / "tools/test_current_source.py"
replace_once(fr_test, 'require("mod_version=0.1.0-alpha.112" in gradle, "current verifier/version drift")',
             'require("mod_version=0.1.0-alpha.113" in gradle, "current verifier/version drift")')
replace_once(fr_test,
             "road = text(SETTLEMENT / \"SettlementRoadService.java\")",
             "require(\"withinConstructionProtectionEnvelope\" in construction, \"construction bulk-break coarse guard missing\")\n"
             "core_break = text(SETTLEMENT / \"SettlementCoreService.java\")\n"
             "require(\"Math.abs(pos.getX() - center.getX()) > 6\" in core_break, \"civic core still rebuilds all tier plans for remote breaks\")\n"
             "waterfront_break = text(SETTLEMENT / \"SettlementWaterfrontService.java\")\n"
             "require(\"Blocks.SPRUCE_SLAB\" in waterfront_break and \"Blocks.BARREL\" in waterfront_break and \"Blocks.OAK_FENCE\" in waterfront_break, \"waterfront type gate missing\")\n\n"
             "road = text(SETTLEMENT / \"SettlementRoadService.java\")")
replace_once(fr_test,
             "require(\"infrastructureProjectBuilder\" in road and \"ProjectLane.ROAD\" in road and \"clearRoadConstruction\" in road,",
             "require(\"withinActiveRoadProtectionEnvelope\" in road and \"road.path()\" in road, \"road break protection still reconstructs full plans before coarse rejection\")\n"
             "require(\"Math.abs(pos.getX() - state.gateX()) > 16\" in outpost, \"outpost break protection lacks coarse envelope\")\n"
             "require(\"infrastructureProjectBuilder\" in road and \"ProjectLane.ROAD\" in road and \"clearRoadConstruction\" in road,")
replace_once(fr_test,
             'print("CURRENT SOURCE CHECK PASS: alpha112 explicit settlement location UX + alpha110 scalable parallel construction crews + prior authority invariants")' if 'alpha112 explicit settlement location UX' in read(fr_test) else 'print("CURRENT SOURCE CHECK PASS: alpha112 footprint-only placement + alpha111 settlement location UX + prior authority invariants")',
             'print("CURRENT SOURCE CHECK PASS: alpha113 bulk-break event guards + alpha112 footprint-only placement + prior authority invariants")')

print("PATCH APPLIED: Survival 0.61.17 adaptive bore scheduler/profiler + Frontier alpha113 break-event guards")
