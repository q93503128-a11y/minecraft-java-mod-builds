package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Persistent marriage/remarriage ledger for Erden capital households. */
public final class ErdenCapitalMarriageSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record Union(
            String id,
            String personA,
            String personB,
            String householdId,
            long startDay,
            long endDay,
            boolean remarriage) {
        private static final Codec<Union> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Union::id),
                Codec.STRING.fieldOf("person_a").forGetter(Union::personA),
                Codec.STRING.fieldOf("person_b").forGetter(Union::personB),
                Codec.STRING.fieldOf("household_id").forGetter(Union::householdId),
                Codec.LONG.fieldOf("start_day").forGetter(Union::startDay),
                Codec.LONG.optionalFieldOf("end_day", -1L).forGetter(Union::endDay),
                Codec.BOOL.optionalFieldOf("remarriage", false).forGetter(Union::remarriage)
        ).apply(instance, Union::new));

        public boolean activeOn(long day) {
            return startDay <= day && (endDay < 0L || endDay > day);
        }

        public boolean involves(String personId) {
            return personA.equals(personId) || personB.equals(personId);
        }

        public String spouseOf(String personId) {
            if (personA.equals(personId)) return personB;
            if (personB.equals(personId)) return personA;
            return "";
        }

        public Union withEnd(long day) {
            if (endDay >= 0L && endDay <= day) return this;
            return new Union(id, personA, personB, householdId, startDay, day, remarriage);
        }
    }

    private static final Codec<ErdenCapitalMarriageSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("marriage_revision", 0).forGetter(data -> data.marriageRevision),
            Codec.INT.optionalFieldOf("last_processed_year", -1).forGetter(data -> data.lastProcessedYear),
            Codec.INT.optionalFieldOf("next_union_sequence", 1).forGetter(data -> data.nextUnionSequence),
            Union.CODEC.listOf().optionalFieldOf("unions", List.of()).forGetter(data -> List.copyOf(data.unions)),
            Codec.INT.optionalFieldOf("household_moves", 0).forGetter(data -> data.householdMoves)
    ).apply(instance, ErdenCapitalMarriageSavedData::new));

    public static final SavedDataType<ErdenCapitalMarriageSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_capital_marriages"),
            level -> new ErdenCapitalMarriageSavedData(),
            level -> CODEC
    );

    private int marriageRevision;
    private int lastProcessedYear;
    private int nextUnionSequence;
    private final List<Union> unions;
    private int householdMoves;

    public ErdenCapitalMarriageSavedData() {
        this(0, -1, 1, List.of(), 0);
    }

    private ErdenCapitalMarriageSavedData(
            int marriageRevision,
            int lastProcessedYear,
            int nextUnionSequence,
            List<Union> unions,
            int householdMoves) {
        this.marriageRevision = Math.max(0, marriageRevision);
        this.lastProcessedYear = lastProcessedYear;
        this.nextUnionSequence = Math.max(1, nextUnionSequence);
        this.unions = new ArrayList<>(unions);
        this.householdMoves = Math.max(0, householdMoves);
    }

    public boolean initialized(int revision) {
        return marriageRevision == revision;
    }

    public int lastProcessedYear() {
        return lastProcessedYear;
    }

    public int nextUnionSequence() {
        return nextUnionSequence;
    }

    public List<Union> unions() {
        return List.copyOf(unions);
    }

    public int householdMoves() {
        return householdMoves;
    }

    public Union activeUnion(String personId, long day) {
        for (Union union : unions) {
            if (union.activeOn(day) && union.involves(personId)) return union;
        }
        return null;
    }

    public void initialize(int revision, int nextSequence, List<Union> initialUnions) {
        marriageRevision = revision;
        lastProcessedYear = 0;
        nextUnionSequence = Math.max(1, nextSequence);
        unions.clear();
        unions.addAll(initialUnions);
        householdMoves = 0;
        setDirty();
    }

    public void replaceYear(
            int year,
            int nextSequence,
            List<Union> replacement,
            int additionalMoves) {
        if (year < lastProcessedYear) return;
        lastProcessedYear = year;
        nextUnionSequence = Math.max(1, nextSequence);
        unions.clear();
        unions.addAll(replacement);
        householdMoves += Math.max(0, additionalMoves);
        setDirty();
    }

    public int activeCount(long day) {
        int count = 0;
        for (Union union : unions) if (union.activeOn(day)) count++;
        return count;
    }

    public int remarriageCount() {
        int count = 0;
        for (Union union : unions) if (union.remarriage()) count++;
        return count;
    }
}
