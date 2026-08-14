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
 * Save-compatible genealogy overlay for Erden's 77 capital households.
 *
 * <p>The founding 231 residents remain authoritative in {@link ErdenPopulationSavedData}. Ages,
 * descendants, retirement and succession live here so existing worlds never need their original
 * population codec rewritten. Descendants can inherit vacated workplaces while the fixed 77 homes,
 * 156 worksites and their household wallets keep their original identities.</p>
 */
public final class ErdenCapitalLifecycleSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record Person(
            String id,
            String name,
            String householdId,
            long birthDay,
            String parentA,
            String parentB,
            int generation,
            boolean founder,
            boolean foundingWorker,
            int workX,
            int workZ,
            String workRole,
            int shiftStart,
            int shiftEnd,
            long retirementDay,
            long deathDay) {
        private static final Codec<Person> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Person::id),
                Codec.STRING.fieldOf("name").forGetter(Person::name),
                Codec.STRING.fieldOf("household_id").forGetter(Person::householdId),
                Codec.LONG.fieldOf("birth_day").forGetter(Person::birthDay),
                Codec.STRING.optionalFieldOf("parent_a", "").forGetter(Person::parentA),
                Codec.STRING.optionalFieldOf("parent_b", "").forGetter(Person::parentB),
                Codec.INT.optionalFieldOf("generation", 0).forGetter(Person::generation),
                Codec.BOOL.optionalFieldOf("founder", false).forGetter(Person::founder),
                Codec.BOOL.optionalFieldOf("founding_worker", false).forGetter(Person::foundingWorker),
                Codec.INT.optionalFieldOf("work_x", 0).forGetter(Person::workX),
                Codec.INT.optionalFieldOf("work_z", 0).forGetter(Person::workZ),
                Codec.STRING.optionalFieldOf("work_role", "").forGetter(Person::workRole),
                Codec.INT.optionalFieldOf("shift_start", 0).forGetter(Person::shiftStart),
                Codec.INT.optionalFieldOf("shift_end", 0).forGetter(Person::shiftEnd),
                Codec.LONG.optionalFieldOf("retirement_day", -1L).forGetter(Person::retirementDay),
                Codec.LONG.optionalFieldOf("death_day", -1L).forGetter(Person::deathDay)
        ).apply(instance, Person::new));

        public boolean aliveOn(long day) {
            return deathDay < 0L || deathDay > day;
        }

        public boolean retiredOn(long day) {
            return retirementDay >= 0L && retirementDay <= day;
        }

        public boolean assignedWorker() {
            return !workRole.isBlank();
        }

        public Person withWork(int x, int z, String role, int start, int end) {
            return new Person(id, name, householdId, birthDay, parentA, parentB,
                    generation, founder, foundingWorker, x, z, role, start, end,
                    retirementDay, deathDay);
        }

        public Person withRetirement(long day) {
            return new Person(id, name, householdId, birthDay, parentA, parentB,
                    generation, founder, foundingWorker, workX, workZ, workRole,
                    shiftStart, shiftEnd, day, deathDay);
        }

        public Person withDeath(long day) {
            return new Person(id, name, householdId, birthDay, parentA, parentB,
                    generation, founder, foundingWorker, workX, workZ, workRole,
                    shiftStart, shiftEnd, retirementDay, day);
        }
    }

    public record HouseholdLine(
            String householdId,
            String stewardId,
            String heirId,
            int lastBirthYear,
            int successionCount,
            int birthCount) {
        private static final Codec<HouseholdLine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("household_id").forGetter(HouseholdLine::householdId),
                Codec.STRING.optionalFieldOf("steward_id", "").forGetter(HouseholdLine::stewardId),
                Codec.STRING.optionalFieldOf("heir_id", "").forGetter(HouseholdLine::heirId),
                Codec.INT.optionalFieldOf("last_birth_year", Integer.MIN_VALUE)
                        .forGetter(HouseholdLine::lastBirthYear),
                Codec.INT.optionalFieldOf("succession_count", 0).forGetter(HouseholdLine::successionCount),
                Codec.INT.optionalFieldOf("birth_count", 0).forGetter(HouseholdLine::birthCount)
        ).apply(instance, HouseholdLine::new));

        public HouseholdLine withBirth(int year) {
            return new HouseholdLine(householdId, stewardId, heirId, year,
                    successionCount, birthCount + 1);
        }

        public HouseholdLine withSuccession(String steward, String heir) {
            return new HouseholdLine(householdId, steward, heir, lastBirthYear,
                    successionCount + 1, birthCount);
        }

        public HouseholdLine withHeir(String heir) {
            return new HouseholdLine(householdId, stewardId, heir, lastBirthYear,
                    successionCount, birthCount);
        }
    }

    private static final Codec<ErdenCapitalLifecycleSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("lifecycle_revision", 0).forGetter(data -> data.lifecycleRevision),
            Codec.LONG.optionalFieldOf("established_day", -1L).forGetter(data -> data.establishedDay),
            Codec.LONG.optionalFieldOf("last_processed_day", -1L).forGetter(data -> data.lastProcessedDay),
            Codec.INT.optionalFieldOf("next_birth_sequence", 1).forGetter(data -> data.nextBirthSequence),
            Person.CODEC.listOf().optionalFieldOf("persons", List.of()).forGetter(data -> List.copyOf(data.persons)),
            HouseholdLine.CODEC.listOf().optionalFieldOf("household_lines", List.of())
                    .forGetter(data -> List.copyOf(data.householdLines))
    ).apply(instance, ErdenCapitalLifecycleSavedData::new));

    public static final SavedDataType<ErdenCapitalLifecycleSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_capital_lifecycle"),
            level -> new ErdenCapitalLifecycleSavedData(),
            level -> CODEC
    );

    private int lifecycleRevision;
    private long establishedDay;
    private long lastProcessedDay;
    private int nextBirthSequence;
    private final List<Person> persons;
    private final List<HouseholdLine> householdLines;

    public ErdenCapitalLifecycleSavedData() {
        this(0, -1L, -1L, 1, List.of(), List.of());
    }

    private ErdenCapitalLifecycleSavedData(
            int lifecycleRevision,
            long establishedDay,
            long lastProcessedDay,
            int nextBirthSequence,
            List<Person> persons,
            List<HouseholdLine> householdLines) {
        this.lifecycleRevision = Math.max(0, lifecycleRevision);
        this.establishedDay = establishedDay;
        this.lastProcessedDay = lastProcessedDay;
        this.nextBirthSequence = Math.max(1, nextBirthSequence);
        this.persons = new ArrayList<>(persons);
        this.householdLines = new ArrayList<>(householdLines);
    }

    public boolean initialized(int revision, int founderCount, int householdCount) {
        return lifecycleRevision == revision
                && persons.stream().filter(Person::founder).count() == founderCount
                && householdLines.size() == householdCount;
    }

    public long establishedDay() {
        return establishedDay;
    }

    public long lastProcessedDay() {
        return lastProcessedDay;
    }

    public int nextBirthSequence() {
        return nextBirthSequence;
    }

    public List<Person> persons() {
        return List.copyOf(persons);
    }

    public List<HouseholdLine> householdLines() {
        return List.copyOf(householdLines);
    }

    public Person person(String id) {
        for (Person person : persons) if (person.id().equals(id)) return person;
        return null;
    }

    public Person personByName(String name) {
        for (Person person : persons) if (person.name().equals(name)) return person;
        return null;
    }

    public HouseholdLine householdLine(String householdId) {
        for (HouseholdLine line : householdLines) {
            if (line.householdId().equals(householdId)) return line;
        }
        return null;
    }

    public void initialize(
            int revision,
            long day,
            List<Person> founders,
            List<HouseholdLine> lines) {
        lifecycleRevision = revision;
        establishedDay = day;
        lastProcessedDay = day - 1L;
        nextBirthSequence = 1;
        persons.clear();
        persons.addAll(founders);
        householdLines.clear();
        householdLines.addAll(lines);
        setDirty();
    }

    public void replaceDay(
            long day,
            int nextSequence,
            List<Person> replacementPersons,
            List<HouseholdLine> replacementLines) {
        lastProcessedDay = day;
        nextBirthSequence = Math.max(1, nextSequence);
        persons.clear();
        persons.addAll(replacementPersons);
        householdLines.clear();
        householdLines.addAll(replacementLines);
        setDirty();
    }

    public void markDeath(String personId, long day) {
        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            if (!person.id().equals(personId) || !person.aliveOn(day)) continue;
            persons.set(i, person.withDeath(day));
            setDirty();
            return;
        }
    }

    public int founderCount() {
        int count = 0;
        for (Person person : persons) if (person.founder()) count++;
        return count;
    }

    public int descendantCount() {
        return persons.size() - founderCount();
    }

    public int livingCount(long day) {
        int count = 0;
        for (Person person : persons) if (person.aliveOn(day)) count++;
        return count;
    }

    public int retiredCount(long day) {
        int count = 0;
        for (Person person : persons) {
            if (person.aliveOn(day) && person.retiredOn(day)) count++;
        }
        return count;
    }

    public int birthCount() {
        int count = 0;
        for (HouseholdLine line : householdLines) count += line.birthCount();
        return count;
    }

    public int successionCount() {
        int count = 0;
        for (HouseholdLine line : householdLines) count += line.successionCount();
        return count;
    }
}
