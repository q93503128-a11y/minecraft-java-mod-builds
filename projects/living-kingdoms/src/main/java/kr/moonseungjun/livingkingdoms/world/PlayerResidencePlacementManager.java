package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Final entry barrier between the temporary staging platform and an actual authored Erden room.
 * The client only accepts the residence_complete packet as final while this barrier is active.
 */
public final class PlayerResidencePlacementManager {
    private static final boolean CI_PLAYER_ENTRY =
            "1".equals(System.getenv("LIVING_KINGDOMS_CI_PLAYER_ENTRY"));
    private static final int MAX_WAIT_TICKS = 6_000;
    private static final int REPORT_INTERVAL = 200;
    private static final Map<UUID, Pending> PENDING = new LinkedHashMap<>();

    private static MinecraftServer activeServer;
    private static ChunkPos retainedChunk;
    private static boolean ticketHeld;
    private static boolean diagnosticPassed;
    private static int diagnosticAgeTicks;

    private PlayerResidencePlacementManager() {
    }

    public static synchronized void queue(ServerPlayer player, OriginProfile profile) {
        MinecraftServer server = player.level().getServer();
        if (activeServer != server) resetFor(server);
        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null || !RealmSitePlanner.isBuilt(realm, profile.homelandId())) return;
        PENDING.putIfAbsent(player.getUUID(), new Pending(profile));
        ensureTicket(realm, profile.homelandId(), profile.residenceId());
        PacketDistributor.sendToPlayer(player, new RealmBuildProgressPayload(
                profile.homelandId(), "residence", 99,
                "왕도 시민구의 실제 임대방과 출입 동선을 확인하고 있습니다.", false, false
        ));
    }

    public static synchronized boolean pending(UUID playerId) {
        return PENDING.containsKey(playerId);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        synchronized (PlayerResidencePlacementManager.class) {
            if (activeServer != server) resetFor(server);
        }
        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return;

        synchronized (PlayerResidencePlacementManager.class) {
            if (CI_PLAYER_ENTRY && !diagnosticPassed) runDiagnostic(realm);
            if (PENDING.isEmpty()) {
                if ((!CI_PLAYER_ENTRY || diagnosticPassed) && ticketHeld) releaseTicket(realm);
                return;
            }

            Iterator<Map.Entry<UUID, Pending>> iterator = PENDING.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Pending> entry = iterator.next();
                UUID playerId = entry.getKey();
                Pending pending = entry.getValue();
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    iterator.remove();
                    continue;
                }

                ensureTicket(realm, pending.profile.homelandId(), pending.profile.residenceId());
                if (LivingRealmWorldManager.tryFinishPlacement(player, pending.profile)) {
                    StarterNpcManager.ensureForPlayer(player, pending.profile);
                    RealmEconomyManager.sync(player);
                    PacketDistributor.sendToPlayer(player, new RealmBuildProgressPayload(
                            pending.profile.homelandId(), "residence_complete", 100,
                            "실제 시민구 임대방 확인이 끝났습니다. 입국합니다.", true, false
                    ));
                    iterator.remove();
                    continue;
                }

                pending.ageTicks++;
                if (pending.ageTicks % 20 == 0 && retainedChunk != null) {
                    ErdenCapitalStreamingBuilder.requestChunk(realm, retainedChunk.x(), retainedChunk.z());
                }
                if (pending.ageTicks % REPORT_INTERVAL == 0) {
                    LivingKingdoms.LOGGER.info(
                            "Waiting for verified Erden player residence player={} age_ticks={} chunk={} loaded={} built={}",
                            playerId, pending.ageTicks, retainedChunk,
                            retainedChunk != null && realm.hasChunk(retainedChunk.x(), retainedChunk.z()),
                            retainedChunk != null && ErdenCapitalStreamingBuilder.isChunkBuilt(
                                    realm, retainedChunk.x(), retainedChunk.z())
                    );
                }
                if (pending.ageTicks >= MAX_WAIT_TICKS) {
                    PacketDistributor.sendToPlayer(player, new RealmBuildProgressPayload(
                            pending.profile.homelandId(), "failed", 99,
                            "실제 임대방 검증이 제한 시간 안에 끝나지 않았습니다. 임시 지점으로 이동하지 않았습니다.",
                            false, true
                    ));
                    LivingKingdoms.LOGGER.error(
                            "Timed out verified Erden residence placement player={} chunk={} roof_fallback=false staging_exit=false",
                            playerId, retainedChunk
                    );
                    iterator.remove();
                }
            }
            if (PENDING.isEmpty() && (!CI_PLAYER_ENTRY || diagnosticPassed)) releaseTicket(realm);
        }
    }

    private static void runDiagnostic(ServerLevel realm) {
        if (!RealmSitePlanner.isBuilt(realm, PlayableOriginCatalog.DEFAULT_HOMELAND)) return;
        ensureTicket(realm, PlayableOriginCatalog.DEFAULT_HOMELAND, PlayableOriginCatalog.DEFAULT_RESIDENCE);
        if (retainedChunk != null) {
            ErdenCapitalStreamingBuilder.requestChunk(realm, retainedChunk.x(), retainedChunk.z());
        }
        BlockPos target = SafeResidenceLocator.residence(
                realm, PlayableOriginCatalog.DEFAULT_HOMELAND, PlayableOriginCatalog.DEFAULT_RESIDENCE);
        if (target == null) {
            diagnosticAgeTicks++;
            if (diagnosticAgeTicks >= MAX_WAIT_TICKS) {
                throw new IllegalStateException("Verified Erden player residence did not become ready in CI");
            }
            return;
        }

        ExternalUrbanFabricBuilder.UrbanEntrance entrance = SafeResidenceLocator.starterResidenceEntrance(
                realm, PlayableOriginCatalog.DEFAULT_HOMELAND, PlayableOriginCatalog.DEFAULT_RESIDENCE);
        ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement =
                ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics().stream()
                        .filter(candidate -> candidate.entrance().equals(entrance))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Starter residence has no authored building placement"));
        boolean insideBuilding = target.getX() >= placement.minX() && target.getX() <= placement.maxX()
                && target.getZ() >= placement.minZ() && target.getZ() <= placement.maxZ();
        if (!"tenement".equals(placement.role())
                || !insideBuilding
                || !SafeResidenceLocator.isWalkable(realm, target)
                || !ErdenUrbanResidenceResolver.isResidenceReady(realm, entrance)) {
            throw new IllegalStateException(
                    "Invalid authored starter residence target=" + target + " placement=" + placement.role());
        }
        diagnosticPassed = true;
        if (PENDING.isEmpty()) releaseTicket(realm);
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_PLAYER_RESIDENCE_PASS role=tenement target={},{},{} building={},{} verified_authored=true target_inside_building=true walkable=true terrain_scan=false roof_fallback=false staging=false temporary_ticket_released={} persistent_forced_chunks=false",
                target.getX(), target.getY(), target.getZ(), entrance.x(), entrance.z(), !ticketHeld
        );
    }

    private static void ensureTicket(ServerLevel realm, String homelandId, String residenceId) {
        ChunkPos wanted = SafeResidenceLocator.residenceChunk(realm, homelandId, residenceId);
        if (ticketHeld && wanted.equals(retainedChunk)) {
            ErdenCapitalStreamingBuilder.requestChunk(realm, wanted.x(), wanted.z());
            return;
        }
        if (ticketHeld) releaseTicket(realm);
        retainedChunk = wanted;
        realm.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, wanted, 0);
        ErdenCapitalStreamingBuilder.requestChunk(realm, wanted.x(), wanted.z());
        ticketHeld = true;
        LivingKingdoms.LOGGER.info(
                "Retained temporary verified-residence chunk {},{} persistent_forced_chunks=false",
                wanted.x(), wanted.z());
    }

    private static void releaseTicket(ServerLevel realm) {
        if (!ticketHeld || retainedChunk == null) return;
        realm.getChunkSource().removeTicketWithRadius(TicketType.PORTAL, retainedChunk, 0);
        LivingKingdoms.LOGGER.info(
                "Released temporary verified-residence chunk {},{}",
                retainedChunk.x(), retainedChunk.z());
        ticketHeld = false;
        retainedChunk = null;
    }

    private static void resetFor(MinecraftServer server) {
        if (activeServer != null && ticketHeld && retainedChunk != null) {
            ServerLevel oldRealm = activeServer.getLevel(StarterRealmManager.REALM_KEY);
            if (oldRealm != null) releaseTicket(oldRealm);
        }
        PENDING.clear();
        activeServer = server;
        retainedChunk = null;
        ticketHeld = false;
        diagnosticPassed = false;
        diagnosticAgeTicks = 0;
    }

    private static final class Pending {
        private final OriginProfile profile;
        private int ageTicks;

        private Pending(OriginProfile profile) {
            this.profile = profile;
        }
    }
}
