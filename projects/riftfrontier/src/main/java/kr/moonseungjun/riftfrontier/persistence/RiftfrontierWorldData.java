package kr.moonseungjun.riftfrontier.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionRun;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionRunCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Authoritative world-scoped Riftfrontier root state.
 *
 * The instance is always resolved from the overworld data storage so dimensions do not silently fork
 * expedition counters or world-response state. Codec decode passes through the explicit migration
 * registry before the mutable SavedData instance becomes visible to gameplay code.
 */
public final class RiftfrontierWorldData extends SavedData {
    public static final Identifier STORAGE_ID = Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "world_state");

    private static final Codec<RiftfrontierWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf(PersistenceSchema.VERSION_KEY, 0).forGetter(data -> PersistenceSchema.CURRENT),
        Codec.LONG.optionalFieldOf("world_revision", 0L).forGetter(RiftfrontierWorldData::worldRevision),
        Codec.LONG.optionalFieldOf("expedition_sequence", 0L).forGetter(RiftfrontierWorldData::expeditionSequence),
        Codec.STRING.optionalFieldOf("content_fingerprint", "").forGetter(RiftfrontierWorldData::contentFingerprint),
        ExpeditionRunCodec.CODEC.listOf().optionalFieldOf("expeditions", List.of()).forGetter(RiftfrontierWorldData::expeditions)
    ).apply(instance, RiftfrontierWorldData::decode));

    public static final SavedDataType<RiftfrontierWorldData> TYPE = new SavedDataType<>(
        STORAGE_ID,
        RiftfrontierWorldData::new,
        CODEC,
        null
    );

    private long worldRevision;
    private long expeditionSequence;
    private String contentFingerprint;
    private final List<ExpeditionRun> expeditions;

    private RiftfrontierWorldData() {
        this(0L, 0L, "", List.of());
    }

    private RiftfrontierWorldData(long worldRevision, long expeditionSequence, String contentFingerprint, List<ExpeditionRun> expeditions) {
        if (worldRevision < 0L) throw new IllegalArgumentException("worldRevision must be >= 0");
        if (expeditionSequence < 0L) throw new IllegalArgumentException("expeditionSequence must be >= 0");
        this.worldRevision = worldRevision;
        this.expeditionSequence = expeditionSequence;
        this.contentFingerprint = Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        this.expeditions = new ArrayList<>(Objects.requireNonNull(expeditions, "expeditions"));
        validateExpeditionSequences(this.expeditions, expeditionSequence);
    }

    private static RiftfrontierWorldData decode(
        int sourceSchema,
        long worldRevision,
        long expeditionSequence,
        String contentFingerprint,
        List<ExpeditionRun> expeditions
    ) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put(PersistenceSchema.VERSION_KEY, sourceSchema);
        raw.put("world_revision", worldRevision);
        raw.put("expedition_sequence", expeditionSequence);
        raw.put("content_fingerprint", contentFingerprint);
        raw.put("expeditions", expeditions);

        Map<String, Object> migrated = PersistenceMigrationRegistry.defaults().migrate(sourceSchema, raw);
        @SuppressWarnings("unchecked")
        List<ExpeditionRun> migratedExpeditions = migrated.get("expeditions") instanceof List<?> list
            ? (List<ExpeditionRun>) list
            : List.of();
        return new RiftfrontierWorldData(
            longValue(migrated, "world_revision"),
            longValue(migrated, "expedition_sequence"),
            String.valueOf(migrated.getOrDefault("content_fingerprint", "")),
            migratedExpeditions
        );
    }

    private static long longValue(Map<String, Object> values, String key) {
        Object value = values.getOrDefault(key, 0L);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Persistence field '" + key + "' must be numeric");
        }
        return number.longValue();
    }

    private static void validateExpeditionSequences(List<ExpeditionRun> runs, long highestAllocated) {
        long previous = 0L;
        for (ExpeditionRun run : runs.stream().sorted((a, b) -> Long.compare(a.sequence(), b.sequence())).toList()) {
            if (run.sequence() <= previous) throw new IllegalStateException("Duplicate or unordered expedition sequence " + run.sequence());
            if (run.sequence() > highestAllocated) throw new IllegalStateException("Persisted expedition sequence exceeds allocation counter: " + run.sequence());
            previous = run.sequence();
        }
    }

    /** Always returns the single authoritative world root, even when called from another dimension. */
    public static RiftfrontierWorldData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public long worldRevision() {
        return worldRevision;
    }

    public long expeditionSequence() {
        return expeditionSequence;
    }

    public String contentFingerprint() {
        return contentFingerprint;
    }

    public List<ExpeditionRun> expeditions() {
        return List.copyOf(expeditions);
    }

    public Optional<ExpeditionRun> expedition(long sequence) {
        return expeditions.stream().filter(run -> run.sequence() == sequence).findFirst();
    }

    /** Allocates the next stable expedition sequence and records the content snapshot that authored it. */
    public long allocateExpeditionSequence(String activeContentFingerprint) {
        expeditionSequence++;
        worldRevision++;
        contentFingerprint = Objects.requireNonNull(activeContentFingerprint, "activeContentFingerprint");
        setDirty();
        return expeditionSequence;
    }

    /** Creates and persists a PREPARING run. Domain validation must happen before calling this mutation. */
    public ExpeditionRun createExpedition(ContentId regionId, ContentId contractId, String activeContentFingerprint, long gameTime) {
        long sequence = allocateExpeditionSequence(activeContentFingerprint);
        ExpeditionRun run = ExpeditionRun.preparing(sequence, regionId, contractId, activeContentFingerprint, gameTime);
        expeditions.add(run);
        setDirty();
        return run;
    }

    /** Replaces one authoritative run after an ExpeditionLifecycle transition. */
    public ExpeditionRun updateExpedition(ExpeditionRun updated) {
        Objects.requireNonNull(updated, "updated");
        for (int index = 0; index < expeditions.size(); index++) {
            ExpeditionRun existing = expeditions.get(index);
            if (existing.sequence() != updated.sequence()) continue;
            if (!existing.regionId().equals(updated.regionId()) || !existing.contractId().equals(updated.contractId())) {
                throw new IllegalArgumentException("Expedition identity cannot change after allocation");
            }
            expeditions.set(index, updated);
            worldRevision++;
            setDirty();
            return updated;
        }
        throw new IllegalArgumentException("Unknown expedition sequence " + updated.sequence());
    }

    /**
     * Records the active validated content fingerprint without creating revision noise when it is unchanged.
     * This gives diagnostics and future migrations a durable content/world compatibility breadcrumb.
     */
    public boolean synchronizeContentFingerprint(String activeContentFingerprint) {
        Objects.requireNonNull(activeContentFingerprint, "activeContentFingerprint");
        if (contentFingerprint.equals(activeContentFingerprint)) return false;
        contentFingerprint = activeContentFingerprint;
        worldRevision++;
        setDirty();
        return true;
    }

    public String diagnosticSummary() {
        long active = expeditions.stream().filter(run -> !run.status().terminal()).count();
        return "schema=" + PersistenceSchema.CURRENT
            + ", worldRevision=" + worldRevision
            + ", expeditionSequence=" + expeditionSequence
            + ", expeditions=" + expeditions.size()
            + ", activeExpeditions=" + active
            + ", contentFingerprint=" + (contentFingerprint.isBlank() ? "<unset>" : contentFingerprint);
    }
}
