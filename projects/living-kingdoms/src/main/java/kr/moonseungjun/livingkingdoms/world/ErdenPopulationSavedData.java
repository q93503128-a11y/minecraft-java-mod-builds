package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Persistent Erden households, residents, deaths and aggregate daily civic supplies. */
public final class ErdenPopulationSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record Resident(
            String id,
            String name,
            String lifeStage,
            int bedSlot,
            int workX,
            int workZ,
            String workRole,
            int shiftStart,
            int shiftEnd) {
        private static final Codec<Resident> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Resident::id),
                Codec.STRING.fieldOf("name").forGetter(Resident::name),
                Codec.STRING.fieldOf("life_stage").forGetter(Resident::lifeStage),
                Codec.INT.fieldOf("bed_slot").forGetter(Resident::bedSlot),
                Codec.INT.fieldOf("work_x").forGetter(Resident::workX),
                Codec.INT.fieldOf("work_z").forGetter(Resident::workZ),
                Codec.STRING.fieldOf("work_role").forGetter(Resident::workRole),
                Codec.INT.fieldOf("shift_start").forGetter(Resident::shiftStart),
                Codec.INT.fieldOf("shift_end").forGetter(Resident::shiftEnd)
        ).apply(instance, Resident::new));

        public boolean worker() {
            return !workRole.isBlank();
        }
    }

    public record Household(
            String id,
            String familyName,
            int homeX,
            int homeZ,
            List<Resident> residents) {
        private static final Codec<Household> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Household::id),
                Codec.STRING.fieldOf("family_name").forGetter(Household::familyName),
                Codec.INT.fieldOf("home_x").forGetter(Household::homeX),
                Codec.INT.fieldOf("home_z").forGetter(Household::homeZ),
                Resident.CODEC.listOf().fieldOf("residents").forGetter(Household::residents)
        ).apply(instance, Household::new));

        public Household {
            residents = List.copyOf(residents);
        }
    }

    public record LedgerEntry(String resource, long stock, long shortage) {
        private static final Codec<LedgerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("resource").forGetter(LedgerEntry::resource),
                Codec.LONG.fieldOf("stock").forGetter(LedgerEntry::stock),
                Codec.LONG.fieldOf("shortage").forGetter(LedgerEntry::shortage)
        ).apply(instance, LedgerEntry::new));
    }

    private static final Codec<ErdenPopulationSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("population_revision", 0)
                    .forGetter(data -> data.populationRevision),
            Codec.LONG.optionalFieldOf("last_processed_day", -1L)
                    .forGetter(data -> data.lastProcessedDay),
            Household.CODEC.listOf().optionalFieldOf("households", List.of())
                    .forGetter(data -> List.copyOf(data.households)),
            LedgerEntry.CODEC.listOf().optionalFieldOf("ledger", List.of())
                    .forGetter(data -> List.copyOf(data.ledger)),
            Codec.STRING.listOf().optionalFieldOf("dead_residents", List.of())
                    .forGetter(data -> List.copyOf(data.deadResidentIds))
    ).apply(instance, ErdenPopulationSavedData::new));

    public static final SavedDataType<ErdenPopulationSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_population"),
            level -> new ErdenPopulationSavedData(),
            level -> CODEC
    );

    private int populationRevision;
    private long lastProcessedDay;
    private final List<Household> households;
    private final List<LedgerEntry> ledger;
    private final Set<String> deadResidentIds;

    public ErdenPopulationSavedData() {
        this(0, -1L, List.of(), List.of(), List.of());
    }

    private ErdenPopulationSavedData(
            int populationRevision,
            long lastProcessedDay,
            List<Household> households,
            List<LedgerEntry> ledger,
            List<String> deadResidentIds) {
        this.populationRevision = Math.max(0, populationRevision);
        this.lastProcessedDay = lastProcessedDay;
        this.households = new ArrayList<>(households);
        this.ledger = new ArrayList<>(ledger);
        this.deadResidentIds = new LinkedHashSet<>(deadResidentIds);
    }

    public boolean hasPopulation(int revision, int expectedHouseholds) {
        return populationRevision == revision && households.size() == expectedHouseholds;
    }

    public List<Household> households() {
        return List.copyOf(households);
    }

    public void replacePopulation(int revision, List<Household> replacement) {
        if (populationRevision == revision && households.equals(replacement)) return;
        populationRevision = revision;
        households.clear();
        households.addAll(replacement);
        deadResidentIds.clear();
        ledger.clear();
        lastProcessedDay = -1L;
        setDirty();
    }

    public long lastProcessedDay() {
        return lastProcessedDay;
    }

    public boolean applyDay(long day, Map<String, Long> production, Map<String, Long> consumption) {
        if (day <= lastProcessedDay) return false;
        Map<String, LedgerEntry> current = new TreeMap<>();
        for (LedgerEntry entry : ledger) current.put(entry.resource(), entry);
        Set<String> resources = new HashSet<>(current.keySet());
        resources.addAll(production.keySet());
        resources.addAll(consumption.keySet());

        Map<String, LedgerEntry> updated = new TreeMap<>();
        for (String resource : resources) {
            LedgerEntry previous = current.getOrDefault(resource, new LedgerEntry(resource, 0L, 0L));
            long available = Math.max(0L, previous.stock())
                    + Math.max(0L, production.getOrDefault(resource, 0L));
            long required = Math.max(0L, consumption.getOrDefault(resource, 0L));
            long fulfilled = Math.min(available, required);
            long missing = required - fulfilled;
            updated.put(resource, new LedgerEntry(
                    resource,
                    available - fulfilled,
                    previous.shortage() + missing));
        }
        ledger.clear();
        ledger.addAll(updated.values());
        lastProcessedDay = day;
        setDirty();
        return true;
    }

    public Map<String, Long> stocks() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (LedgerEntry entry : ledger) result.put(entry.resource(), entry.stock());
        return Map.copyOf(result);
    }

    public long totalShortage() {
        long total = 0L;
        for (LedgerEntry entry : ledger) total += Math.max(0L, entry.shortage());
        return total;
    }

    public boolean isDead(String residentId) {
        return deadResidentIds.contains(residentId);
    }

    public void markDead(String residentId) {
        if (deadResidentIds.add(residentId)) setDirty();
    }

    public int aliveResidentCount() {
        int total = 0;
        for (Household household : households) {
            for (Resident resident : household.residents()) {
                if (!isDead(resident.id())) total++;
            }
        }
        return total;
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
}
