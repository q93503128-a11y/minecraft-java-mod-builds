package kr.moonseungjun.survivalascension.expedition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.BlockPos;
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

public final class ExpeditionOperationData extends SavedData {
    private static final int ALL_REGIONS_MASK = (1 << ExpeditionRegion.values().length) - 1;

    private record PlayerEntry(String uuid, String activeRegion, String dimension, int x, int y, int z,
                               long deadline, boolean rangeReached, int progressA, int progressB,
                               int completedMask, int totalCompletions, boolean masteryClaimed) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.STRING.optionalFieldOf("active_region", "").forGetter(PlayerEntry::activeRegion),
                Codec.STRING.optionalFieldOf("dimension", "").forGetter(PlayerEntry::dimension),
                Codec.INT.optionalFieldOf("x", 0).forGetter(PlayerEntry::x),
                Codec.INT.optionalFieldOf("y", 0).forGetter(PlayerEntry::y),
                Codec.INT.optionalFieldOf("z", 0).forGetter(PlayerEntry::z),
                Codec.LONG.optionalFieldOf("deadline", 0L).forGetter(PlayerEntry::deadline),
                Codec.BOOL.optionalFieldOf("range_reached", false).forGetter(PlayerEntry::rangeReached),
                Codec.INT.optionalFieldOf("progress_a", 0).forGetter(PlayerEntry::progressA),
                Codec.INT.optionalFieldOf("progress_b", 0).forGetter(PlayerEntry::progressB),
                Codec.INT.optionalFieldOf("completed_mask", 0).forGetter(PlayerEntry::completedMask),
                Codec.INT.optionalFieldOf("total_completions", 0).forGetter(PlayerEntry::totalCompletions),
                Codec.BOOL.optionalFieldOf("mastery_claimed", false).forGetter(PlayerEntry::masteryClaimed)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<ExpeditionOperationData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_operations_v1"),
            ExpeditionOperationData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ExpeditionOperationData::entries)
            ).apply(instance, ExpeditionOperationData::new))
    );

    private static final class State {
        ExpeditionRegion activeRegion;
        String dimension = "";
        BlockPos anchor = BlockPos.ZERO;
        long deadline;
        boolean rangeReached;
        int progressA;
        int progressB;
        int completedMask;
        int totalCompletions;
        boolean masteryClaimed;

        State(PlayerEntry entry) {
            try { this.activeRegion = entry.activeRegion().isEmpty() ? null : ExpeditionRegion.valueOf(entry.activeRegion()); }
            catch (IllegalArgumentException ignored) { this.activeRegion = null; }
            if (this.activeRegion != null && !entry.dimension().isEmpty() && entry.deadline() > 0L) {
                this.dimension = entry.dimension();
                this.anchor = new BlockPos(entry.x(), entry.y(), entry.z());
                this.deadline = entry.deadline();
                this.rangeReached = entry.rangeReached();
                ExpeditionOperation operation = ExpeditionOperation.forRegion(this.activeRegion);
                this.progressA = clamp(entry.progressA(), operation.tasks().get(0).target());
                this.progressB = clamp(entry.progressB(), operation.tasks().get(1).target());
            } else {
                clearActive();
            }
            this.completedMask = entry.completedMask() & ALL_REGIONS_MASK;
            this.totalCompletions = Math.max(0, entry.totalCompletions());
            this.masteryClaimed = entry.masteryClaimed() || this.completedMask == ALL_REGIONS_MASK;
        }

        State() {}

        void clearActive() {
            activeRegion = null;
            dimension = "";
            anchor = BlockPos.ZERO;
            deadline = 0L;
            rangeReached = false;
            progressA = 0;
            progressB = 0;
        }
    }

    private final Map<String, State> players = new HashMap<>();

    public ExpeditionOperationData() {}

    private ExpeditionOperationData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) players.put(entry.uuid(), new State(entry));
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> out.add(new PlayerEntry(
                uuid,
                state.activeRegion == null ? "" : state.activeRegion.name(),
                state.dimension,
                state.anchor.getX(), state.anchor.getY(), state.anchor.getZ(),
                state.deadline, state.rangeReached, state.progressA, state.progressB,
                state.completedMask, state.totalCompletions, state.masteryClaimed
        )));
        return out;
    }

    public static ExpeditionOperationData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static ExpeditionOperationData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private State state(ServerPlayer player) {
        return players.computeIfAbsent(player.getUUID().toString(), ignored -> new State());
    }

    public ActiveOperation active(ServerPlayer player) {
        State state = state(player);
        if (state.activeRegion == null) return null;
        return new ActiveOperation(state.activeRegion, state.dimension, state.anchor, state.deadline,
                state.rangeReached, state.progressA, state.progressB);
    }

    public boolean start(ServerPlayer player, ExpeditionOperation operation, String dimension, BlockPos anchor, long deadline) {
        State state = state(player);
        if (state.activeRegion != null || deadline <= 0L) return false;
        state.activeRegion = operation.region();
        state.dimension = dimension;
        state.anchor = anchor.immutable();
        state.deadline = deadline;
        state.rangeReached = false;
        state.progressA = 0;
        state.progressB = 0;
        setDirty();
        return true;
    }

    public boolean markRangeReached(ServerPlayer player) {
        State state = state(player);
        if (state.activeRegion == null || state.rangeReached) return false;
        state.rangeReached = true;
        setDirty();
        return true;
    }

    public ProgressResult addProgress(ServerPlayer player, int taskIndex, int amount, int target) {
        State state = state(player);
        if (state.activeRegion == null || amount <= 0 || target <= 0) return new ProgressResult(0, 0, false);
        int oldValue = taskIndex == 0 ? state.progressA : state.progressB;
        int newValue = Math.min(target, oldValue + amount);
        if (taskIndex == 0) state.progressA = newValue;
        else state.progressB = newValue;
        if (newValue != oldValue) setDirty();
        return new ProgressResult(oldValue, newValue, oldValue < target && newValue >= target);
    }

    public boolean objectivesComplete(ServerPlayer player, ExpeditionOperation operation) {
        State state = state(player);
        return state.activeRegion == operation.region()
                && state.progressA >= operation.tasks().get(0).target()
                && state.progressB >= operation.tasks().get(1).target();
    }

    public CompletionResult complete(ServerPlayer player, ExpeditionRegion region) {
        State state = state(player);
        boolean firstRegion = (state.completedMask & region.bit()) == 0;
        if (firstRegion) state.completedMask |= region.bit();
        state.totalCompletions++;
        boolean masteryNow = state.completedMask == ALL_REGIONS_MASK && !state.masteryClaimed;
        if (masteryNow) state.masteryClaimed = true;
        state.clearActive();
        setDirty();
        return new CompletionResult(firstRegion, masteryNow, Integer.bitCount(state.completedMask), state.totalCompletions);
    }

    public boolean fail(ServerPlayer player) {
        State state = state(player);
        if (state.activeRegion == null) return false;
        state.clearActive();
        setDirty();
        return true;
    }

    public int uniqueCompleted(ServerPlayer player) { return Integer.bitCount(state(player).completedMask); }
    public int totalCompletions(ServerPlayer player) { return state(player).totalCompletions; }
    public boolean masteryClaimed(ServerPlayer player) { return state(player).masteryClaimed; }

    private static int clamp(int value, int max) { return Math.max(0, Math.min(max, value)); }

    public record ActiveOperation(ExpeditionRegion region, String dimension, BlockPos anchor, long deadline,
                                  boolean rangeReached, int progressA, int progressB) {}
    public record ProgressResult(int oldProgress, int newProgress, boolean taskCompletedNow) {}
    public record CompletionResult(boolean firstRegion, boolean masteryNow, int uniqueCompleted, int totalCompletions) {}
}
