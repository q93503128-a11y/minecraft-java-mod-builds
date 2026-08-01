package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.command.generation.GenerationTask;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Surveys generator columns off-thread, pregenerates the selected site, then builds over safe server ticks. */
public final class RealmBuildCoordinator {
    private static final int CONSTRUCTION_PREGEN_RADIUS_CHUNKS = 15;
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
                startSiteSurvey(realm, profile.homelandId(), job);
            }
        }
        player.sendSystemMessage(Component.literal(
                "§6[왕국 준비] §f선택한 소속의 수도와 거주지를 실제 지형에 맞춰 건설하고 있습니다. "
                        + "입지 조사와 건설을 서버를 멈추지 않도록 분리해 진행합니다."
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
                startSiteSurvey(realm, homelandId, job);
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

    private static void startSiteSurvey(ServerLevel realm, String homelandId, BuildJob job) {
        RealmSiteLayoutSavedData.RealmSite existing = RealmSitePlanner.site(realm, homelandId);
        if (existing != null && existing.revision() >= RealmSitePlanner.LAYOUT_REVISION) {
            synchronized (job) {
                job.site = existing;
            }
            startSelectedSitePregeneration(realm, homelandId, job, existing);
            return;
        }

        synchronized (job) {
            if (job.finished || job.selectingSite) return;
            job.selectingSite = true;
        }
        LivingKingdoms.LOGGER.info("Starting background generator survey for {}", homelandId);
        job.surveyFuture = CompletableFuture.supplyAsync(
                () -> RealmSitePlanner.surveyGeneratedTerrain(realm, homelandId),
                Util.backgroundExecutor()
        );
        job.surveyFuture.whenComplete((surveyed, throwable) -> realm.getServer().submit(() -> {
            if (throwable != null) {
                failBuild(homelandId, job, throwable);
                return;
            }
            try {
                RealmSiteLayoutSavedData.RealmSite stored = RealmSitePlanner.storeSurvey(realm, homelandId, surveyed);
                synchronized (job) {
                    if (job.finished) return;
                    job.site = stored;
                    job.selectingSite = false;
                }
                startSelectedSitePregeneration(realm, homelandId, job, stored);
            } catch (Throwable storeFailure) {
                failBuild(homelandId, job, storeFailure);
            }
        }));
    }

    private static void startSelectedSitePregeneration(
            ServerLevel realm,
            String homelandId,
            BuildJob job,
            RealmSiteLayoutSavedData.RealmSite site
    ) {
        startGenerationTask(
                realm,
                homelandId,
                job,
                "construction",
                site.centerX() >> 4,
                site.centerZ() >> 4,
                CONSTRUCTION_PREGEN_RADIUS_CHUNKS,
                errors -> preparePlan(realm, homelandId, job, errors)
        );
    }

    private static void startGenerationTask(
            ServerLevel realm,
            String homelandId,
            BuildJob job,
            String phase,
            int centerChunkX,
            int centerChunkZ,
            int radius,
            java.util.function.IntConsumer completion
    ) {
        LivingKingdoms.LOGGER.info(
                "Starting asynchronous {} chunk preparation for {} at chunk {},{} radius={}",
                phase, homelandId, centerChunkX, centerChunkZ, radius
        );
        GenerationTask task = new GenerationTask(realm, centerChunkX, centerChunkZ, radius);
        synchronized (job) {
            if (job.finished) return;
            if (job.task != null) job.task.stop();
            job.task = task;
        }
        task.run(new GenerationTask.Listener() {
            private int lastReported;

            @Override
            public void update(int ok, int error, int skipped, int total) {
                int done = ok + error + skipped;
                if (done - lastReported >= 200 || done == total) {
                    lastReported = done;
                    LivingKingdoms.LOGGER.info(
                            "Realm {} chunk preparation {} progress {}/{} errors={}",
                            phase, homelandId, done, total, error
                    );
                }
            }

            @Override
            public void complete(int error) {
                MinecraftServer server = realm.getServer();
                server.submit(() -> completion.accept(error));
            }
        });
    }

    private static void preparePlan(ServerLevel realm, String homelandId, BuildJob job, int generationErrors) {
        synchronized (job) {
            if (job.finished || job.preparingPlan || job.plan != null) return;
            job.preparingPlan = true;
        }
        try {
            if (generationErrors > 0) {
                throw new IllegalStateException("Construction chunk preparation reported " + generationErrors + " errors");
            }
            long started = System.nanoTime();
            RealmSiteLayoutSavedData.RealmSite site;
            synchronized (job) {
                site = job.site;
            }
            if (site == null) throw new IllegalStateException("Selected homeland site is unavailable");
            IncrementalWorldEditPlan plan = PlannedRealmBuilder.create(realm, homelandId, site);
            synchronized (job) {
                if (job.finished) return;
                job.plan = plan;
                job.preparingPlan = false;
                if (job.task != null) job.task.stop();
            }
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            LivingKingdoms.LOGGER.info(
                    "Prepared incremental homeland plan {} operations={} estimated_writes={} planning_ms={}",
                    homelandId, plan.operationCount(), plan.estimatedWrites(), elapsedMs
            );
        } catch (Throwable throwable) {
            synchronized (job) {
                job.preparingPlan = false;
            }
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
            synchronized (job) {
                job.finished = true;
            }
            notifyCompletions(job, null);
            JOBS.remove(homelandId, job);
        } catch (Throwable throwable) {
            failBuild(homelandId, job, throwable);
        }
    }

    private static void failBuild(String homelandId, BuildJob job, Throwable failure) {
        synchronized (job) {
            if (job.finished) return;
            job.finished = true;
        }
        JOBS.remove(homelandId, job);
        LivingKingdoms.LOGGER.error("Failed incremental homeland construction for {}", homelandId, failure);
        if (job.task != null) job.task.stop();
        if (job.surveyFuture != null) job.surveyFuture.cancel(true);
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

    private static final class BuildJob {
        private final ServerLevel realm;
        private final Set<UUID> waitingPlayers = new LinkedHashSet<>();
        private final Set<Consumer<Throwable>> completions = new LinkedHashSet<>();
        private boolean started;
        private boolean selectingSite;
        private boolean preparingPlan;
        private boolean finished;
        private CompletableFuture<RealmSiteLayoutSavedData.RealmSite> surveyFuture;
        private GenerationTask task;
        private RealmSiteLayoutSavedData.RealmSite site;
        private IncrementalWorldEditPlan plan;
        private int lastReportedPercent = -10;

        private BuildJob(ServerLevel realm) {
            this.realm = realm;
        }
    }
}
