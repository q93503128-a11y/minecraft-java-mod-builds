package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.command.generation.GenerationTask;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Asynchronously pregenerates complete chunks before surveying and constructing one homeland. */
public final class RealmBuildCoordinator {
    private static final int PREGEN_RADIUS_CHUNKS = 21;
    private static final Map<String, BuildJob> JOBS = new ConcurrentHashMap<>();

    private RealmBuildCoordinator() {
    }

    public static void requestPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            player.sendSystemMessage(Component.literal("§c[살아있는 왕국] 판타지 대륙을 불러오지 못했습니다."));
            return;
        }
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, profile.homelandId());
        if (site != null && site.built() && site.revision() >= RealmSitePlanner.LAYOUT_REVISION) {
            LivingRealmWorldManager.finishPlacement(player, profile);
            StarterNpcManager.ensureForPlayer(player, profile);
            return;
        }

        BuildJob job = JOBS.computeIfAbsent(profile.homelandId(), ignored -> new BuildJob());
        synchronized (job) {
            job.waitingPlayers.add(player.getUUID());
            if (!job.started) {
                job.started = true;
                start(realm, profile.homelandId(), job);
            }
        }
        player.sendSystemMessage(Component.literal(
                "§6[왕국 준비] §f선택한 소속의 수도와 거주지를 실제 지형에 맞춰 건설하고 있습니다. 완료되면 자동으로 이동합니다."
        ));
    }

    public static void prepareHomeland(ServerLevel realm, String homelandId, Consumer<Throwable> completion) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, homelandId);
        if (site != null && site.built() && site.revision() >= RealmSitePlanner.LAYOUT_REVISION) {
            completion.accept(null);
            return;
        }
        BuildJob job = JOBS.computeIfAbsent(homelandId, ignored -> new BuildJob());
        synchronized (job) {
            job.completions.add(completion);
            if (!job.started) {
                job.started = true;
                start(realm, homelandId, job);
            }
        }
    }

    private static void start(ServerLevel realm, String homelandId, BuildJob job) {
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
                server.submit(() -> finish(realm, homelandId, job, error));
            }
        });
    }

    private static void finish(ServerLevel realm, String homelandId, BuildJob job, int generationErrors) {
        Throwable failure = null;
        try {
            if (generationErrors > 0) {
                throw new IllegalStateException("Chunk preparation reported " + generationErrors + " errors");
            }
            long started = System.nanoTime();
            RealmSitePlanner.ensureBuilt(realm, homelandId);
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            LivingKingdoms.LOGGER.info("Completed queued homeland {} construction in {}ms", homelandId, elapsedMs);

            for (UUID playerId : Set.copyOf(job.waitingPlayers)) {
                ServerPlayer player = realm.getServer().getPlayerList().getPlayer(playerId);
                if (player == null) continue;
                OriginProfileManager.profile(playerId).ifPresent(profile -> {
                    if (profile.homelandId().equals(homelandId)) {
                        LivingRealmWorldManager.finishPlacement(player, profile);
                        StarterNpcManager.ensureForPlayer(player, profile);
                    }
                });
            }
        } catch (Throwable throwable) {
            failure = throwable;
            LivingKingdoms.LOGGER.error("Failed queued homeland construction for {}", homelandId, throwable);
            for (UUID playerId : Set.copyOf(job.waitingPlayers)) {
                ServerPlayer player = realm.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§c[왕국 준비 실패] §f시작 지역을 만들지 못했습니다. 로그를 확인하십시오."
                    ));
                }
            }
        }

        for (Consumer<Throwable> completion : Set.copyOf(job.completions)) {
            try {
                completion.accept(failure);
            } catch (Throwable callbackFailure) {
                LivingKingdoms.LOGGER.error("Realm build completion callback failed", callbackFailure);
            }
        }
        JOBS.remove(homelandId, job);
    }

    private static int[] nominalCenter(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> new int[]{1500, 250};
            case "kardum_league" -> new int[]{-1500, 250};
            default -> new int[]{0, 0};
        };
    }

    private static final class BuildJob {
        private final Set<UUID> waitingPlayers = new LinkedHashSet<>();
        private final Set<Consumer<Throwable>> completions = new LinkedHashSet<>();
        private boolean started;
        private GenerationTask task;
    }
}
