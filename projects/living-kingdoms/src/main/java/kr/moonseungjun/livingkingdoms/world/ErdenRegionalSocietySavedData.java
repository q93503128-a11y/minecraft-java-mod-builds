package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persistent founding households and residents for Erden's second-ring regional villages. */
public final class ErdenRegionalSocietySavedData extends SavedData {
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
            String settlementId,
            String familyName,
            String homeRole,
            int homeX,
            int homeZ,
            List<Resident> residents) {
        private static final Codec<Household> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Household::id),
                Codec.STRING.fieldOf("settlement_id").forGetter(Household::settlementId),
                Codec.STRING.fieldOf("family_name").forGetter(Household::familyName),
                Codec.STRING.fieldOf("home_role").forGetter(Household::homeRole),
                Codec.INT.fieldOf("home_x").forGetter(Household::homeX),
                Codec.INT.fieldOf("home_z").forGetter(Household::homeZ),
                Resident.CODEC.listOf().fieldOf("residents").forGetter(Household::residents)
        ).apply(instance, Household::new));

        public Household {
            residents = List.copyOf(residents);
        }
    }

    private static final Codec<ErdenRegionalSocietySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("society_revision", 0)
                    .forGetter(data -> data.societyRevision),
            Household.CODEC.listOf().optionalFieldOf("households", List.of())
                    .forGetter(data -> List.copyOf(data.households)),
            Codec.STRING.listOf().optionalFieldOf("dead_residents", List.of())
                    .forGetter(data -> List.copyOf(data.deadResidentIds))
    ).apply(instance, ErdenRegionalSocietySavedData::new));

    public static final SavedDataType<ErdenRegionalSocietySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_regional_society"),
            level -> new ErdenRegionalSocietySavedData(),
            level -> CODEC
    );

    private int societyRevision;
    private final List<Household> households;
    private final Set<String> deadResidentIds;

    public ErdenRegionalSocietySavedData() {
        this(0, List.of(), List.of());
    }

    private ErdenRegionalSocietySavedData(
            int societyRevision,
            List<Household> households,
            List<String> deadResidentIds) {
        this.societyRevision = Math.max(0, societyRevision);
        this.households = new ArrayList<>(households);
        this.deadResidentIds = new LinkedHashSet<>(deadResidentIds);
    }

    public boolean hasPopulation(int revision, int expectedHouseholds) {
        return societyRevision == revision && households.size() == expectedHouseholds;
    }

    public List<Household> households() {
        return List.copyOf(households);
    }

    public void replacePopulation(int revision, List<Household> replacement) {
        if (societyRevision == revision && households.equals(replacement)) return;
        societyRevision = revision;
        households.clear();
        households.addAll(replacement);
        deadResidentIds.clear();
        setDirty();
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

    public int deadCount() {
        return deadResidentIds.size();
    }
}
