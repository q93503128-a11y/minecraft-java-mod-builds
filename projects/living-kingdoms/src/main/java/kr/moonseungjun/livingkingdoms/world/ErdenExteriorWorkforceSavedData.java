package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Persistent households, deaths and daily attendance for Erden's rural production belt. */
public final class ErdenExteriorWorkforceSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record Resident(
            String id,
            String name,
            String lifeStage,
            int bedSlot,
            String workRole,
            int shiftStart,
            int shiftEnd,
            int restDay) {
        private static final Codec<Resident> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Resident::id),
                Codec.STRING.fieldOf("name").forGetter(Resident::name),
                Codec.STRING.fieldOf("life_stage").forGetter(Resident::lifeStage),
                Codec.INT.fieldOf("bed_slot").forGetter(Resident::bedSlot),
                Codec.STRING.fieldOf("work_role").forGetter(Resident::workRole),
                Codec.INT.fieldOf("shift_start").forGetter(Resident::shiftStart),
                Codec.INT.fieldOf("shift_end").forGetter(Resident::shiftEnd),
                Codec.INT.fieldOf("rest_day").forGetter(Resident::restDay)
        ).apply(instance, Resident::new));

        public boolean worker() {
            return !workRole.isBlank();
        }
    }

    public record Household(
            String id,
            String familyName,
            String nodeId,
            String nodeRole,
            int homeX,
            int homeZ,
            List<Resident> residents) {
        private static final Codec<Household> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Household::id),
                Codec.STRING.fieldOf("family_name").forGetter(Household::familyName),
                Codec.STRING.fieldOf("node_id").forGetter(Household::nodeId),
                Codec.STRING.fieldOf("node_role").forGetter(Household::nodeRole),
                Codec.INT.fieldOf("home_x").forGetter(Household::homeX),
                Codec.INT.fieldOf("home_z").forGetter(Household::homeZ),
                Resident.CODEC.listOf().fieldOf("residents").forGetter(Household::residents)
        ).apply(instance, Household::new));

        public Household {
            residents = List.copyOf(residents);
        }
    }

    public record NodeLabor(
            String nodeId,
            long day,
            int requiredWorkers,
            int aliveWorkers,
            int attendedWorkers,
            int absentWorkers,
            int deadWorkers,
            int productionPercent,
            long cumulativeWorkDays,
            long cumulativeAbsences) {
        private static final Codec<NodeLabor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("node_id").forGetter(NodeLabor::nodeId),
                Codec.LONG.fieldOf("day").forGetter(NodeLabor::day),
                Codec.INT.fieldOf("required_workers").forGetter(NodeLabor::requiredWorkers),
                Codec.INT.fieldOf("alive_workers").forGetter(NodeLabor::aliveWorkers),
                Codec.INT.fieldOf("attended_workers").forGetter(NodeLabor::attendedWorkers),
                Codec.INT.fieldOf("absent_workers").forGetter(NodeLabor::absentWorkers),
                Codec.INT.fieldOf("dead_workers").forGetter(NodeLabor::deadWorkers),
                Codec.INT.fieldOf("production_percent").forGetter(NodeLabor::productionPercent),
                Codec.LONG.fieldOf("cumulative_work_days").forGetter(NodeLabor::cumulativeWorkDays),
                Codec.LONG.fieldOf("cumulative_absences").forGetter(NodeLabor::cumulativeAbsences)
        ).apply(instance, NodeLabor::new));
    }

    private static final Codec<ErdenExteriorWorkforceSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("workforce_revision", 0)
                    .forGetter(data -> data.workforceRevision),
            Codec.LONG.optionalFieldOf("last_processed_day", -1L)
                    .forGetter(data -> data.lastProcessedDay),
            Household.CODEC.listOf().optionalFieldOf("households", List.of())
                    .forGetter(data -> List.copyOf(data.households)),
            NodeLabor.CODEC.listOf().optionalFieldOf("node_labor", List.of())
                    .forGetter(data -> List.copyOf(data.nodeLabor)),
            Codec.STRING.listOf().optionalFieldOf("dead_residents", List.of())
                    .forGetter(data -> List.copyOf(data.deadResidentIds))
    ).apply(instance, ErdenExteriorWorkforceSavedData::new));

    public static final SavedDataType<ErdenExteriorWorkforceSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_exterior_workforce"),
            level -> new ErdenExteriorWorkforceSavedData(),
            level -> CODEC
    );

    private int workforceRevision;
    private long lastProcessedDay;
    private final List<Household> households;
    private final List<NodeLabor> nodeLabor;
    private final Set<String> deadResidentIds;

    public ErdenExteriorWorkforceSavedData() {
        this(0, -1L, List.of(), List.of(), List.of());
    }

    private ErdenExteriorWorkforceSavedData(
            int workforceRevision,
            long lastProcessedDay,
            List<Household> households,
            List<NodeLabor> nodeLabor,
            List<String> deadResidentIds) {
        this.workforceRevision = Math.max(0, workforceRevision);
        this.lastProcessedDay = lastProcessedDay;
        this.households = new ArrayList<>(households);
        this.nodeLabor = new ArrayList<>(nodeLabor);
        this.deadResidentIds = new LinkedHashSet<>(deadResidentIds);
    }

    public boolean hasPopulation(int revision, int expectedHouseholds) {
        return workforceRevision == revision && households.size() == expectedHouseholds;
    }

    public List<Household> households() {
        return List.copyOf(households);
    }

    public List<NodeLabor> nodeLabor() {
        return List.copyOf(nodeLabor);
    }

    public long lastProcessedDay() {
        return lastProcessedDay;
    }

    public void replacePopulation(int revision, List<Household> replacement) {
        if (workforceRevision == revision && households.equals(replacement)) return;
        workforceRevision = revision;
        households.clear();
        households.addAll(replacement);
        nodeLabor.clear();
        deadResidentIds.clear();
        lastProcessedDay = -1L;
        setDirty();
    }

    public boolean recordDay(long day, List<NodeLabor> dailyStates) {
        if (day <= lastProcessedDay) return false;
        Map<String, NodeLabor> previous = new LinkedHashMap<>();
        for (NodeLabor labor : nodeLabor) previous.put(labor.nodeId(), labor);
        nodeLabor.clear();
        for (NodeLabor daily : dailyStates) {
            NodeLabor old = previous.get(daily.nodeId());
            long workDays = (old == null ? 0L : old.cumulativeWorkDays())
                    + Math.max(0, daily.attendedWorkers());
            long absences = (old == null ? 0L : old.cumulativeAbsences())
                    + Math.max(0, daily.absentWorkers());
            nodeLabor.add(new NodeLabor(
                    daily.nodeId(), day,
                    daily.requiredWorkers(), daily.aliveWorkers(),
                    daily.attendedWorkers(), daily.absentWorkers(),
                    daily.deadWorkers(), daily.productionPercent(),
                    workDays, absences));
        }
        lastProcessedDay = day;
        setDirty();
        return true;
    }

    public NodeLabor labor(String nodeId) {
        for (NodeLabor labor : nodeLabor) {
            if (labor.nodeId().equals(nodeId)) return labor;
        }
        return null;
    }

    public int productionPercent(String nodeId, long day) {
        NodeLabor labor = labor(nodeId);
        return labor != null && labor.day() == day ? labor.productionPercent() : 0;
    }

    public boolean isDead(String residentId) {
        return deadResidentIds.contains(residentId);
    }

    public void markDead(String residentId) {
        if (deadResidentIds.add(residentId)) setDirty();
    }

    public int householdCount() {
        return households.size();
    }

    public int residentCount() {
        int total = 0;
        for (Household household : households) total += household.residents().size();
        return total;
    }

    public int workerCount() {
        int total = 0;
        for (Household household : households) {
            for (Resident resident : household.residents()) if (resident.worker()) total++;
        }
        return total;
    }

    public int dependentCount() {
        return residentCount() - workerCount();
    }

    public int aliveWorkerCount() {
        int total = 0;
        for (Household household : households) {
            for (Resident resident : household.residents()) {
                if (resident.worker() && !isDead(resident.id())) total++;
            }
        }
        return total;
    }

    public int deadWorkerCount() {
        return workerCount() - aliveWorkerCount();
    }

    public int attendedWorkerCount() {
        int total = 0;
        for (NodeLabor labor : nodeLabor) total += Math.max(0, labor.attendedWorkers());
        return total;
    }

    public int absentWorkerCount() {
        int total = 0;
        for (NodeLabor labor : nodeLabor) total += Math.max(0, labor.absentWorkers());
        return total;
    }
}
