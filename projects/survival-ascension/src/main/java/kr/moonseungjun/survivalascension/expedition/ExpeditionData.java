package kr.moonseungjun.survivalascension.expedition;

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
import java.util.Locale;
import java.util.Map;

public final class ExpeditionData extends SavedData {
    public static final int MILESTONE_OVERWORLD = 1;
    public static final int MILESTONE_LEGENDARY = 1 << 1;
    public static final int MILESTONE_MASTER = 1 << 2;
    private static final int ALL_REGIONS_MASK = (1 << ExpeditionRegion.values().length) - 1;
    private static final Codec<Map<String, Integer>> PROGRESS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    private record PlayerEntry(String uuid, int discoveredMask, int completedMask,
                               Map<String, Integer> progress, int regionRewardMask, int milestoneMask) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.INT.optionalFieldOf("discovered", 0).forGetter(PlayerEntry::discoveredMask),
                Codec.INT.optionalFieldOf("completed", 0).forGetter(PlayerEntry::completedMask),
                PROGRESS_CODEC.optionalFieldOf("progress", Map.of()).forGetter(PlayerEntry::progress),
                Codec.INT.optionalFieldOf("region_rewards", -1).forGetter(PlayerEntry::regionRewardMask),
                Codec.INT.optionalFieldOf("milestones", 0).forGetter(PlayerEntry::milestoneMask)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<ExpeditionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_v1"),
            ExpeditionData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ExpeditionData::entries)
            ).apply(instance, ExpeditionData::new))
    );

    private static final class State {
        int discoveredMask;
        int completedMask;
        int regionRewardMask;
        int milestoneMask;
        final Map<String, Integer> progress = new HashMap<>();

        State(int discoveredMask, int completedMask, Map<String, Integer> progress,
              int regionRewardMask, int milestoneMask) {
            this.discoveredMask = discoveredMask & ALL_REGIONS_MASK;
            this.milestoneMask = milestoneMask & (MILESTONE_OVERWORLD | MILESTONE_LEGENDARY | MILESTONE_MASTER);
            this.regionRewardMask = regionRewardMask < 0
                    ? this.discoveredMask
                    : regionRewardMask & ALL_REGIONS_MASK;
            int migratedCompleted = completedMask & ALL_REGIONS_MASK;
            if ((this.milestoneMask & MILESTONE_MASTER) != 0) migratedCompleted = ALL_REGIONS_MASK;
            this.completedMask = migratedCompleted;
            progress.forEach((key, value) -> this.progress.put(key, Math.max(0, value)));
            for (ExpeditionRegion region : ExpeditionRegion.values()) {
                if ((this.completedMask & region.bit()) != 0) {
                    this.discoveredMask |= region.bit();
                    this.progress.put(progressKey(region), region.objectiveTarget());
                }
            }
        }
    }

    public record ProgressResult(int oldProgress, int newProgress, boolean completedNow) {}

    private final Map<String, State> players = new HashMap<>();

    public ExpeditionData() {}

    private ExpeditionData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            players.put(entry.uuid(), new State(entry.discoveredMask(), entry.completedMask(), entry.progress(),
                    entry.regionRewardMask(), entry.milestoneMask()));
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> out.add(new PlayerEntry(
                uuid, state.discoveredMask, state.completedMask, Map.copyOf(state.progress),
                state.regionRewardMask, state.milestoneMask)));
        return out;
    }

    public static ExpeditionData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static ExpeditionData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private State state(ServerPlayer player) {
        String key = player.getUUID().toString();
        State state = players.get(key);
        if (state == null) {
            state = new State(0, 0, Map.of(), 0, 0);
            players.put(key, state);
            setDirty();
        }
        return state;
    }

    public boolean discover(ServerPlayer player, ExpeditionRegion region) {
        State state = state(player);
        int bit = region.bit();
        if ((state.discoveredMask & bit) != 0) return false;
        state.discoveredMask |= bit;
        setDirty();
        return true;
    }

    public boolean isDiscovered(ServerPlayer player, ExpeditionRegion region) {
        return (state(player).discoveredMask & region.bit()) != 0;
    }

    public boolean isComplete(ServerPlayer player, ExpeditionRegion region) {
        return (state(player).completedMask & region.bit()) != 0;
    }

    public boolean has(ServerPlayer player, ExpeditionRegion region) { return isDiscovered(player, region); }

    public int progress(ServerPlayer player, ExpeditionRegion region) {
        return Math.min(region.objectiveTarget(), state(player).progress.getOrDefault(progressKey(region), 0));
    }

    public ProgressResult addProgress(ServerPlayer player, ExpeditionRegion region, int amount) {
        State state = state(player);
        if (amount <= 0 || (state.completedMask & region.bit()) != 0 || (state.discoveredMask & region.bit()) == 0) {
            int current = progress(player, region);
            return new ProgressResult(current, current, false);
        }
        String key = progressKey(region);
        int oldProgress = Math.min(region.objectiveTarget(), state.progress.getOrDefault(key, 0));
        int newProgress = Math.min(region.objectiveTarget(), oldProgress + amount);
        if (newProgress == oldProgress) return new ProgressResult(oldProgress, newProgress, false);
        state.progress.put(key, newProgress);
        boolean completedNow = newProgress >= region.objectiveTarget();
        if (completedNow) state.completedMask |= region.bit();
        setDirty();
        return new ProgressResult(oldProgress, newProgress, completedNow);
    }

    public boolean claimRegionReward(ServerPlayer player, ExpeditionRegion region) {
        State state = state(player);
        if ((state.regionRewardMask & region.bit()) != 0) return false;
        state.regionRewardMask |= region.bit();
        setDirty();
        return true;
    }

    public int count(ServerPlayer player) { return Integer.bitCount(state(player).discoveredMask); }
    public int countCompleted(ServerPlayer player) { return Integer.bitCount(state(player).completedMask); }

    public int countStageZeroCompleted(ServerPlayer player) {
        int count = 0;
        State state = state(player);
        for (ExpeditionRegion region : ExpeditionRegion.values()) {
            if (region.requiredWorldStage() == 0 && (state.completedMask & region.bit()) != 0) count++;
        }
        return count;
    }

    public boolean isMasterSurveyComplete(ServerPlayer player) {
        return (state(player).completedMask & ALL_REGIONS_MASK) == ALL_REGIONS_MASK;
    }

    public boolean claimMilestone(ServerPlayer player, int milestone) {
        State state = state(player);
        if ((state.milestoneMask & milestone) != 0) return false;
        state.milestoneMask |= milestone;
        setDirty();
        return true;
    }

    public boolean milestoneClaimed(ServerPlayer player, int milestone) {
        return (state(player).milestoneMask & milestone) != 0;
    }

    public String summary(ServerPlayer player) {
        StringBuilder out = new StringBuilder();
        for (ExpeditionRegion region : ExpeditionRegion.values()) {
            if (!out.isEmpty()) out.append(" · ");
            if (isComplete(player, region)) out.append("§a✓");
            else if (isDiscovered(player, region)) out.append("§e◐");
            else out.append("§8·");
            out.append(region.koreanName());
        }
        return out.toString();
    }

    private static String progressKey(ExpeditionRegion region) {
        return region.name().toLowerCase(Locale.ROOT);
    }
}
