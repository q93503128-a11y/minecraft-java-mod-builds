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
import java.util.UUID;

/**
 * Authoritative world-scoped Riftfrontier root state.
 *
 * The instance is always resolved from the overworld data storage so dimensions do not silently fork
 * expedition counters or world-response state. Codec decode passes through the explicit migration
 * registry before the mutable SavedData instance becomes visible to gameplay code.
 */
public final class RiftfrontierWorldData extends SavedData {
    public static final Identifier STORAGE_ID = Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "world_state");

    private static final int FRESH_WORLD_SUPPLY = 2;

    private static final Codec<RiftfrontierWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf(PersistenceSchema.VERSION_KEY, 0).forGetter(data -> PersistenceSchema.CURRENT),
        Codec.LONG.optionalFieldOf("world_revision", 0L).forGetter(RiftfrontierWorldData::worldRevision),
        Codec.LONG.optionalFieldOf("expedition_sequence", 0L).forGetter(RiftfrontierWorldData::expeditionSequence),
        Codec.STRING.optionalFieldOf("content_fingerprint", "").forGetter(RiftfrontierWorldData::contentFingerprint),
        ExpeditionRunCodec.CODEC.listOf().optionalFieldOf("expeditions", List.of()).forGetter(RiftfrontierWorldData::expeditions),
        Codec.INT.optionalFieldOf("secured_region_01_salvage", 0).forGetter(RiftfrontierWorldData::securedRegion01Salvage),
        Codec.INT.optionalFieldOf("expedition_supply", FRESH_WORLD_SUPPLY).forGetter(RiftfrontierWorldData::expeditionSupply),
        Codec.INT.optionalFieldOf("region_01_pressure", 0).forGetter(RiftfrontierWorldData::region01Pressure)
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
    private int securedRegion01Salvage;
    private int expeditionSupply;
    private int region01Pressure;

    private RiftfrontierWorldData() {
        this(0L, 0L, "", List.of(), 0, FRESH_WORLD_SUPPLY, 0);
    }

    private RiftfrontierWorldData(
        long worldRevision,
        long expeditionSequence,
        String contentFingerprint,
        List<ExpeditionRun> expeditions,
        int securedRegion01Salvage,
        int expeditionSupply,
        int region01Pressure
    ) {
        if (worldRevision < 0L) throw new IllegalArgumentException("worldRevision must be >= 0");
        if (expeditionSequence < 0L) throw new IllegalArgumentException("expeditionSequence must be >= 0");
        if (securedRegion01Salvage < 0) throw new IllegalArgumentException("securedRegion01Salvage must be >= 0");
        if (expeditionSupply < 0) throw new IllegalArgumentException("expeditionSupply must be >= 0");
        if (region01Pressure < 0) throw new IllegalArgumentException("region01Pressure must be >= 0");
        this.worldRevision = worldRevision;
        this.expeditionSequence = expeditionSequence;
        this.contentFingerprint = Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        this.expeditions = new ArrayList<>(Objects.requireNonNull(expeditions, "expeditions"));
        this.securedRegion01Salvage = securedRegion01Salvage;
        this.expeditionSupply = expeditionSupply;
        this.region01Pressure = region01Pressure;
        validateExpeditionSequences(this.expeditions, expeditionSequence);
    }

    private static RiftfrontierWorldData decode(
        int sourceSchema,
        long worldRevision,
        long expeditionSequence,
        String contentFingerprint,
        List<ExpeditionRun> expeditions,
        int securedRegion01Salvage,
        int expeditionSupply,
        int region01Pressure
    ) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put(PersistenceSchema.VERSION_KEY, sourceSchema);
        raw.put("world_revision", worldRevision);
        raw.put("expedition_sequence", expeditionSequence);
        raw.put("content_fingerprint", contentFingerprint);
        raw.put("expeditions", expeditions);
        raw.put("secured_region_01_salvage", securedRegion01Salvage);
        raw.put("expedition_supply", expeditionSupply);
        raw.put("region_01_pressure", region01Pressure);

        Map<String, Object> migrated = PersistenceMigrationRegistry.defaults().migrate(sourceSchema, raw);
        @SuppressWarnings("unchecked")
        List<ExpeditionRun> migratedExpeditions = migrated.get("expeditions") instanceof List<?> list
            ? (List<ExpeditionRun>) list
            : List.of();
        return new RiftfrontierWorldData(
            longValue(migrated, "world_revision"),
            longValue(migrated, "expedition_sequence"),
            String.valueOf(migrated.getOrDefault("content_fingerprint", "")),
            migratedExpeditions,
            intValue(migrated, "secured_region_01_salvage", 0),
            intValue(migrated, "expedition_supply", FRESH_WORLD_SUPPLY),
            intValue(migrated, "region_01_pressure", 0)
        );
    }

    private static long longValue(Map<String, Object> values, String key) {
        Object value = values.getOrDefault(key, 0L);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Persistence field '" + key + "' must be numeric");
        }
        return number.longValue();
    }

    private static int intValue(Map<String, Object> values, String key, int fallback) {
        Object value = values.getOrDefault(key, fallback);
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Persistence field '" + key + "' must be numeric");
        }
        return number.intValue();
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

    public long worldRevision() { return worldRevision; }
    public long expeditionSequence() { return expeditionSequence; }
    public String contentFingerprint() { return contentFingerprint; }
    public List<ExpeditionRun> expeditions() { return List.copyOf(expeditions); }
    public int securedRegion01Salvage() { return securedRegion01Salvage; }
    public int expeditionSupply() { return expeditionSupply; }
    public int region01Pressure() { return region01Pressure; }

    public Optional<ExpeditionRun> expedition(long sequence) {
        return expeditions.stream().filter(run -> run.sequence() == sequence).findFirst();
    }

    /** World response: repeated successful Region 01 extraction gradually raises preparation pressure. */
    public int region01PreparationSupplyCost() {
        return Math.min(3, 1 + (region01Pressure / 2));
    }

    public boolean consumeRegion01PreparationSupply() {
        int cost = region01PreparationSupplyCost();
        if (expeditionSupply < cost) return false;
        expeditionSupply -= cost;
        worldRevision++;
        setDirty();
        return true;
    }

    /** Settles retained Region 01 salvage into hub storage and advances visible regional pressure. */
    public void settleRegion01Extraction(int retainedSalvage) {
        if (retainedSalvage < 0) throw new IllegalArgumentException("retainedSalvage must be >= 0");
        securedRegion01Salvage = Math.addExact(securedRegion01Salvage, retainedSalvage);
        region01Pressure = Math.addExact(region01Pressure, 1);
        worldRevision++;
        setDirty();
    }

    /** Converts secured salvage into expedition supply without inventing a separate production subsystem yet. */
    public void provisionRegion01Supply() {
        if (securedRegion01Salvage < 1) throw new IllegalStateException("At least 1 secured Region 01 salvage is required to provision supplies");
        securedRegion01Salvage -= 1;
        expeditionSupply = Math.addExact(expeditionSupply, 2);
        worldRevision++;
        setDirty();
    }

    /** Allocates the next stable expedition sequence and records the content snapshot that authored it. */
    public long allocateExpeditionSequence(String activeContentFingerprint) {
        expeditionSequence++;
        worldRevision++;
        contentFingerprint = Objects.requireNonNull(activeContentFingerprint, "activeContentFingerprint");
        setDirty();
        return expeditionSequence;
    }

    /** Legacy/test-fixture allocation with no participant ownership. */
    public ExpeditionRun createExpedition(ContentId regionId, ContentId contractId, String activeContentFingerprint, long gameTime) {
        long sequence = allocateExpeditionSequence(activeContentFingerprint);
        ExpeditionRun run = ExpeditionRun.preparing(sequence, regionId, contractId, activeContentFingerprint, gameTime);
        expeditions.add(run);
        setDirty();
        return run;
    }

    /** Creates and persists a PREPARING run bound to the authoritative participant UUID. */
    public ExpeditionRun createExpedition(ContentId regionId, ContentId contractId, UUID ownerId, String activeContentFingerprint, long gameTime) {
        long sequence = allocateExpeditionSequence(activeContentFingerprint);
        ExpeditionRun run = ExpeditionRun.preparing(sequence, regionId, contractId, ownerId, activeContentFingerprint, gameTime);
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
            if (!existing.ownerId().equals(updated.ownerId())) {
                throw new IllegalArgumentException("Expedition owner cannot change after allocation");
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
            + ", securedRegion01Salvage=" + securedRegion01Salvage
            + ", expeditionSupply=" + expeditionSupply
            + ", region01Pressure=" + region01Pressure
            + ", nextRegion01SupplyCost=" + region01PreparationSupplyCost()
            + ", contentFingerprint=" + (contentFingerprint.isBlank() ? "<unset>" : contentFingerprint);
    }
}
