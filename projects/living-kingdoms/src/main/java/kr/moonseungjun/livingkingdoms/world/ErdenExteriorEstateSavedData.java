package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Save-compatible ownership and maintenance overlay for Erden's exterior homes.
 * The workforce and genealogy codecs remain untouched; succession updates only this estate ledger.
 */
public final class ErdenExteriorEstateSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record Estate(
            String householdId,
            String nodeId,
            int homeX,
            int homeZ,
            String stewardId,
            String heirId,
            int livingMembers,
            int capacity,
            int maintenanceReserve,
            int condition,
            int inheritedCount,
            int lastSuccessionCount,
            long lastProcessedDay,
            boolean vacant,
            boolean overcrowded) {
        private static final Codec<Estate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("household_id").forGetter(Estate::householdId),
                Codec.STRING.fieldOf("node_id").forGetter(Estate::nodeId),
                Codec.INT.fieldOf("home_x").forGetter(Estate::homeX),
                Codec.INT.fieldOf("home_z").forGetter(Estate::homeZ),
                Codec.STRING.optionalFieldOf("steward_id", "").forGetter(Estate::stewardId),
                Codec.STRING.optionalFieldOf("heir_id", "").forGetter(Estate::heirId),
                Codec.INT.optionalFieldOf("living_members", 0).forGetter(Estate::livingMembers),
                Codec.INT.optionalFieldOf("capacity", 5).forGetter(Estate::capacity),
                Codec.INT.optionalFieldOf("maintenance_reserve", 0)
                        .forGetter(Estate::maintenanceReserve),
                Codec.INT.optionalFieldOf("condition", 100).forGetter(Estate::condition),
                Codec.INT.optionalFieldOf("inherited_count", 0).forGetter(Estate::inheritedCount),
                Codec.INT.optionalFieldOf("last_succession_count", 0)
                        .forGetter(Estate::lastSuccessionCount),
                Codec.LONG.optionalFieldOf("last_processed_day", -1L)
                        .forGetter(Estate::lastProcessedDay),
                Codec.BOOL.optionalFieldOf("vacant", false).forGetter(Estate::vacant),
                Codec.BOOL.optionalFieldOf("overcrowded", false).forGetter(Estate::overcrowded)
        ).apply(instance, Estate::new));

        public Estate {
            livingMembers = Math.max(0, livingMembers);
            capacity = Math.max(1, capacity);
            maintenanceReserve = Math.max(0, maintenanceReserve);
            condition = Math.clamp(condition, 0, 100);
            inheritedCount = Math.max(0, inheritedCount);
            lastSuccessionCount = Math.max(0, lastSuccessionCount);
        }

        public Estate withDailyState(
                String steward,
                String heir,
                int living,
                int reserve,
                int homeCondition,
                int inherited,
                int successionCount,
                long day,
                boolean isVacant,
                boolean isOvercrowded) {
            return new Estate(
                    householdId, nodeId, homeX, homeZ,
                    steward, heir, living, capacity, reserve, homeCondition,
                    inherited, successionCount, day, isVacant, isOvercrowded);
        }
    }

    private static final Codec<ErdenExteriorEstateSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("estate_revision", 0)
                    .forGetter(data -> data.estateRevision),
            Codec.LONG.optionalFieldOf("last_processed_day", -1L)
                    .forGetter(data -> data.lastProcessedDay),
            Estate.CODEC.listOf().optionalFieldOf("estates", List.of())
                    .forGetter(data -> List.copyOf(data.estates))
    ).apply(instance, ErdenExteriorEstateSavedData::new));

    public static final SavedDataType<ErdenExteriorEstateSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_exterior_estates"),
            level -> new ErdenExteriorEstateSavedData(),
            level -> CODEC
    );

    private int estateRevision;
    private long lastProcessedDay;
    private final List<Estate> estates;

    public ErdenExteriorEstateSavedData() {
        this(0, -1L, List.of());
    }

    private ErdenExteriorEstateSavedData(
            int estateRevision,
            long lastProcessedDay,
            List<Estate> estates) {
        this.estateRevision = Math.max(0, estateRevision);
        this.lastProcessedDay = lastProcessedDay;
        this.estates = new ArrayList<>(estates);
    }

    public boolean initialized(int revision, int expectedEstates) {
        return estateRevision == revision && estates.size() == expectedEstates;
    }

    public long lastProcessedDay() {
        return lastProcessedDay;
    }

    public List<Estate> estates() {
        return List.copyOf(estates);
    }

    public Estate estate(String householdId) {
        for (Estate estate : estates) {
            if (estate.householdId().equals(householdId)) return estate;
        }
        return null;
    }

    public void initialize(int revision, long day, List<Estate> initialEstates) {
        estateRevision = revision;
        lastProcessedDay = day - 1L;
        estates.clear();
        estates.addAll(initialEstates);
        setDirty();
    }

    public void replaceDay(long day, List<Estate> replacement) {
        if (day < lastProcessedDay) return;
        lastProcessedDay = day;
        estates.clear();
        estates.addAll(replacement);
        setDirty();
    }

    public int occupiedCount() {
        int count = 0;
        for (Estate estate : estates) if (!estate.vacant()) count++;
        return count;
    }

    public int vacantCount() {
        return estates.size() - occupiedCount();
    }

    public int overcrowdedCount() {
        int count = 0;
        for (Estate estate : estates) if (estate.overcrowded()) count++;
        return count;
    }

    public int inheritanceCount() {
        int count = 0;
        for (Estate estate : estates) count += estate.inheritedCount();
        return count;
    }

    public int totalMaintenanceReserve() {
        int total = 0;
        for (Estate estate : estates) total += estate.maintenanceReserve();
        return total;
    }

    public int minimumCondition() {
        int minimum = 100;
        for (Estate estate : estates) minimum = Math.min(minimum, estate.condition());
        return estates.isEmpty() ? 0 : minimum;
    }
}
