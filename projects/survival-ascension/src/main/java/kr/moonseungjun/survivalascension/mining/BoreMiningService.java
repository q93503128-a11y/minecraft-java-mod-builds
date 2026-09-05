package kr.moonseungjun.survivalascension.mining;

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
