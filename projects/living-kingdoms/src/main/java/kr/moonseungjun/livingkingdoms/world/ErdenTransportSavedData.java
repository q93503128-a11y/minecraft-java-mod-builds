package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Persistent road manifests, authoritative in-transit cargo and courier progress for Erden. */
public final class ErdenTransportSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 2;

    public record RoutePoint(int x, int z) {
        private static final Codec<RoutePoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(RoutePoint::x),
                Codec.INT.fieldOf("z").forGetter(RoutePoint::z)
        ).apply(instance, RoutePoint::new));
    }

    public record DeliveryJob(
            String id,
            String sourceId,
            String targetId,
            String resource,
            long amount,
            long createdTick,
            long phaseTick,
            String status,
            List<RoutePoint> route,
            int waypointIndex,
            int attempts,
            boolean cart,
            String porterUuid,
            String cartUuid,
            long travelTicks,
            boolean authoritative) {
        private static final Codec<DeliveryJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(DeliveryJob::id),
                Codec.STRING.fieldOf("source_id").forGetter(DeliveryJob::sourceId),
                Codec.STRING.fieldOf("target_id").forGetter(DeliveryJob::targetId),
                Codec.STRING.fieldOf("resource").forGetter(DeliveryJob::resource),
                Codec.LONG.fieldOf("amount").forGetter(DeliveryJob::amount),
                Codec.LONG.fieldOf("created_tick").forGetter(DeliveryJob::createdTick),
                Codec.LONG.fieldOf("phase_tick").forGetter(DeliveryJob::phaseTick),
                Codec.STRING.fieldOf("status").forGetter(DeliveryJob::status),
                RoutePoint.CODEC.listOf().optionalFieldOf("route", List.of()).forGetter(DeliveryJob::route),
                Codec.INT.optionalFieldOf("waypoint_index", 0).forGetter(DeliveryJob::waypointIndex),
                Codec.INT.optionalFieldOf("attempts", 0).forGetter(DeliveryJob::attempts),
                Codec.BOOL.optionalFieldOf("cart", false).forGetter(DeliveryJob::cart),
                Codec.STRING.optionalFieldOf("porter_uuid", "").forGetter(DeliveryJob::porterUuid),
                Codec.STRING.optionalFieldOf("cart_uuid", "").forGetter(DeliveryJob::cartUuid),
                Codec.LONG.optionalFieldOf("travel_ticks", 0L).forGetter(DeliveryJob::travelTicks),
                Codec.BOOL.optionalFieldOf("authoritative", false).forGetter(DeliveryJob::authoritative)
        ).apply(instance, DeliveryJob::new));

        public DeliveryJob(
                String id,
                String sourceId,
                String targetId,
                String resource,
                long amount,
                long createdTick,
                long phaseTick,
                String status,
                List<RoutePoint> route,
                int waypointIndex,
                int attempts,
                boolean cart,
                String porterUuid,
                String cartUuid,
                long travelTicks) {
            this(id, sourceId, targetId, resource, amount, createdTick, phaseTick, status,
                    route, waypointIndex, attempts, cart, porterUuid, cartUuid, travelTicks, false);
        }

        public DeliveryJob {
            amount = Math.max(1L, amount);
            route = List.copyOf(route);
            waypointIndex = Math.max(0, waypointIndex);
            attempts = Math.max(0, attempts);
            travelTicks = Math.max(1L, travelTicks);
            porterUuid = porterUuid == null ? "" : porterUuid;
            cartUuid = cartUuid == null ? "" : cartUuid;
        }

        public DeliveryJob withStatus(String nextStatus, long tick) {
            return new DeliveryJob(
                    id, sourceId, targetId, resource, amount,
                    createdTick, tick, nextStatus, route, waypointIndex,
                    attempts, cart, porterUuid, cartUuid, travelTicks, authoritative);
        }

        public DeliveryJob withWaypoint(int nextIndex, long extraTravelTicks) {
            return new DeliveryJob(
                    id, sourceId, targetId, resource, amount,
                    createdTick, phaseTick, status, route, nextIndex,
                    attempts, cart, porterUuid, cartUuid,
                    travelTicks + Math.max(0L, extraTravelTicks), authoritative);
        }

        public DeliveryJob withWaypoint(int nextIndex) {
            return new DeliveryJob(
                    id, sourceId, targetId, resource, amount,
                    createdTick, phaseTick, status, route, nextIndex,
                    attempts, cart, porterUuid, cartUuid, travelTicks, authoritative);
        }

        public DeliveryJob withAttemptAndRoute(
                int nextAttempts,
                List<RoutePoint> nextRoute,
                int nextIndex,
                long tick) {
            return withAttemptAndRoute(nextAttempts, nextRoute, nextIndex, tick, travelTicks);
        }

        public DeliveryJob withAttemptAndRoute(
                int nextAttempts,
                List<RoutePoint> nextRoute,
                int nextIndex,
                long tick,
                long nextTravelTicks) {
            return new DeliveryJob(
                    id, sourceId, targetId, resource, amount,
                    createdTick, tick, "moving", nextRoute, nextIndex,
                    nextAttempts, cart, porterUuid, cartUuid,
                    Math.max(travelTicks, nextTravelTicks), authoritative);
        }

        public DeliveryJob withEntities(String nextPorterUuid, String nextCartUuid) {
            return new DeliveryJob(
                    id, sourceId, targetId, resource, amount,
                    createdTick, phaseTick, status, route, waypointIndex,
                    attempts, cart, nextPorterUuid, nextCartUuid, travelTicks, authoritative);
        }

        public DeliveryJob withoutEntities() {
            return withEntities("", "");
        }

        public boolean terminal() {
            return status.equals("completed") || status.equals("failed")
                    || status.equals("settled") || status.equals("returned")
                    || status.equals("settlement_failed") || status.equals("return_failed");
        }

        public long dueTick() {
            return createdTick + travelTicks;
        }
    }

    private static final Codec<ErdenTransportSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION).forGetter(data -> data.schemaVersion),
            Codec.LONG.optionalFieldOf("last_manifest_day", -1L).forGetter(data -> data.lastManifestDay),
            Codec.LONG.optionalFieldOf("next_sequence", 1L).forGetter(data -> data.nextSequence),
            DeliveryJob.CODEC.listOf().optionalFieldOf("jobs", List.of()).forGetter(data -> List.copyOf(data.jobs)),
            Codec.LONG.optionalFieldOf("total_manifests", 0L).forGetter(data -> data.totalManifests),
            Codec.LONG.optionalFieldOf("total_physicalized", 0L).forGetter(data -> data.totalPhysicalized),
            Codec.LONG.optionalFieldOf("total_completed", 0L).forGetter(data -> data.totalCompleted),
            Codec.LONG.optionalFieldOf("total_blocked", 0L).forGetter(data -> data.totalBlocked),
            Codec.LONG.optionalFieldOf("total_failed", 0L).forGetter(data -> data.totalFailed),
            Codec.LONG.optionalFieldOf("modeled_travel_ticks", 0L).forGetter(data -> data.modeledTravelTicks),
            Codec.LONG.optionalFieldOf("total_escrowed", 0L).forGetter(data -> data.totalEscrowed),
            Codec.LONG.optionalFieldOf("total_returned", 0L).forGetter(data -> data.totalReturned)
    ).apply(instance, ErdenTransportSavedData::new));

    public static final SavedDataType<ErdenTransportSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_transport"),
            level -> new ErdenTransportSavedData(),
            level -> CODEC
    );

    private int schemaVersion;
    private long lastManifestDay;
    private long nextSequence;
    private final List<DeliveryJob> jobs;
    private long totalManifests;
    private long totalPhysicalized;
    private long totalCompleted;
    private long totalBlocked;
    private long totalFailed;
    private long modeledTravelTicks;
    private long totalEscrowed;
    private long totalReturned;

    public ErdenTransportSavedData() {
        this(SCHEMA_VERSION, -1L, 1L, List.of(), 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private ErdenTransportSavedData(
            int schemaVersion,
            long lastManifestDay,
            long nextSequence,
            List<DeliveryJob> jobs,
            long totalManifests,
            long totalPhysicalized,
            long totalCompleted,
            long totalBlocked,
            long totalFailed,
            long modeledTravelTicks,
            long totalEscrowed,
            long totalReturned) {
        this.schemaVersion = Math.max(1, schemaVersion);
        this.lastManifestDay = lastManifestDay;
        this.nextSequence = Math.max(1L, nextSequence);
        this.jobs = new ArrayList<>(jobs);
        this.totalManifests = Math.max(0L, totalManifests);
        this.totalPhysicalized = Math.max(0L, totalPhysicalized);
        this.totalCompleted = Math.max(0L, totalCompleted);
        this.totalBlocked = Math.max(0L, totalBlocked);
        this.totalFailed = Math.max(0L, totalFailed);
        this.modeledTravelTicks = Math.max(0L, modeledTravelTicks);
        this.totalEscrowed = Math.max(0L, totalEscrowed);
        this.totalReturned = Math.max(0L, totalReturned);
    }

    public long lastManifestDay() { return lastManifestDay; }
    public List<DeliveryJob> jobs() { return List.copyOf(jobs); }
    public long totalManifests() { return totalManifests; }
    public long totalPhysicalized() { return totalPhysicalized; }
    public long totalCompleted() { return totalCompleted; }
    public long totalBlocked() { return totalBlocked; }
    public long totalFailed() { return totalFailed; }
    public long modeledTravelTicks() { return modeledTravelTicks; }
    public long totalEscrowed() { return totalEscrowed; }
    public long totalReturned() { return totalReturned; }

    public String nextJobId(long day) {
        String id = "erden_delivery_%d_%06d".formatted(day, nextSequence++);
        setDirty();
        return id;
    }

    public void recordManifest(long day, long travelTicks) {
        lastManifestDay = Math.max(lastManifestDay, day);
        totalManifests++;
        modeledTravelTicks += Math.max(0L, travelTicks);
        setDirty();
    }

    /** Backward-compatible aggregate import for phase-one saves. */
    public void beginManifestDay(long day, long manifestCount, long travelTicks) {
        if (day <= lastManifestDay) return;
        lastManifestDay = day;
        totalManifests += Math.max(0L, manifestCount);
        modeledTravelTicks += Math.max(0L, travelTicks);
        setDirty();
    }

    public void addJob(DeliveryJob job) {
        jobs.add(job);
        totalPhysicalized++;
        if (job.authoritative()) totalEscrowed += job.amount();
        setDirty();
    }

    public void replaceJob(DeliveryJob replacement) {
        for (int index = 0; index < jobs.size(); index++) {
            if (!jobs.get(index).id().equals(replacement.id())) continue;
            if (!jobs.get(index).equals(replacement)) {
                jobs.set(index, replacement);
                setDirty();
            }
            return;
        }
    }

    public void markCompleted(DeliveryJob replacement) {
        replaceJob(replacement);
        totalCompleted++;
        setDirty();
    }

    public void markBlocked() {
        totalBlocked++;
        setDirty();
    }

    public void markFailed(DeliveryJob replacement) {
        markFailed(replacement, false);
    }

    public void markFailed(DeliveryJob replacement, boolean returned) {
        replaceJob(replacement);
        totalFailed++;
        if (returned && replacement.authoritative()) totalReturned += replacement.amount();
        setDirty();
    }

    /** Records economic return after a physical authoritative job already failed in the road runtime. */
    public void recordAuthoritativeReturn(long amount) {
        totalReturned += Math.max(0L, amount);
        setDirty();
    }

    public void pruneTerminalJobs(long oldestTick) {
        if (jobs.removeIf(job -> job.terminal() && job.phaseTick() < oldestTick)) setDirty();
    }

    public int activeJobCount() {
        int count = 0;
        for (DeliveryJob job : jobs) if (!job.terminal()) count++;
        return count;
    }
}
