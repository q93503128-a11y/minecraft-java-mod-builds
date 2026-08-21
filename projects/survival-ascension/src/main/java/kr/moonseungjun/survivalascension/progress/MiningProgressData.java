package kr.moonseungjun.survivalascension.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MiningProgressData extends SavedData {
    public static final int MAX_LEVEL = 100;

    private record PlayerEntry(String uuid, long miningXp, boolean introduced) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.LONG.optionalFieldOf("mining_xp", 0L).forGetter(PlayerEntry::miningXp),
                Codec.BOOL.optionalFieldOf("introduced", false).forGetter(PlayerEntry::introduced)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<MiningProgressData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mining_progress_v1"),
            MiningProgressData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of())
                            .forGetter(MiningProgressData::entries)
            ).apply(instance, MiningProgressData::new))
    );

    private static final class PlayerState {
        private long miningXp;
        private boolean introduced;

        private PlayerState(long miningXp, boolean introduced) {
            this.miningXp = Math.max(0L, miningXp);
            this.introduced = introduced;
        }
    }

    public record AddXpResult(int oldLevel, int newLevel, long oldXp, long newXp) {
        public boolean leveledUp() {
            return newLevel > oldLevel;
        }
    }

    private final Map<String, PlayerState> players = new HashMap<>();

    public MiningProgressData() {}

    private MiningProgressData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            players.put(entry.uuid(), new PlayerState(entry.miningXp(), entry.introduced()));
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> result = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> result.add(new PlayerEntry(uuid, state.miningXp, state.introduced)));
        return result;
    }

    public static MiningProgressData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public static MiningProgressData get(ServerPlayer player) {
        return get(((ServerLevel) player.level()).getServer());
    }

    public boolean ensureProfile(ServerPlayer player) {
        String key = player.getUUID().toString();
        if (players.containsKey(key)) return false;
        players.put(key, new PlayerState(0L, false));
        setDirty();
        return true;
    }

    private PlayerState state(ServerPlayer player) {
        ensureProfile(player);
        return players.get(player.getUUID().toString());
    }

    public long miningXp(ServerPlayer player) {
        return state(player).miningXp;
    }

    public int miningLevel(ServerPlayer player) {
        return levelFromXp(miningXp(player));
    }

    public boolean markIntroduced(ServerPlayer player) {
        PlayerState state = state(player);
        if (state.introduced) return false;
        state.introduced = true;
        setDirty();
        return true;
    }

    public AddXpResult addMiningXp(ServerPlayer player, long amount) {
        PlayerState state = state(player);
        long oldXp = state.miningXp;
        int oldLevel = levelFromXp(oldXp);
        if (amount <= 0 || oldLevel >= MAX_LEVEL) return new AddXpResult(oldLevel, oldLevel, oldXp, oldXp);
        long cap = xpAtLevel(MAX_LEVEL);
        state.miningXp = Math.min(cap, oldXp + amount);
        int newLevel = levelFromXp(state.miningXp);
        setDirty();
        return new AddXpResult(oldLevel, newLevel, oldXp, state.miningXp);
    }

    public void setMiningLevel(ServerPlayer player, int level) {
        int clamped = Math.max(0, Math.min(MAX_LEVEL, level));
        PlayerState state = state(player);
        state.miningXp = xpAtLevel(clamped);
        setDirty();
    }

    public static int levelFromXp(long xp) {
        long safeXp = Math.max(0L, xp);
        int level = 0;
        long threshold = 0L;
        while (level < MAX_LEVEL) {
            long next = xpForNextLevel(level);
            if (safeXp < threshold + next) break;
            threshold += next;
            level++;
        }
        return level;
    }

    public static long xpAtLevel(int targetLevel) {
        int clamped = Math.max(0, Math.min(MAX_LEVEL, targetLevel));
        long total = 0L;
        for (int level = 0; level < clamped; level++) total += xpForNextLevel(level);
        return total;
    }

    public static long xpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0L;
        int level = Math.max(0, currentLevel);
        return 40L + 8L * level + Math.round(1.5D * level * level);
    }

    public static long xpIntoLevel(long totalXp) {
        int level = levelFromXp(totalXp);
        return Math.max(0L, totalXp - xpAtLevel(level));
    }
}
