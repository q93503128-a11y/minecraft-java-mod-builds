package kr.moonseungjun.livingkingdoms.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent, action-driven mastery progression.
 *
 * <p>Mastery levels have no authored maximum. Required experience grows quadratically, while
 * gameplay bonuses use logarithmic scaling so long-running worlds can continue progressing without
 * early-game values becoming unstable.</p>
 */
public final class MasteryProgressionSavedData extends SavedData {
    public static final String COMBAT = "combat";
    public static final String DEFENSE = "defense";
    public static final String MINING = "mining";
    public static final String LOGGING = "logging";
    public static final String FARMING = "farming";
    public static final String GATHERING = "gathering";
    public static final String EXPLORATION = "exploration";

    private static final long BASE_LEVEL_COST = 18L;

    private static final Codec<MasteryState> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                    .optionalFieldOf("xp", Map.of())
                    .forGetter(state -> Map.copyOf(state.xp()))
    ).apply(instance, MasteryState::new));

    private static final Codec<MasteryProgressionSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, STATE_CODEC)
                    .optionalFieldOf("players", Map.of())
                    .forGetter(data -> Map.copyOf(data.states))
    ).apply(instance, MasteryProgressionSavedData::new));

    public static final SavedDataType<MasteryProgressionSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "mastery_progression"),
            level -> new MasteryProgressionSavedData(),
            level -> CODEC
    );

    private final Map<String, MasteryState> states;

    public MasteryProgressionSavedData() {
        this(Map.of());
    }

    private MasteryProgressionSavedData(Map<String, MasteryState> states) {
        this.states = new LinkedHashMap<>(states);
    }

    public MasteryState state(UUID playerId) {
        return states.computeIfAbsent(playerId.toString(), ignored -> new MasteryState(Map.of()));
    }

    public MasteryState add(UUID playerId, String track, long amount) {
        if (amount <= 0 || track == null || track.isBlank()) return state(playerId);
        MasteryState current = state(playerId);
        Map<String, Long> xp = new LinkedHashMap<>(current.xp());
        long previous = Math.max(0L, xp.getOrDefault(track, 0L));
        long next = previous > Long.MAX_VALUE - amount ? Long.MAX_VALUE : previous + amount;
        xp.put(track, next);
        MasteryState updated = new MasteryState(xp);
        states.put(playerId.toString(), updated);
        setDirty();
        return updated;
    }

    public long xp(UUID playerId, String track) {
        return Math.max(0L, state(playerId).xp().getOrDefault(track, 0L));
    }

    public int level(UUID playerId, String track) {
        return levelForXp(xp(playerId, track));
    }

    public static int levelForXp(long xp) {
        if (xp <= 0L) return 0;
        double value = Math.sqrt(xp / (double) BASE_LEVEL_COST);
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(0, (int) Math.floor(value));
    }

    public static long xpForLevel(int level) {
        if (level <= 0) return 0L;
        long safe = Math.min(level, 715_827_882);
        if (safe > Math.sqrt(Long.MAX_VALUE / (double) BASE_LEVEL_COST)) return Long.MAX_VALUE;
        return BASE_LEVEL_COST * safe * safe;
    }

    public static float progress(long xp) {
        int level = levelForXp(xp);
        if (level == Integer.MAX_VALUE) return 1.0F;
        long start = xpForLevel(level);
        long end = xpForLevel(level + 1);
        if (end <= start) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, (xp - start) / (float) (end - start)));
    }

    public static String displayName(String track) {
        return switch (track) {
            case COMBAT -> "전투 숙련";
            case DEFENSE -> "방어 숙련";
            case MINING -> "채광 숙련";
            case LOGGING -> "벌목 숙련";
            case FARMING -> "농사 숙련";
            case GATHERING -> "채집 숙련";
            case EXPLORATION -> "탐험 숙련";
            default -> track;
        };
    }

    public record MasteryState(Map<String, Long> xp) {
        public MasteryState {
            Map<String, Long> sanitized = new LinkedHashMap<>();
            xp.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && value > 0L) {
                    sanitized.put(key, value);
                }
            });
            xp = Map.copyOf(sanitized);
        }
    }
}
