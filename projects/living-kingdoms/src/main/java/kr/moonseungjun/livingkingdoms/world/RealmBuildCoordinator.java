package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.command.generation.GenerationTask;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Prepares fixed authored continent anchors, pregenerates them, then builds over safe server ticks. */
public final class RealmBuildCoordinator {
    private static final int CONSTRUCTION_PREGEN_RADIUS_CHUNKS = 15;
    private static final int NORMAL_TICK_BUDGET = 3_000;
    private static final int BUSY_TICK_BUDGET = 750;
    private static final int SETTLING_TICKS = 20;
    private static final Map<String, BuildJob> JOBS = new ConcurrentHashMap<>();

    private RealmBuildCoordinator() {
    }

    public static void requestPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            player.sendSystemMessage(Component.literal("§c[살아있는 왕국] 판타지 대륙을 불러오지 못했습니다."));
            send(player, profile.homelandId(), "failed", 0,
                    "판타지 대륙을 불러오지 못했습니다.", false, true);
            return;
        }
        if (RealmSitePlanner.isBuilt(realm, profile.homelandId())) {
            queueResidencePlacement(player, profile, realm);
            return;
        }

        BuildJob job = JOBS.computeIfAbsent(profile.homelandId(), ignored -> new BuildJob(realm));
        synchronized (job) {
            job.waitingPlayers.add(player.getUUID());
            if (!job.started) {
                job.started = true;
                setStatus(job, "terrain", 8, "판타지 대륙의 고정 지형을 준비하고 있습니다.");
                startAuthoredSite(realm, profile.homelandId(), job);
            }
        }
        sendCurrent(player, profile.homelandId(), job);
        player.sendSystemMessage(Component.literal(
                "§6[왕국 준비] §f정해진 판타지 대륙 좌표에 수도와 거주지를 건설하고 있습니다. "
                        + "진행 화면은 실제 거주지 입주가 끝날 때까지 유지됩니다."
        ));
    }

    private static void queueResidencePlacement(ServerPlayer player, OriginProfile profile, ServerLevel realm) {
        BuildJob job = JOBS.computeIfAbsent(profile.homelandId(), ignored -> new BuildJob(realm));
        synchronized (job) {
            job.started = true;
            job.site = RealmSitePlanner.site(realm, profile.homelandId());
            job.awaitingResidence = true;
            job.waitingPlayers.add(player.getUUID());
            setStatus(job, "residence", 99, "왕도 시민구의 실제 임대방과 내부 동선을 준비하고 있습니다.");
        }
        LivingRealmWorldManager.prepareResidence(realm, profile);
        sendCurrent(player, profile.homelandId(), job);
    }

    public static void prepareHomeland(ServerLevel realm, String homelandId, Consumer<Throwable> completion) {
        if (RealmSitePlanner.isBuilt(realm, homelandId)) {
            if (requiresResidenceCiAudit(homelandId)) {
                BuildJob job = JOBS.computeIfAbsent(homelandId, ignored -> new BuildJob(realm));
                synchronized (job) {
                    job.started = true;
                    job.site = RealmSitePlanner.site(realm, homelandId);
                    job.awaitingResidence = true;
                    job.auditResidenceWithoutPlayer = true;
                    job.completions.add(completion);
                    setStatus(job, "residence", 99, "CI가 실제 왕도 임대방을 검증하고 있습니다.");
                }
                SafeResidenceLocator.prepareResidence(realm, "erden_kingdom", "erden_city_room");
            } else {
                completion.accept(null);
            }
            return;
        }
        BuildJob job = JOBS.computeIfAbsent(homelandId, ignored -> new BuildJob(realm));
        synchronized (job) {
            job.completions.add(completion);
            if (!job.started) {
                job.started = true;
                setStatus(job, "terrain", 8, "판타지 대륙의 고정 지형을 준비하고 있습니다.");
                startAuthoredSite(realm, homelandId, job);
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
            int settlingTicks;
            boolean awaitingResidence;
            synchronized (job) {
                plan = job.plan;
                settlingTicks = job.settlingTicks;
                awaitingResidence = job.awaitingResidence;
            }
            if (job.realm.getServer() != event.getServer()) continue;

            try {
                if (awaitingResidence) {
                    continueResidencePlacement(homelandId, job);
                    continue;
                }
                if (plan == null) continue;
                if (settlingTicks >= 0) {
                    continueSettling(homelandId, job);
                    continue;
                }
                int used = plan.apply(job.realm, remaining);
                remaining -= used;
                reportProgress(homelandId, job, plan);
                if (plan.done()) beginSettling(homelandId, job);
            } catch (Throwable throwable) {
                failBuild(homelandId, job, throwable);
            }
        }
    }

    private static void startAuthoredSite(ServerLevel realm, String homelandId, BuildJob job) {
        try {
            RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.ensureSite(realm, homelandId);
            synchronized (job) {
                if (job.finished) return;
                job.site = site;
            }
            updateProgress(homelandId, job, "chunks", 24,
                    "설계된 수도권 지형 청크를 생성하고 있습니다.", true);
            LivingKingdoms.LOGGER.info(
                    "Preparing authored homeland {} at fixed anchor {},{} baseY={}",
                    homelandId, site.centerX(), site.centerZ(), site.baseY()
            );
            startSelectedSitePregeneration(realm, homelandId, job, site);
        } catch (Throwable throwable) {
            failBuild(homelandId, job, throwable);
        }
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
            private int lastClientPercent = 23;

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
                int clientPercent = 24 + Math.round(done / (float) Math.max(1, total) * 21.0F);
                if (clientPercent >= lastClientPercent + 4 || done == total) {
                    lastClientPercent = clientPercent;
                    realm.getServer().submit(() -> updateProgress(
                            homelandId, job, "chunks", clientPercent,
                            "수도와 거주지 주변 청크를 생성하고 있습니다.", false
                    ));
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
            updateProgress(homelandId, job, "planning", 47,
                    "도로, 시설, 거주지 배치를 조립하고 있습니다.", true);
            long started = System.nanoTime();
            RealmSiteLayoutSavedData.RealmSite site;
            synchronized (job) {
                site = job.site;
            }
            if (site == null) throw new IllegalStateException("Authored homeland site is unavailable");
            IncrementalWorldEditPlan plan = PlannedRealmBuilder.create(realm, homelandId, site);
            synchronized (job) {
                if (job.finished) return;
                job.plan = plan;
                job.preparingPlan = false;
                if (job.task != null) job.task.stop();
            }
            updateProgress(homelandId, job, "building", 50,
                    "도로와 건물을 구역별로 적용하고 있습니다.", true);
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            LivingKingdoms.LOGGER.info(
                    "Prepared incremental homeland plan {} operations={} estimated_writes={} suppressed_terrain_writes={} planning_ms={}",
                    homelandId, plan.operationCount(), plan.estimatedWrites(),
                    plan.suppressedTerrainWrites(), elapsedMs
            );
        } catch (Throwable throwable) {
            synchronized (job) {
                job.preparingPlan = false;
            }
            failBuild(homelandId, job, throwable);
        }
    }

    private static void reportProgress(String homelandId, BuildJob job, IncrementalWorldEditPlan plan) {
        int planPercent = Math.min(100, Math.round(plan.progress() * 100.0F));
        if (planPercent >= job.lastReportedPercent + 10 || planPercent == 100) {
            job.lastReportedPercent = planPercent;
            LivingKingdoms.LOGGER.info(
                    "Realm construction {} progress {}% ({}/{})",
                    homelandId, planPercent, plan.appliedWrites(), plan.estimatedWrites()
            );
        }
        int clientPercent = 50 + Math.round(plan.progress() * 48.0F);
        updateProgress(homelandId, job, "building", clientPercent,
                "도로와 건물을 구역별로 배치하고 있습니다.", false);
    }

    private static void beginSettling(String homelandId, BuildJob job) {
        RealmSiteLayoutSavedData.RealmSite site;
        synchronized (job) {
            if (job.finished || job.settlingTicks >= 0) return;
            site = job.site;
            job.settlingTicks = SETTLING_TICKS;
        }
        if (site == null) throw new IllegalStateException("Authored homeland site disappeared before settling");
        ConstructionDebrisCleaner.cleanConstructionCompletion(job.realm, homelandId, site);
        updateProgress(homelandId, job, "settling", 99,
                "마지막 블록 갱신과 건설 잔해를 정리하고 있습니다.", true);
        LivingKingdoms.LOGGER.info(
                "Settling authored homeland {} for {} ticks before player placement",
                homelandId, SETTLING_TICKS
        );
    }

    private static void continueSettling(String homelandId, BuildJob job) {
        RealmSiteLayoutSavedData.RealmSite site;
        int remaining;
        synchronized (job) {
            if (job.finished || job.settlingTicks < 0) return;
            site = job.site;
            remaining = --job.settlingTicks;
        }
        if (site == null) throw new IllegalStateException("Authored homeland site disappeared while settling");
        ConstructionDebrisCleaner.cleanConstructionCompletion(job.realm, homelandId, site);
        if (remaining <= 0) completeBuild(homelandId, job);
    }

    private static void completeBuild(String homelandId, BuildJob job) {
        try {
            RealmSiteLayoutSavedData.RealmSite site = job.site;
            if (site == null) throw new IllegalStateException("Authored homeland site disappeared before completion");
            ConstructionDebrisCleaner.cleanConstructionCompletion(job.realm, homelandId, site);
            RealmSitePlanner.markBuilt(job.realm, homelandId);

            boolean ciResidenceAudit = requiresResidenceCiAudit(homelandId);
            synchronized (job) {
                job.settlingTicks = -1;
                if (job.waitingPlayers.isEmpty() && !ciResidenceAudit) {
                    job.finished = true;
                    setStatus(job, "complete", 100, "왕국 준비가 끝났습니다.");
                } else {
                    job.awaitingResidence = true;
                    job.auditResidenceWithoutPlayer = ciResidenceAudit;
                    setStatus(job, "residence", 99,
                            ciResidenceAudit
                                    ? "CI가 실제 왕도 임대방과 내부 동선을 검증하고 있습니다."
                                    : "왕도 시민구의 실제 임대방과 내부 동선을 준비하고 있습니다.");
                }
            }
            if (job.finished) {
                notifyCompletions(job, null);
                JOBS.remove(homelandId, job);
                return;
            }

            if (ciResidenceAudit) {
                SafeResidenceLocator.prepareResidence(job.realm, "erden_kingdom", "erden_city_room");
            }
            for (UUID playerId : Set.copyOf(job.waitingPlayers)) {
                ServerPlayer player = job.realm.getServer().getPlayerList().getPlayer(playerId);
                if (player == null) continue;
                OriginProfileManager.profile(playerId).ifPresent(profile -> {
                    if (profile.homelandId().equals(homelandId)) {
                        LivingRealmWorldManager.prepareResidence(job.realm, profile);
                    }
                });
            }
            broadcast(homelandId, job, false, false, true);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_RESIDENCE_GATE homeland={} site_built=true awaiting_players={} ci_residence_audit={} synthetic_fallback=false premature_complete=false",
                    homelandId, job.waitingPlayers.size(), ciResidenceAudit);
        } catch (Throwable throwable) {
            failBuild(homelandId, job, throwable);
        }
    }

    private static void continueResidencePlacement(String homelandId, BuildJob job) {
        if (job.auditResidenceWithoutPlayer) {
            SafeResidenceLocator.prepareResidence(job.realm, "erden_kingdom", "erden_city_room");
            var feet = SafeResidenceLocator.tryResidence(job.realm, "erden_kingdom", "erden_city_room");
            if (feet == null) return;
            var entrance = SafeResidenceLocator.residenceEntrance(
                    job.realm, "erden_kingdom", "erden_city_room");
            if (!"tenement".equals(entrance.role())) {
                throw new IllegalStateException("Player residence resolved outside a tenement: " + entrance.role());
            }
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_PLAYER_RESIDENCE_DIAGNOSTIC_PASS role={} entrance={},{} feet={} authored_tenement=true upper_route=true synthetic_fallback=false staging_complete_gate=true",
                    entrance.role(), entrance.x(), entrance.z(), feet);
            synchronized (job) {
                job.auditResidenceWithoutPlayer = false;
            }
        }

        for (UUID playerId : Set.copyOf(job.waitingPlayers)) {
            ServerPlayer player = job.realm.getServer().getPlayerList().getPlayer(playerId);
            if (player == null) {
                synchronized (job) {
                    job.waitingPlayers.remove(playerId);
                }
                continue;
            }
            OriginProfile profile = OriginProfileManager.profile(playerId).orElse(null);
            if (profile == null || !profile.homelandId().equals(homelandId)) {
                synchronized (job) {
                    job.waitingPlayers.remove(playerId);
                }
                continue;
            }

            LivingRealmWorldManager.prepareResidence(job.realm, profile);
            if (!LivingRealmWorldManager.finishPlacement(player, profile)) continue;

            StarterNpcManager.ensureForPlayer(player, profile);
            send(player, homelandId, "complete", 100,
                    "실제 왕도 임대방과 내부 동선을 검증했습니다. 입주를 완료합니다.", true, false);
            synchronized (job) {
                job.waitingPlayers.remove(playerId);
            }
        }

        synchronized (job) {
            if (job.auditResidenceWithoutPlayer || !job.waitingPlayers.isEmpty()) return;
            job.awaitingResidence = false;
            job.finished = true;
            setStatus(job, "complete", 100, "왕국 준비와 실제 거주지 입주가 끝났습니다.");
        }
        notifyCompletions(job, null);
        JOBS.remove(homelandId, job);
    }

    private static boolean requiresResidenceCiAudit(String homelandId) {
        return "erden_kingdom".equals(homelandId)
                && "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private static void failBuild(String homelandId, BuildJob job, Throwable failure) {
        synchronized (job) {
            if (job.finished) return;
            job.finished = true;
            setStatus(job, "failed", job.progressPercent,
                    "왕국 생성에 실패했습니다. 서버 로그를 확인하십시오.");
        }
        JOBS.remove(homelandId, job);
        LivingKingdoms.LOGGER.error("Failed incremental homeland construction for {}", homelandId, failure);
        if (job.task != null) job.task.stop();
        broadcast(homelandId, job, false, true, true);
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

    private static void updateProgress(String homelandId, BuildJob job, String phase,
                                       int percent, String message, boolean force) {
        boolean changed;
        synchronized (job) {
            int safePercent = Math.max(job.progressPercent, Math.min(100, percent));
            changed = force || !phase.equals(job.phase) || safePercent >= job.lastSentClientPercent + 4;
            setStatus(job, phase, safePercent, message);
            if (changed) job.lastSentClientPercent = safePercent;
        }
        if (changed) broadcast(homelandId, job, false, false, false);
    }

    private static void setStatus(BuildJob job, String phase, int percent, String message) {
        job.phase = phase;
        job.progressPercent = Math.max(0, Math.min(100, percent));
        job.progressMessage = message;
    }

    private static void sendCurrent(ServerPlayer player, String homelandId, BuildJob job) {
        String phase;
        int percent;
        String message;
        synchronized (job) {
            phase = job.phase;
            percent = job.progressPercent;
            message = job.progressMessage;
        }
        send(player, homelandId, phase, percent, message, false, false);
    }

    private static void broadcast(String homelandId, BuildJob job,
                                  boolean complete, boolean failed, boolean force) {
        String phase;
        int percent;
        String message;
        Set<UUID> players;
        synchronized (job) {
            phase = job.phase;
            percent = job.progressPercent;
            message = job.progressMessage;
            players = Set.copyOf(job.waitingPlayers);
        }
        for (UUID playerId : players) {
            ServerPlayer player = job.realm.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) send(player, homelandId, phase, percent, message, complete, failed);
        }
    }

    private static void send(ServerPlayer player, String homelandId, String phase,
                             int percent, String message, boolean complete, boolean failed) {
        PacketDistributor.sendToPlayer(player, new RealmBuildProgressPayload(
                homelandId, phase, percent, message, complete, failed
        ));
    }

    private static void notifyCompletions(BuildJob job, Throwable failure) {
        Set<Consumer<Throwable>> callbacks;
        synchronized (job) {
            callbacks = Set.copyOf(job.completions);
            job.completions.clear();
        }
        for (Consumer<Throwable> completion : callbacks) {
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
        private boolean preparingPlan;
        private boolean awaitingResidence;
        private boolean auditResidenceWithoutPlayer;
        private boolean finished;
        private GenerationTask task;
        private RealmSiteLayoutSavedData.RealmSite site;
        private IncrementalWorldEditPlan plan;
        private int settlingTicks = -1;
        private int lastReportedPercent = -10;
        private int lastSentClientPercent = -1;
        private String phase = "preparing";
        private int progressPercent = 2;
        private String progressMessage = "출신과 소속을 확인하고 있습니다.";

        private BuildJob(ServerLevel realm) {
            this.realm = realm;
        }
    }
}
