package kr.moonseungjun.livingkingdoms.crime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Persistent civic case ledger for Erden's witness, warrant, arrest and trial flow. */
public final class ErdenJusticeSavedData extends SavedData {
    public record CaseRecord(
            long id,
            String suspectId,
            String offense,
            int severity,
            int incidentX,
            int incidentY,
            int incidentZ,
            String witnessName,
            String guardName,
            String courtRole,
            int courtX,
            int courtZ,
            String stage,
            long createdTick,
            long stageTick,
            int holdTicks) {
        private static final Codec<CaseRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("id").forGetter(CaseRecord::id),
                Codec.STRING.fieldOf("suspect_id").forGetter(CaseRecord::suspectId),
                Codec.STRING.fieldOf("offense").forGetter(CaseRecord::offense),
                Codec.INT.fieldOf("severity").forGetter(CaseRecord::severity),
                Codec.INT.fieldOf("incident_x").forGetter(CaseRecord::incidentX),
                Codec.INT.fieldOf("incident_y").forGetter(CaseRecord::incidentY),
                Codec.INT.fieldOf("incident_z").forGetter(CaseRecord::incidentZ),
                Codec.STRING.optionalFieldOf("witness_name", "").forGetter(CaseRecord::witnessName),
                Codec.STRING.optionalFieldOf("guard_name", "").forGetter(CaseRecord::guardName),
                Codec.STRING.optionalFieldOf("court_role", "").forGetter(CaseRecord::courtRole),
                Codec.INT.optionalFieldOf("court_x", 0).forGetter(CaseRecord::courtX),
                Codec.INT.optionalFieldOf("court_z", 0).forGetter(CaseRecord::courtZ),
                Codec.STRING.fieldOf("stage").forGetter(CaseRecord::stage),
                Codec.LONG.fieldOf("created_tick").forGetter(CaseRecord::createdTick),
                Codec.LONG.fieldOf("stage_tick").forGetter(CaseRecord::stageTick),
                Codec.INT.optionalFieldOf("hold_ticks", 0).forGetter(CaseRecord::holdTicks)
        ).apply(instance, CaseRecord::new));

        public CaseRecord escalate(String latestOffense, int addedSeverity, long tick) {
            return new CaseRecord(id, suspectId, latestOffense,
                    Math.min(100, severity + Math.max(1, addedSeverity)),
                    incidentX, incidentY, incidentZ, witnessName, guardName,
                    courtRole, courtX, courtZ, stage, createdTick, Math.max(stageTick, tick), holdTicks);
        }

        public CaseRecord witness(String name, long tick) {
            return new CaseRecord(id, suspectId, offense, severity,
                    incidentX, incidentY, incidentZ, name, "",
                    courtRole, courtX, courtZ, "reporting", createdTick, tick, 0);
        }

        public CaseRecord warrant(String guard, long tick) {
            return new CaseRecord(id, suspectId, offense, severity,
                    incidentX, incidentY, incidentZ, witnessName, guard,
                    courtRole, courtX, courtZ, "arresting", createdTick, tick, 0);
        }

        public CaseRecord guard(String guard, long tick) {
            return new CaseRecord(id, suspectId, offense, severity,
                    incidentX, incidentY, incidentZ, witnessName, guard,
                    courtRole, courtX, courtZ, stage, createdTick, tick, holdTicks);
        }

        public CaseRecord holding(int ticks) {
            return new CaseRecord(id, suspectId, offense, severity,
                    incidentX, incidentY, incidentZ, witnessName, guardName,
                    courtRole, courtX, courtZ, stage, createdTick, stageTick, Math.max(0, ticks));
        }

        public CaseRecord stage(String nextStage, long tick) {
            return new CaseRecord(id, suspectId, offense, severity,
                    incidentX, incidentY, incidentZ, witnessName, guardName,
                    courtRole, courtX, courtZ, nextStage, createdTick, tick, 0);
        }

        public CaseRecord court(String role, int x, int z, long tick) {
            return new CaseRecord(id, suspectId, offense, severity,
                    incidentX, incidentY, incidentZ, witnessName, guardName,
                    role, x, z, "trial", createdTick, tick, 0);
        }
    }

    private static final Codec<ErdenJusticeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("next_id", 1L).forGetter(data -> data.nextId),
            CaseRecord.CODEC.listOf().optionalFieldOf("cases", List.of())
                    .forGetter(data -> List.copyOf(data.cases))
    ).apply(instance, ErdenJusticeSavedData::new));

    public static final SavedDataType<ErdenJusticeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_justice"),
            level -> new ErdenJusticeSavedData(),
            level -> CODEC
    );

    private long nextId;
    private final List<CaseRecord> cases;

    public ErdenJusticeSavedData() {
        this(1L, List.of());
    }

    private ErdenJusticeSavedData(long nextId, List<CaseRecord> cases) {
        this.nextId = Math.max(1L, nextId);
        this.cases = new ArrayList<>(cases);
    }

    public List<CaseRecord> cases() {
        return List.copyOf(cases);
    }

    public CaseRecord caseFor(UUID suspect) {
        String id = suspect.toString();
        for (CaseRecord record : cases) if (record.suspectId().equals(id)) return record;
        return null;
    }

    public CaseRecord observe(UUID suspect, String offense, int severity,
                              int x, int y, int z, long tick) {
        CaseRecord current = caseFor(suspect);
        if (current != null) {
            replace(current.id(), record -> record.escalate(offense, severity, tick));
            return caseFor(suspect);
        }
        if (cases.size() >= 32) cases.removeFirst();
        CaseRecord created = new CaseRecord(nextId++, suspect.toString(), offense,
                Math.max(1, Math.min(100, severity)), x, y, z,
                "", "", "", 0, 0, "observed", tick, tick, 0);
        cases.add(created);
        setDirty();
        return created;
    }

    public void assignWitness(long id, String witnessName, long tick) {
        replace(id, record -> record.witness(witnessName, tick));
    }

    public void issueWarrant(long id, String guardName, long tick) {
        replace(id, record -> record.warrant(guardName, tick));
    }

    public void reassignGuard(long id, String guardName, long tick) {
        replace(id, record -> record.guard(guardName, tick));
    }

    public void setHoldTicks(long id, int ticks) {
        replace(id, record -> record.holding(ticks));
    }

    public void advance(long id, String stage, long tick) {
        replace(id, record -> record.stage(stage, tick));
    }

    public void beginTrial(long id, String courtRole, int courtX, int courtZ, long tick) {
        replace(id, record -> record.court(courtRole, courtX, courtZ, tick));
    }

    public void close(long id) {
        if (cases.removeIf(record -> record.id() == id)) setDirty();
    }

    private void replace(long id, UnaryOperator<CaseRecord> updater) {
        for (int index = 0; index < cases.size(); index++) {
            CaseRecord current = cases.get(index);
            if (current.id() != id) continue;
            CaseRecord updated = updater.apply(current);
            if (!updated.equals(current)) {
                cases.set(index, updated);
                setDirty();
            }
            return;
        }
    }
}
