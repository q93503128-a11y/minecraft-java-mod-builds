package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Persistent, bounded incident ledger for Erden's loaded-city fire brigade. */
public final class ErdenFireResponseSavedData extends SavedData {
    public record Incident(
            long id,
            int fireX,
            int fireY,
            int fireZ,
            int cisternX,
            int cisternY,
            int cisternZ,
            String responderName,
            String stage,
            long detectedTick,
            long stageTick) {
        private static final Codec<Incident> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("id").forGetter(Incident::id),
                Codec.INT.fieldOf("fire_x").forGetter(Incident::fireX),
                Codec.INT.fieldOf("fire_y").forGetter(Incident::fireY),
                Codec.INT.fieldOf("fire_z").forGetter(Incident::fireZ),
                Codec.INT.fieldOf("cistern_x").forGetter(Incident::cisternX),
                Codec.INT.fieldOf("cistern_y").forGetter(Incident::cisternY),
                Codec.INT.fieldOf("cistern_z").forGetter(Incident::cisternZ),
                Codec.STRING.optionalFieldOf("responder_name", "")
                        .forGetter(Incident::responderName),
                Codec.STRING.fieldOf("stage").forGetter(Incident::stage),
                Codec.LONG.fieldOf("detected_tick").forGetter(Incident::detectedTick),
                Codec.LONG.fieldOf("stage_tick").forGetter(Incident::stageTick)
        ).apply(instance, Incident::new));

        public Incident withResponder(String name, String nextStage, long tick) {
            return new Incident(id, fireX, fireY, fireZ,
                    cisternX, cisternY, cisternZ,
                    name, nextStage, detectedTick, tick);
        }

        public Incident withStage(String nextStage, long tick) {
            return new Incident(id, fireX, fireY, fireZ,
                    cisternX, cisternY, cisternZ,
                    responderName, nextStage, detectedTick, tick);
        }

        public Incident requeue(long tick) {
            return new Incident(id, fireX, fireY, fireZ,
                    cisternX, cisternY, cisternZ,
                    "", "reported", detectedTick, tick);
        }
    }

    private static final Codec<ErdenFireResponseSavedData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.optionalFieldOf("next_id", 1L)
                            .forGetter(data -> data.nextId),
                    Codec.INT.optionalFieldOf("detected_count", 0)
                            .forGetter(data -> data.detectedCount),
                    Codec.INT.optionalFieldOf("dispatched_count", 0)
                            .forGetter(data -> data.dispatchedCount),
                    Codec.INT.optionalFieldOf("extinguished_count", 0)
                            .forGetter(data -> data.extinguishedCount),
                    Incident.CODEC.listOf().optionalFieldOf("incidents", List.of())
                            .forGetter(data -> List.copyOf(data.incidents))
            ).apply(instance, ErdenFireResponseSavedData::new));

    public static final SavedDataType<ErdenFireResponseSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_fire_response"),
            level -> new ErdenFireResponseSavedData(),
            level -> CODEC
    );

    private long nextId;
    private int detectedCount;
    private int dispatchedCount;
    private int extinguishedCount;
    private final List<Incident> incidents;

    public ErdenFireResponseSavedData() {
        this(1L, 0, 0, 0, List.of());
    }

    private ErdenFireResponseSavedData(
            long nextId,
            int detectedCount,
            int dispatchedCount,
            int extinguishedCount,
            List<Incident> incidents) {
        this.nextId = Math.max(1L, nextId);
        this.detectedCount = Math.max(0, detectedCount);
        this.dispatchedCount = Math.max(0, dispatchedCount);
        this.extinguishedCount = Math.max(0, extinguishedCount);
        this.incidents = new ArrayList<>(incidents);
    }

    public List<Incident> incidents() {
        return List.copyOf(incidents);
    }

    public int activeCount() {
        return incidents.size();
    }

    public int detectedCount() {
        return detectedCount;
    }

    public int dispatchedCount() {
        return dispatchedCount;
    }

    public int extinguishedCount() {
        return extinguishedCount;
    }

    public boolean hasIncidentAt(int x, int y, int z) {
        for (Incident incident : incidents) {
            if (incident.fireX() == x && incident.fireY() == y && incident.fireZ() == z) return true;
        }
        return false;
    }

    public Incident report(
            int fireX,
            int fireY,
            int fireZ,
            ErdenUrbanInfrastructureBuilder.FireCistern cistern,
            long tick) {
        Incident incident = new Incident(
                nextId++, fireX, fireY, fireZ,
                cistern.x(), cistern.y(), cistern.z(),
                "", "reported", tick, tick);
        incidents.add(incident);
        detectedCount++;
        setDirty();
        return incident;
    }

    public void assign(long id, String responderName, long tick) {
        replace(id, incident -> incident.withResponder(responderName, "to_cistern", tick));
        dispatchedCount++;
        setDirty();
    }

    public void advance(long id, String stage, long tick) {
        replace(id, incident -> incident.withStage(stage, tick));
    }

    public void requeue(long id, long tick) {
        replace(id, incident -> incident.requeue(tick));
    }

    public void resolve(long id, boolean extinguished) {
        boolean removed = incidents.removeIf(incident -> incident.id() == id);
        if (!removed) return;
        if (extinguished) extinguishedCount++;
        setDirty();
    }

    private void replace(long id, java.util.function.UnaryOperator<Incident> updater) {
        for (int index = 0; index < incidents.size(); index++) {
            Incident incident = incidents.get(index);
            if (incident.id() != id) continue;
            Incident updated = updater.apply(incident);
            if (!updated.equals(incident)) {
                incidents.set(index, updated);
                setDirty();
            }
            return;
        }
    }
}
