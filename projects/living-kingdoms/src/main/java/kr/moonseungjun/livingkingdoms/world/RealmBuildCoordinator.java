package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.command.generation.GenerationTask;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Pregenerates chunks, plans a capital, then applies its block edits over many safe server ticks. */
public final class RealmBuildCoordinator {
    private static final int PREGEN_RADIUS_CHUNKS = 21;
    private static final int NORMAL_TICK_BUDGET = 3_000;
    private static final int BUSY_TICK_BUDGET = 750;
    private static final Map<String, BuildJob> JOBS = new ConcurrentHashMap<>();

    private RealmBuildCoordinator() {
    }

    public static void requestPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            player.sendSystemMessage(Component.literal("§c[살아있는 왕국] 판타지 대륙을 불러오지 못했습니다."));
            return;
        }
        if (RealmSitePlanner.isBuilt(realm, profile.homelandId())) {
            LivingRealmWorldManager.finishPlacement(player, profile);
            StarterNpcManager.ensureForPlayer(player, profile);
            return;
        }

        BuildJob job = JOBS.computeIfAbsent(profile.homelandId(), ignored -> new BuildJob(realm));
        synchronized (job) {
            job.waitingPlayers.add(player.getUUID());
            if (!job.started) {
                job.started = true;
                startPregeneration(realm, profile.homelandId(), job);
            }
        }
        player.sendSystemMessage(Component.literal(
                "§6[왕국 준비] §f선택한 소속의 수도와 거주지를 실제 지형에 맞춰 건설하고 있습니다. "
                        + "서버를 멈추지 않도록 구역별로 나누어 진행하며, 완료되면 자동으로 이동합니다."
        ));
    }

    public static void prepareHomeland(ServerLevel realm, String homelandId, Consumer<Throwable> completion) {
        if (RealmSitePlanner.isBuilt(realm, homelandId)) {
            completion.accept(null);
            return;
        }
        BuildJob job = JOBS.computeIfAbsent(homelandId, ignored -> new BuildJob(realm));
        synchronized (job) {
            job.completions.add(completion);
            if (!job.started) {
                job.started = true;
                startPregeneration(realm, homelandId, job);
            }
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        int remaining = event.hasTime() ? NORMAL_TICK_BUDGET : BUSY_TICK_BUDGET;
        if (remaining <= 0 || JOBS.isEmpty()) return;

        for (Map.Entry<String, BuildJob> entry : Set.copyOf(JOBS.entrySet())) {
            if (remaining <= 0) break;
            String homelandId = entry.getKey();
            BuildJob job = entry.getValue();
            IncrementalWorldEditPlan plan;
            synchronized (job) {
                plan = job.plan;
            }
            if (plan == null || job.realm.getServer() != event.getServer()) continue;

            try {
                int used = plan.apply(job.realm, remaining);
                remaining -= used;
                reportProgress(homelandId, job, plan);
                if (plan.done()) completeBuild(homelandId, job);
            } catch (Throwable throwable) {
                failBuild(homelandId, job, throwable);
            }
        }
    }

    private static void startPregeneration(ServerLevel realm, String homelandId, BuildJob job) {
        int[] nominal = nominalCenter(homelandId);
        int centerChunkX = nominal[0] >> 4;
        int centerChunkZ = nominal[1] >> 4;
        LivingKingdoms.LOGGER.info(
                "Starting asynchronous chunk preparation for {} at chunk {},{} radius={}",
                homelandId, centerChunkX, centerChunkZ, PREGEN_RADIUS_CHUNKS
        );
        GenerationTask task = new GenerationTask(realm, centerChunkX, centerChunkZ, PREGEN_RADIUS_CHUNKS);
        job.task = task;
        task.run(new GenerationTask.Listener() {
            private int lastReported;

            @Override
            public void update(int ok, int error, int skipped, int total) {
                int done = ok + error + skipped;
                if (done - lastReported >= 200 || done == total) {
                    lastReported = done;
                    LivingKingdoms.LOGGER.info(
                            "Realm chunk preparation {} progress {}/{} errors={}", homelandId, done, total, error
                    );
                }
            }

            @Override
            public void complete(int error) {
                MinecraftServer server = realm.getServer();
                server.submit(() -> preparePlan(realm, homelandId, job, error));
            }
        });
    }

    private static void preparePlan(ServerLevel realm, String homelandId, BuildJob job, int generationErrors) {
        try {
            if (generationErrors > 0) {
                throw new IllegalStateException("Chunk preparation reported " + generationErrors + " errors");
            }
            long started = System.nanoTime();
            RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.ensureSite(realm, homelandId);
            IncrementalWorldEditPlan plan = PlannedRealmBuilder.create(realm, homelandId, site);
            synchronized (job) {
                job.plan = plan;
            }
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            LivingKingdoms.LOGGER.info(
                    "Prepared incremental homeland plan {} operations={} estimated_writes={} planning_ms={}",
                    homelandId, plan.operationCount(), plan.estimatedWrites(), elapsedMs
            );
        } catch (Throwable throwable) {
            failBuild(homelandId, job, throwable);
        }
    }

    private static void reportProgress(String homelandId, BuildJob job, IncrementalWorldEditPlan plan) {
        int percent = Math.min(100, Math.round(plan.progress() * 100.0F));
        if (percent < job.lastReportedPercent + 10 && percent != 100) return;
        job.lastReportedPercent = percent;
        LivingKingdoms.LOGGER.info(
                "Realm construction {} progress {}% ({}/{})",
                homelandId, percent, plan.appliedWrites(), plan.estimatedWrites()
        );
    }

    private static void completeBuild(String homelandId, BuildJob job) {
        try {
            RealmSitePlanner.markBuilt(job.realm, homelandId);
            for (UUID playerId : Set.copyOf(job.waitingPlayers)) {
                ServerPlayer player = job.realm.getServer().getPlayerList().getPlayer(playerId);
                if (player == null) continue;
                OriginProfileManager.profile(playerId).ifPresent(profile -> {
                    if (profile.homelandId().equals(homelandId)) {
                        LivingRealmWorldManager.finishPlacement(player, profile);
                        StarterNpcManager.ensureForPlayer(player, profile);
                    }
                });
            }
            notifyCompletions(job, null);
            JOBS.remove(homelandId, job);
        } catch (Throwable throwable) {
            failBuild(homelandId, job, throwable);
        }
    }

    private static void failBuild(String homelandId, BuildJob job, Throwable failure) {
        if (!JOBS.remove(homelandId, job)) return;
        LivingKingdoms.LOGGER.error("Failed incremental homeland construction for {}", homelandId, failure);
        if (job.task != null) job.task.stop();
        for (UUID playerId : Set.copyOf(job.waitingPlayers)) {
            ServerPlayer player = job.realm.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.sendSystemMessage(Component.literal(
                        "§c[왕국 준비 실패] §f시작 지역을 만들지 못했습니다. 서버 로그를 확인하십시오."
                ));
            }
        }
        notifyCompletions(job, failure);
    }

    private static void notifyCompletions(BuildJob job, Throwable failure) {
        for (Consumer<Throwable> completion : Set.copyOf(job.completions)) {
            try {
                completion.accept(failure);
            } catch (Throwable callbackFailure) {
                LivingKingdoms.LOGGER.error("Realm build completion callback failed", callbackFailure);
            }
        }
    }

    private static int[] nominalCenter(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> new int[]{1500, 250};
            case "kardum_league" -> new int[]{-1500, 250};
            default -> new int[]{0, 0};
        };
    }

    private static final class BuildJob {
        private final ServerLevel realm;
        private final Set<UUID> waitingPlayers = new LinkedHashSet<>();
        private final Set<Consumer<Throwable>> completions = new LinkedHashSet<>();
        private boolean started;
        private GenerationTask task;
        private IncrementalWorldEditPlan plan;
        private int lastReportedPercent = -10;

        private BuildJob(ServerLevel realm) {
            this.realm = realm;
        }
    }
}
