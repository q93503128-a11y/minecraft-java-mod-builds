package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** CI-only physical proof for the real first-player Erden residence destination. */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenResidencePlacementPhysicalAudit {
    private static final int TICKET_RADIUS = 1;
    private static final int REFRESH_INTERVAL = 40;
    private static final int MAX_WAIT_TICKS = 1_200;
    private static final int MAX_HORIZONTAL_FALLBACK = 10;
    private static final int MAX_VERTICAL_FALLBACK = 16;
    private static final String AUTHORED_ROLE = "residential_middle_south_04";

    private static MinecraftServer activeServer;
    private static ChunkPos ticketChunk;
    private static int ageTicks;
    private static int refreshes;
    private static boolean passed;

    private ErdenResidencePlacementPhysicalAudit() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!enabled() || passed) return;
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, PlayableOriginCatalog.DEFAULT_HOMELAND)) return;

        boolean authoredRolePresent = ExternalDistrictBuildingBuilder.entrances().stream()
                .anyMatch(entrance -> entrance.role().equals(AUTHORED_ROLE) && entrance.residential());
        if (!authoredRolePresent) {
            fail(level, "authored_starting_residence_missing role=" + AUTHORED_ROLE);
            return;
        }

        BlockPos preferred = SafeResidenceLocator.preferredResidence(
                level, PlayableOriginCatalog.DEFAULT_HOMELAND, PlayableOriginCatalog.DEFAULT_RESIDENCE);
        ChunkPos chunk = new ChunkPos(preferred.getX() >> 4, preferred.getZ() >> 4);
        if (ticketChunk == null) {
            ticketChunk = chunk;
            retain(level, chunk);
        }
        if (!ticketChunk.equals(chunk)) {
            fail(level, "residence_chunk_drift expected=" + ticketChunk + " actual=" + chunk);
            return;
        }

        ageTicks++;
        if (ageTicks % REFRESH_INTERVAL == 0) retain(level, chunk);
        if (ageTicks > MAX_WAIT_TICKS) {
            fail(level, "residence_chunk_timeout loaded=" + loadedChunks(level, chunk));
            return;
        }
        if (loadedChunks(level, chunk) != 9) return;

        BlockPos actual = SafeResidenceLocator.residence(
                level, PlayableOriginCatalog.DEFAULT_HOMELAND, PlayableOriginCatalog.DEFAULT_RESIDENCE);
        if (!SafeResidenceLocator.isWalkable(level, actual)) {
            fail(level, "residence_not_walkable preferred=" + preferred + " actual=" + actual);
            return;
        }
        if (Math.abs(actual.getX() - preferred.getX()) > MAX_HORIZONTAL_FALLBACK
                || Math.abs(actual.getZ() - preferred.getZ()) > MAX_HORIZONTAL_FALLBACK
                || Math.abs(actual.getY() - preferred.getY()) > MAX_VERTICAL_FALLBACK) {
            fail(level, "residence_existing_walkable_out_of_bounds preferred=" + preferred + " actual=" + actual);
            return;
        }

        release(level, chunk);
        passed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_RESIDENCE_PHYSICAL_PASS authored_role={} preferred={},{},{} actual={},{},{} preferred_chunk={},{} loaded_probe_chunks=9 walkable=true existing_authored_walkable=true no_residence_block_carving=true bounded_fallback=true transient_ticket_released=true ticket_radius={} refresh_ticks={} refreshes={} persistent_forced_chunks=false",
                AUTHORED_ROLE,
                preferred.getX(), preferred.getY(), preferred.getZ(),
                actual.getX(), actual.getY(), actual.getZ(),
                chunk.x(), chunk.z(), TICKET_RADIUS, REFRESH_INTERVAL, refreshes);
    }

    private static void retain(ServerLevel level, ChunkPos chunk) {
        level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, TICKET_RADIUS);
        refreshes++;
    }

    private static void release(ServerLevel level, ChunkPos chunk) {
        level.getChunkSource().removeTicketWithRadius(TicketType.PORTAL, chunk, TICKET_RADIUS);
        ticketChunk = null;
    }

    private static int loadedChunks(ServerLevel level, ChunkPos center) {
        int loaded = 0;
        for (int dx = -TICKET_RADIUS; dx <= TICKET_RADIUS; dx++) {
            for (int dz = -TICKET_RADIUS; dz <= TICKET_RADIUS; dz++) {
                if (level.hasChunk(center.x() + dx, center.z() + dz)) loaded++;
            }
        }
        return loaded;
    }

    private static void fail(ServerLevel level, String reason) {
        if (ticketChunk != null) release(level, ticketChunk);
        throw new IllegalStateException("Erden residence physical audit failed: " + reason);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        ticketChunk = null;
        ageTicks = 0;
        refreshes = 0;
        passed = false;
    }

    private static boolean enabled() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_RESIDENCE_PLACEMENT_TEST"));
    }
}
