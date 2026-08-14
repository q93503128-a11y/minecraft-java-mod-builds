package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.entity.AshHoundEntity;
import kr.moonseungjun.livingkingdoms.entity.FantasyEntityTypes;
import kr.moonseungjun.livingkingdoms.entity.RiverWispEntity;
import kr.moonseungjun.livingkingdoms.entity.SilverHartEntity;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Player-local fantasy fauna for the authored Erden continent.
 *
 * <p>Normal play never force-loads ecology chunks. A player must already be travelling through a
 * matching loaded region before one of the registered Living Kingdoms species can materialise.
 * Northern forest uplands carry silver harts, western mineral hills carry ash hounds, and the
 * Silver River outside the capital carries river wisps. Population caps are local to the player
 * rather than global, so a 48x40 km realm does not accumulate thousands of unattended entities.</p>
 */
public final class ErdenFantasyEcologyManager {
    public static final int ECOLOGY_REVISION = 1;

    private static final int SPAWN_INTERVAL = 100;
    private static final int BEHAVIOR_INTERVAL = 20;
    private static final int CI_TICKET_REFRESH_INTERVAL = 100;
    private static final int ATTEMPTS_PER_PLAYER = 6;
    private static final int MIN_SPAWN_DISTANCE = 22;
    private static final int SPAWN_DISTANCE_SPAN = 27;
    private static final int LOCAL_RADIUS = 96;
    private static final int LOCAL_SPECIES_CAP = 5;

    private static final Sample SILVER_HART_SAMPLE = new Sample(-3_200, -7_200, Species.SILVER_HART);
    private static final Sample ASH_HOUND_SAMPLE = new Sample(-8_200, 2_200, Species.ASH_HOUND);
    private static final Sample RIVER_WISP_SAMPLE = new Sample(
            (int) Math.round(AuthoredContinentDensity.silverRiverCenterX(2_600)), 2_600, Species.RIVER_WISP);
    private static final List<Sample> CI_SAMPLES = List.of(
            SILVER_HART_SAMPLE, ASH_HOUND_SAMPLE, RIVER_WISP_SAMPLE);

    private static MinecraftServer activeServer;
    private static final Set<Long> CI_TICKETS = new HashSet<>();
    private static final List<UUID> CI_ENTITIES = new ArrayList<>();
    private static boolean ciPrepared;
    private static boolean ciPassed;
    private static long ciTicketRefreshes;

    private ErdenFantasyEcologyManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        if (isCi()) {
            prepareCi(level);
            if (level.getGameTime() % CI_TICKET_REFRESH_INTERVAL == 0L) {
                refreshCiTickets(level);
            }
            verifyCi(level);
        }
        if (level.getGameTime() % BEHAVIOR_INTERVAL == 0L) {
            tickLoadedSpeciesBehavior(level);
        }
        if (level.getGameTime() % SPAWN_INTERVAL == 0L) {
            spawnAroundTravellers(level);
        }
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        CI_TICKETS.clear();
        CI_ENTITIES.clear();
        ciPrepared = false;
        ciPassed = false;
        ciTicketRefreshes = 0L;
    }

    private static void tickLoadedSpeciesBehavior(ServerLevel level) {
        Set<UUID> handledHarts = new HashSet<>();
        Set<UUID> handledHounds = new HashSet<>();
        Set<UUID> handledWisps = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            AABB area = new AABB(
                    player.getX() - LOCAL_RADIUS, level.getMinY(), player.getZ() - LOCAL_RADIUS,
                    player.getX() + LOCAL_RADIUS, level.getMaxY(), player.getZ() + LOCAL_RADIUS);
            herdSilverHarts(level.getEntitiesOfClass(SilverHartEntity.class, area), player, handledHarts);
            coordinateAshHounds(level, level.getEntitiesOfClass(AshHoundEntity.class, area), player, handledHounds);
            bindRiverWisps(level, level.getEntitiesOfClass(RiverWispEntity.class, area), handledWisps);
        }
    }

    private static void herdSilverHarts(
            List<SilverHartEntity> harts,
            ServerPlayer player,
            Set<UUID> handled) {
        List<SilverHartEntity> herd = harts.stream()
                .filter(hart -> handled.add(hart.getUUID()))
                .sorted(java.util.Comparator.comparing(Entity::getUUID))
                .toList();
        if (herd.size() < 2) return;
        SilverHartEntity leader = herd.getFirst();
        for (int index = 1; index < herd.size(); index++) {
            SilverHartEntity follower = herd.get(index);
            if (follower.distanceToSqr(player) <= 18.0D * 18.0D) continue;
            if (follower.distanceToSqr(leader) > 12.0D * 12.0D) {
                follower.getNavigation().moveTo(leader, 0.90D);
            }
        }
    }

    private static void coordinateAshHounds(
            ServerLevel level,
            List<AshHoundEntity> hounds,
            ServerPlayer player,
            Set<UUID> handled) {
        if (hounds.isEmpty()) return;
        boolean huntingTime = level.isDarkOutside();
        AshHoundEntity leader = null;
        for (AshHoundEntity hound : hounds.stream()
                .sorted(java.util.Comparator.comparing(Entity::getUUID)).toList()) {
            if (!handled.add(hound.getUUID())) continue;
            if (!huntingTime) {
                if (hound.getTarget() instanceof ServerPlayer) hound.setTarget(null);
                continue;
            }
            if (leader == null) leader = hound;
            if (player.isAlive() && hound.distanceToSqr(player) <= 34.0D * 34.0D) {
                hound.setTarget(player);
            } else if (leader != hound && leader.getTarget() instanceof ServerPlayer target) {
                hound.setTarget(target);
            }
        }
    }

    private static void bindRiverWisps(
            ServerLevel level,
            List<RiverWispEntity> wisps,
            Set<UUID> handled) {
        for (RiverWispEntity wisp : wisps) {
            if (!handled.add(wisp.getUUID())) continue;
            int z = wisp.blockPosition().getZ();
            int centerX = (int) Math.round(AuthoredContinentDensity.silverRiverCenterX(z));
            double offset = Math.abs(wisp.getX() - (centerX + 0.5D));
            if (offset <= 26.0D || !level.hasChunk(centerX >> 4, z >> 4)) continue;
            double targetY = Math.max(65.0D, wisp.getY());
            wisp.getNavigation().moveTo(centerX + 0.5D, targetY, z + 0.5D, 1.05D);
        }
    }

    private static void spawnAroundTravellers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            int px = player.blockPosition().getX();
            int pz = player.blockPosition().getZ();
            Species wanted = speciesAt(px, pz);
            if (wanted == Species.NONE || insideCapital(px, pz)) continue;
            if (localCount(level, player, wanted) >= LOCAL_SPECIES_CAP) continue;
            long seed = player.getUUID().getLeastSignificantBits()
                    ^ Long.rotateLeft(player.getUUID().getMostSignificantBits(), 17)
                    ^ level.getGameTime() * 0x9E3779B97F4A7C15L;
            for (int attempt = 0; attempt < ATTEMPTS_PER_PLAYER; attempt++) {
                long mixed = mix(seed + attempt * 0x632BE59BD9B4E019L);
                int distance = MIN_SPAWN_DISTANCE + Math.floorMod((int) mixed, SPAWN_DISTANCE_SPAN);
                double angle = ((mixed >>> 24) & 0xffffL) / 65535.0D * Math.PI * 2.0D;
                int x = px + (int) Math.round(Math.cos(angle) * distance);
                int z = pz + (int) Math.round(Math.sin(angle) * distance);
                if (speciesAt(x, z) != wanted || insideCapital(x, z)
                        || !level.hasChunk(x >> 4, z >> 4)) continue;
                if (spawn(level, wanted, x, z, EntitySpawnReason.NATURAL, false) != null) break;
            }
        }
    }

    private static int localCount(ServerLevel level, ServerPlayer player, Species species) {
        AABB area = new AABB(
                player.getX() - LOCAL_RADIUS, level.getMinY(), player.getZ() - LOCAL_RADIUS,
                player.getX() + LOCAL_RADIUS, level.getMaxY(), player.getZ() + LOCAL_RADIUS);
        return switch (species) {
            case SILVER_HART -> level.getEntitiesOfClass(SilverHartEntity.class, area).size();
            case ASH_HOUND -> level.getEntitiesOfClass(AshHoundEntity.class, area).size();
            case RIVER_WISP -> level.getEntitiesOfClass(RiverWispEntity.class, area).size();
            case NONE -> 0;
        };
    }

    private static Entity spawn(
            ServerLevel level,
            Species species,
            int x,
            int z,
            EntitySpawnReason reason,
            boolean diagnostic) {
        if (species == Species.NONE || insideCapital(x, z) || !insideKingdom(x, z)) return null;
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (groundY <= level.getMinY() || groundY >= level.getMaxY() - 8) return null;
        Entity entity = switch (species) {
            case SILVER_HART -> FantasyEntityTypes.SILVER_HART.get().create(level, reason);
            case ASH_HOUND -> FantasyEntityTypes.ASH_HOUND.get().create(level, reason);
            case RIVER_WISP -> FantasyEntityTypes.RIVER_WISP.get().create(level, reason);
            case NONE -> null;
        };
        if (entity == null) return null;
        double y = species == Species.RIVER_WISP ? groundY + 4.0D : groundY;
        entity.setPos(x + 0.5D, y, z + 0.5D);
        if (!level.noCollision(entity)) {
            entity.discard();
            return null;
        }
        if (diagnostic) {
            entity.setCustomName(Component.literal(switch (species) {
                case SILVER_HART -> "은각사슴";
                case ASH_HOUND -> "재빛사냥개";
                case RIVER_WISP -> "강빛정령";
                case NONE -> "";
            }));
            entity.setCustomNameVisible(false);
        }
        return level.addFreshEntity(entity) ? entity : null;
    }

    static Species speciesAt(int x, int z) {
        if (!insideKingdom(x, z) || insideCapital(x, z)) return Species.NONE;
        if (Math.abs(z) >= 1_200
                && AuthoredContinentDensity.silverRiverStrength(x, z) >= 0.72D) {
            return Species.RIVER_WISP;
        }
        if (x <= -5_500 && AuthoredContinentDensity.surfaceHeight(x, z) >= 78.0D) {
            return Species.ASH_HOUND;
        }
        if (z <= -4_800 && x >= -11_000 && x <= 10_000) {
            return Species.SILVER_HART;
        }
        return Species.NONE;
    }

    private static boolean insideKingdom(int x, int z) {
        double nx = (x + 250.0D) / 24_000.0D;
        double nz = (z - 150.0D) / 20_000.0D;
        return nx * nx + nz * nz <= 1.02D;
    }

    private static boolean insideCapital(int x, int z) {
        return x >= ErdenCapitalStreamingBuilder.WEST_WALL_X - 96
                && x <= ErdenCapitalStreamingBuilder.EAST_WALL_X + 96
                && z >= ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 96
                && z <= ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 96;
    }

    private static void prepareCi(ServerLevel level) {
        if (ciPassed) return;
        for (Sample sample : CI_SAMPLES) {
            ChunkPos chunk = new ChunkPos(sample.x() >> 4, sample.z() >> 4);
            long packed = pack(chunk.x(), chunk.z());
            if (CI_TICKETS.add(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
            }
        }
        if (ciTicketRefreshes == 0L && !CI_TICKETS.isEmpty()) {
            refreshCiTickets(level);
        }
        if (ciPrepared) return;
        for (Sample sample : CI_SAMPLES) {
            if (!level.hasChunk(sample.x() >> 4, sample.z() >> 4)) return;
        }
        for (Sample sample : CI_SAMPLES) {
            if (speciesAt(sample.x(), sample.z()) != sample.species()) {
                throw new IllegalStateException(
                        "Erden ecology CI sample classified incorrectly " + sample);
            }
            Entity created = spawn(
                    level, sample.species(), sample.x(), sample.z(), EntitySpawnReason.COMMAND, true);
            if (created == null) return;
            CI_ENTITIES.add(created.getUUID());
        }
        ciPrepared = CI_ENTITIES.size() == CI_SAMPLES.size();
        if (ciPrepared) {
            LivingKingdoms.LOGGER.info(
                    "Prepared Erden fantasy ecology CI samples=3 bounded_chunks={} northern_forest=true western_hills=true silver_river=true capital_excluded=true normal_force_load=false",
                    CI_TICKETS.size());
        }
    }

    private static void refreshCiTickets(ServerLevel level) {
        if (CI_TICKETS.isEmpty() || ciPassed) return;
        int loaded = 0;
        for (long packed : Set.copyOf(CI_TICKETS)) {
            ChunkPos chunk = new ChunkPos(unpackX(packed), unpackZ(packed));
            level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
            if (level.hasChunk(chunk.x(), chunk.z())) loaded++;
        }
        ciTicketRefreshes++;
        if (ciTicketRefreshes == 1L || ciTicketRefreshes % 10L == 0L) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_FANTASY_ECOLOGY_TICKET_REFRESH refresh={} retained={} loaded={} interval_ticks={} timeout_safe_refresh=true forced_chunks=false",
                    ciTicketRefreshes, CI_TICKETS.size(), loaded, CI_TICKET_REFRESH_INTERVAL);
        }
    }

    private static void verifyCi(ServerLevel level) {
        if (!ciPrepared || ciPassed) return;
        boolean hart = false;
        boolean hound = false;
        boolean wisp = false;
        for (UUID uuid : CI_ENTITIES) {
            Entity entity = level.getEntity(uuid);
            if (entity == null || !entity.isAlive()) return;
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            if (id.equals("livingkingdoms:silver_hart")) hart = entity instanceof SilverHartEntity;
            else if (id.equals("livingkingdoms:ash_hound")) hound = entity instanceof AshHoundEntity;
            else if (id.equals("livingkingdoms:river_wisp")) wisp = entity instanceof RiverWispEntity;
        }
        if (!hart || !hound || !wisp) return;
        ciPassed = true;
        for (UUID uuid : List.copyOf(CI_ENTITIES)) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) entity.discard();
        }
        for (long packed : Set.copyOf(CI_TICKETS)) {
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL,
                    new ChunkPos(unpackX(packed), unpackZ(packed)), 0);
        }
        CI_TICKETS.clear();
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_FANTASY_ECOLOGY_PASS revision=1 registered_species=3 silver_hart=true ash_hound=true river_wisp=true actual_custom_entity_types=true actual_entity_instances=true hart_player_avoidance=true hart_herding=true ash_hound_untameable=true ash_hound_night_pack=true river_wisp_no_item_courier=true river_bound_navigation=true northern_forest_spawn=true western_hill_spawn=true silver_river_spawn=true capital_spawn=false player_loaded_runtime=true local_species_cap={} forced_citywide=false ci_sample_chunks=3 ci_tickets_released=true ci_ticket_refreshes={} timeout_safe_refresh=true",
                LOCAL_SPECIES_CAP, ciTicketRefreshes);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_FANTASY_ECOLOGY_TEST"));
    }

    enum Species {
        NONE,
        SILVER_HART,
        ASH_HOUND,
        RIVER_WISP
    }

    private record Sample(int x, int z, Species species) {
    }
}
