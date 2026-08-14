package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Persistent 77-home vacancy, lease and rent ledger for Erden's capital. */
public final class ErdenCapitalHousingSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record Residence(
            String slotId,
            int homeX,
            int homeZ,
            String tenure,
            String tenantRepresentative,
            long leaseStartDay,
            long lastRentDay,
            int dailyRent,
            long arrears,
            long totalRentPaid,
            long vacancyDays,
            int lettings) {
        private static final Codec<Residence> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("slot_id").forGetter(Residence::slotId),
                Codec.INT.fieldOf("home_x").forGetter(Residence::homeX),
                Codec.INT.fieldOf("home_z").forGetter(Residence::homeZ),
                Codec.STRING.optionalFieldOf("tenure", "owner_occupied").forGetter(Residence::tenure),
                Codec.STRING.optionalFieldOf("tenant_representative", "").forGetter(Residence::tenantRepresentative),
                Codec.LONG.optionalFieldOf("lease_start_day", -1L).forGetter(Residence::leaseStartDay),
                Codec.LONG.optionalFieldOf("last_rent_day", -1L).forGetter(Residence::lastRentDay),
                Codec.INT.optionalFieldOf("daily_rent", 0).forGetter(Residence::dailyRent),
                Codec.LONG.optionalFieldOf("arrears", 0L).forGetter(Residence::arrears),
                Codec.LONG.optionalFieldOf("total_rent_paid", 0L).forGetter(Residence::totalRentPaid),
                Codec.LONG.optionalFieldOf("vacancy_days", 0L).forGetter(Residence::vacancyDays),
                Codec.INT.optionalFieldOf("lettings", 0).forGetter(Residence::lettings)
        ).apply(instance, Residence::new));

        public Residence {
            dailyRent = Math.max(0, dailyRent);
            arrears = Math.max(0L, arrears);
            totalRentPaid = Math.max(0L, totalRentPaid);
            vacancyDays = Math.max(0L, vacancyDays);
            lettings = Math.max(0, lettings);
        }

        public boolean vacant() {
            return tenure.equals("vacant");
        }

        public boolean leased() {
            return tenure.equals("leased");
        }

        public Residence asOwnerOccupied() {
            return new Residence(slotId, homeX, homeZ, "owner_occupied", "",
                    -1L, lastRentDay, 0, 0L, totalRentPaid, 0L, lettings);
        }

        public Residence asVacant(long addedDays) {
            return new Residence(slotId, homeX, homeZ, "vacant", "",
                    -1L, lastRentDay, 0, 0L, totalRentPaid,
                    vacancyDays + Math.max(1L, addedDays), lettings);
        }

        public Residence asLease(String representative, long day, int rent) {
            return new Residence(slotId, homeX, homeZ, "leased", representative,
                    day, day - 1L, Math.max(1, rent), 0L, totalRentPaid, 0L, lettings + 1);
        }

        public Residence withTenant(String representative) {
            return new Residence(slotId, homeX, homeZ, tenure, representative,
                    leaseStartDay, lastRentDay, dailyRent, arrears,
                    totalRentPaid, vacancyDays, lettings);
        }

        public Residence withRentDay(long day, int rent, long paid) {
            long safePaid = Math.max(0L, paid);
            long due = Math.max(1, rent);
            return new Residence(slotId, homeX, homeZ, tenure, tenantRepresentative,
                    leaseStartDay, day, (int) due,
                    arrears + Math.max(0L, due - safePaid),
                    totalRentPaid + safePaid, vacancyDays, lettings);
        }
    }

    private static final Codec<ErdenCapitalHousingSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("housing_revision", 0).forGetter(data -> data.housingRevision),
            Codec.LONG.optionalFieldOf("last_processed_day", -1L).forGetter(data -> data.lastProcessedDay),
            Residence.CODEC.listOf().optionalFieldOf("residences", List.of()).forGetter(data -> List.copyOf(data.residences)),
            Codec.LONG.optionalFieldOf("crown_rent_reserve", 0L).forGetter(data -> data.crownRentReserve),
            Codec.LONG.optionalFieldOf("unclaimed_estate_reserve", 0L).forGetter(data -> data.unclaimedEstateReserve),
            Codec.INT.optionalFieldOf("independent_households", 0).forGetter(data -> data.independentHouseholds),
            Codec.INT.optionalFieldOf("migrant_households", 0).forGetter(data -> data.migrantHouseholds),
            Codec.INT.optionalFieldOf("next_migrant_sequence", 1).forGetter(data -> data.nextMigrantSequence)
    ).apply(instance, ErdenCapitalHousingSavedData::new));

    public static final SavedDataType<ErdenCapitalHousingSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_capital_housing"),
            level -> new ErdenCapitalHousingSavedData(),
            level -> CODEC
    );

    private int housingRevision;
    private long lastProcessedDay;
    private final List<Residence> residences;
    private long crownRentReserve;
    private long unclaimedEstateReserve;
    private int independentHouseholds;
    private int migrantHouseholds;
    private int nextMigrantSequence;

    public ErdenCapitalHousingSavedData() {
        this(0, -1L, List.of(), 0L, 0L, 0, 0, 1);
    }

    private ErdenCapitalHousingSavedData(
            int housingRevision,
            long lastProcessedDay,
            List<Residence> residences,
            long crownRentReserve,
            long unclaimedEstateReserve,
            int independentHouseholds,
            int migrantHouseholds,
            int nextMigrantSequence) {
        this.housingRevision = Math.max(0, housingRevision);
        this.lastProcessedDay = lastProcessedDay;
        this.residences = new ArrayList<>(residences);
        this.crownRentReserve = Math.max(0L, crownRentReserve);
        this.unclaimedEstateReserve = Math.max(0L, unclaimedEstateReserve);
        this.independentHouseholds = Math.max(0, independentHouseholds);
        this.migrantHouseholds = Math.max(0, migrantHouseholds);
        this.nextMigrantSequence = Math.max(1, nextMigrantSequence);
    }

    public boolean initialized(int revision, int expectedResidences) {
        return housingRevision == revision && residences.size() == expectedResidences;
    }

    public long lastProcessedDay() {
        return lastProcessedDay;
    }

    public List<Residence> residences() {
        return List.copyOf(residences);
    }

    public long crownRentReserve() {
        return crownRentReserve;
    }

    public long unclaimedEstateReserve() {
        return unclaimedEstateReserve;
    }

    public int independentHouseholds() {
        return independentHouseholds;
    }

    public int migrantHouseholds() {
        return migrantHouseholds;
    }

    public int nextMigrantSequence() {
        return nextMigrantSequence;
    }

    public void initialize(int revision, long day, List<Residence> initial) {
        housingRevision = revision;
        lastProcessedDay = day - 1L;
        residences.clear();
        residences.addAll(initial);
        crownRentReserve = 0L;
        unclaimedEstateReserve = 0L;
        independentHouseholds = 0;
        migrantHouseholds = 0;
        nextMigrantSequence = 1;
        setDirty();
    }

    public void replaceDay(
            long day,
            List<Residence> replacement,
            long addedRent,
            long addedEstate,
            int addedIndependent,
            int addedMigrants,
            int nextSequence) {
        if (day <= lastProcessedDay) return;
        lastProcessedDay = day;
        residences.clear();
        residences.addAll(replacement);
        crownRentReserve += Math.max(0L, addedRent);
        unclaimedEstateReserve += Math.max(0L, addedEstate);
        independentHouseholds += Math.max(0, addedIndependent);
        migrantHouseholds += Math.max(0, addedMigrants);
        nextMigrantSequence = Math.max(nextMigrantSequence, nextSequence);
        setDirty();
    }

    public int vacantCount() {
        int count = 0;
        for (Residence residence : residences) if (residence.vacant()) count++;
        return count;
    }

    public int leasedCount() {
        int count = 0;
        for (Residence residence : residences) if (residence.leased()) count++;
        return count;
    }
}
