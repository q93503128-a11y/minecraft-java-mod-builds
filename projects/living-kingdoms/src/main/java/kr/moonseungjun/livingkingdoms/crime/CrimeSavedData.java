package kr.moonseungjun.livingkingdoms.crime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CrimeSavedData extends SavedData {
    private static final Codec<CrimeRecord> RECORD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("wanted", 0).forGetter(CrimeRecord::wanted),
            Codec.LONG.optionalFieldOf("last_crime_tick", 0L).forGetter(CrimeRecord::lastCrimeTick),
            Codec.STRING.optionalFieldOf("jurisdiction", "wilderness").forGetter(CrimeRecord::jurisdiction),
            Codec.INT.optionalFieldOf("resistance", 0).forGetter(CrimeRecord::resistance),
            Codec.INT.optionalFieldOf("arrest_ticks", 0).forGetter(CrimeRecord::arrestTicks)
    ).apply(instance, CrimeRecord::new));

    private static final Codec<CrimeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, RECORD_CODEC)
                    .optionalFieldOf("players", Map.of())
                    .forGetter(data -> Map.copyOf(data.records))
    ).apply(instance, CrimeSavedData::new));

    public static final SavedDataType<CrimeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "crime_records"),
            level -> new CrimeSavedData(),
            level -> CODEC
    );

    private final Map<String, CrimeRecord> records;

    public CrimeSavedData() {
        this(Map.of());
    }

    private CrimeSavedData(Map<String, CrimeRecord> records) {
        this.records = new LinkedHashMap<>(records);
    }

    public CrimeRecord record(UUID playerId) {
        return records.getOrDefault(playerId.toString(), CrimeRecord.CLEAN);
    }

    public CrimeRecord addCrime(UUID playerId, String jurisdiction, int severity, long gameTick) {
        CrimeRecord old = record(playerId);
        CrimeRecord next = new CrimeRecord(
                Math.min(100, old.wanted() + Math.max(1, severity)),
                gameTick,
                jurisdiction,
                old.resistance(),
                0
        );
        records.put(playerId.toString(), next);
        setDirty();
        return next;
    }

    public CrimeRecord addResistance(UUID playerId, long gameTick) {
        CrimeRecord old = record(playerId);
        CrimeRecord next = new CrimeRecord(
                Math.min(100, old.wanted() + 2),
                gameTick,
                old.jurisdiction(),
                Math.min(20, old.resistance() + 1),
                0
        );
        records.put(playerId.toString(), next);
        setDirty();
        return next;
    }

    public void setArrestTicks(UUID playerId, int ticks) {
        CrimeRecord old = record(playerId);
        CrimeRecord next = new CrimeRecord(old.wanted(), old.lastCrimeTick(), old.jurisdiction(),
                old.resistance(), Math.max(0, ticks));
        records.put(playerId.toString(), next);
        setDirty();
    }

    public void settleAfterArrest(UUID playerId) {
        CrimeRecord old = record(playerId);
        int remaining = Math.max(0, old.wanted() / 4);
        records.put(playerId.toString(), new CrimeRecord(remaining, old.lastCrimeTick(),
                old.jurisdiction(), 0, 0));
        setDirty();
    }

    public record CrimeRecord(int wanted, long lastCrimeTick, String jurisdiction, int resistance, int arrestTicks) {
        private static final CrimeRecord CLEAN = new CrimeRecord(0, 0L, "wilderness", 0, 0);

        public boolean wantedHere(String jurisdictionId) {
            return wanted > 0 && jurisdiction.equals(jurisdictionId);
        }
    }
}
