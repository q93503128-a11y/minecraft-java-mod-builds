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
    private static final Codec<Map<String, Integer>> INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    private record PlayerEntry(String uuid, int discoveredMask, int completedMask,
                               Map<String, Integer> progress, Map<String, Integer> directives,
                               int regionRewardMask, int incidentRewardMask, int milestoneMask) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.INT.optionalFieldOf("discovered", 0).forGetter(PlayerEntry::discoveredMask),
                Codec.INT.optionalFieldOf("completed", 0).forGetter(PlayerEntry::completedMask),
                INT_MAP_CODEC.optionalFieldOf("progress", Map.of()).forGetter(PlayerEntry::progress),
                INT_MAP_CODEC.optionalFieldOf("directives", Map.of()).forGetter(PlayerEntry::directives),
                Codec.INT.optionalFieldOf("region_rewards", -1).forGetter(PlayerEntry::regionRewardMask),
                Codec.INT.optionalFieldOf("incident_rewards", 0).forGetter(PlayerEntry::incidentRewardMask),
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
        int incidentRewardMask;
        int milestoneMask;
        final Map<String, Integer> progress = new HashMap<>();
        final Map<String, Integer> directives = new HashMap<>();

        State(int discoveredMask, int completedMask, Map<String, Integer> progress,
              Map<String, Integer> directives, int regionRewardMask, int incidentRewardMask, int milestoneMask) {
            this.discoveredMask = discoveredMask & ALL_REGIONS_MASK;
            this.milestoneMask = milestoneMask & (MILESTONE_OVERWORLD | MILESTONE_LEGENDARY | MILESTONE_MASTER);
            this.regionRewardMask = regionRewardMask < 0
                    ? this.discoveredMask
                    : regionRewardMask & ALL_REGIONS_MASK;
            this.incidentRewardMask = incidentRewardMask & ALL_REGIONS_MASK;
            int migratedCompleted = completedMask & ALL_REGIONS_MASK;
            if ((this.milestoneMask & MILESTONE_MASTER) != 0) migratedCompleted = ALL_REGIONS_MASK;
            this.completedMask = migratedCompleted;
            progress.forEach((key, value) -> this.progress.put(key, Math.max(0, value)));
            directives.forEach((key, value) -> this.directives.put(key, Math.max(0, value)));

            for (ExpeditionRegion region : ExpeditionRegion.values()) {
                if ((this.discoveredMask & region.bit()) != 0) {
                    this.directives.putIfAbsent(regionKey(region), 0);
                    int legacy = this.progress.getOrDefault(legacyProgressKey(region), 0);
                    if (legacy > 0) {
                        ExpeditionDirective standard = ExpeditionDirective.select(region, 0);
                        ExpeditionDirective.Task first = standard.tasks().getFirst();
                        this.progress.putIfAbsent(taskProgressKey(region, first.action()), Math.min(first.target(), legacy));
                    }
                }
                if ((this.completedMask & region.bit()) != 0) {
                    this.discoveredMask |= region.bit();
                    this.directives.putIfAbsent(regionKey(region), 0);
                    ExpeditionDirective directive = ExpeditionDirective.select(region, this.directives.get(regionKey(region)));
                    for (ExpeditionDirective.Task task : directive.tasks()) {
                        this.progress.put(taskProgressKey(region, task.action()), task.target());
                    }
                }
            }
        }
    }

    public record ProgressResult(ExpeditionAction action, int oldProgress, int newProgress, int target,
                                 boolean taskCompletedNow, boolean regionCompletedNow) {}

    private final Map<String, State> players = new HashMap<>();

    public ExpeditionData() {}

    private ExpeditionData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            players.put(entry.uuid(), new State(entry.discoveredMask(), entry.completedMask(), entry.progress(),
                    entry.directives(), entry.regionRewardMask(), entry.incidentRewardMask(), entry.milestoneMask()));
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> out.add(new PlayerEntry(
                uuid, state.discoveredMask, state.completedMask, Map.copyOf(state.progress), Map.copyOf(state.directives),
                state.regionRewardMask, state.incidentRewardMask, state.milestoneMask)));
        return out;
    }

    public static ExpeditionData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static ExpeditionData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private State state(ServerPlayer player) {
        String key = player.getUUID().toString();
        State state = players.get(key);
        if (state == null) {
            state = new State(0, 0, Map.of(), Map.of(), 0, 0, 0);
            players.put(key, state);
            setDirty();
        }
        return state;
    }

    public boolean discover(ServerPlayer player, ExpeditionRegion region, int directiveIndex) {
        State state = state(player);
        int bit = region.bit();
        if ((state.discoveredMask & bit) != 0) {
            if (!state.directives.containsKey(regionKey(region))) {
                state.directives.put(regionKey(region), Math.floorMod(directiveIndex, ExpeditionDirective.optionCount(region)));
                setDirty();
            }
            return false;
        }
        state.discoveredMask |= bit;
        state.directives.put(regionKey(region), Math.floorMod(directiveIndex, ExpeditionDirective.optionCount(region)));
        setDirty();
        return true;
    }

    public boolean discover(ServerPlayer player, ExpeditionRegion region) { return discover(player, region, 0); }

    public boolean isDiscovered(ServerPlayer player, ExpeditionRegion region) {
        return (state(player).discoveredMask & region.bit()) != 0;
    }

    public boolean isComplete(ServerPlayer player, ExpeditionRegion region) {
        return (state(player).completedMask & region.bit()) != 0;
    }

    public boolean has(ServerPlayer player, ExpeditionRegion region) { return isDiscovered(player, region); }

    public ExpeditionDirective directive(ServerPlayer player, ExpeditionRegion region) {
        State state = state(player);
        return ExpeditionDirective.select(region, state.directives.getOrDefault(regionKey(region), 0));
    }

    public int progress(ServerPlayer player, ExpeditionRegion region, ExpeditionAction action) {
        ExpeditionDirective.Task task = taskFor(directive(player, region), action);
        if (task == null) return 0;
        return Math.min(task.target(), state(player).progress.getOrDefault(taskProgressKey(region, action), 0));
    }

    public ProgressResult addProgress(ServerPlayer player, ExpeditionRegion region, ExpeditionAction action, int amount) {
        State state = state(player);
        ExpeditionDirective directive = directive(player, region);
        ExpeditionDirective.Task task = taskFor(directive, action);
        if (task == null || amount <= 0 || (state.completedMask & region.bit()) != 0 || (state.discoveredMask & region.bit()) == 0) {
            int current = task == null ? 0 : progress(player, region, action);
            int target = task == null ? 0 : task.target();
            return new ProgressResult(action, current, current, target, false, false);
        }

        String key = taskProgressKey(region, action);
        int oldProgress = Math.min(task.target(), state.progress.getOrDefault(key, 0));
        int newProgress = Math.min(task.target(), oldProgress + amount);
        if (newProgress == oldProgress) {
            return new ProgressResult(action, oldProgress, newProgress, task.target(), false, false);
        }
        state.progress.put(key, newProgress);
        boolean taskCompletedNow = oldProgress < task.target() && newProgress >= task.target();
        boolean regionCompletedNow = directiveComplete(state, region, directive);
        if (regionCompletedNow) state.completedMask |= region.bit();
        setDirty();
        return new ProgressResult(action, oldProgress, newProgress, task.target(), taskCompletedNow, regionCompletedNow);
    }

    public ExpeditionDirective.Task firstIncompleteTask(ServerPlayer player, ExpeditionRegion region) {
        State state = state(player);
        if ((state.completedMask & region.bit()) != 0) return null;
        ExpeditionDirective directive = directive(player, region);
        for (ExpeditionDirective.Task task : directive.tasks()) {
            if (state.progress.getOrDefault(taskProgressKey(region, task.action()), 0) < task.target()) return task;
        }
        return null;
    }

    public boolean claimRegionReward(ServerPlayer player, ExpeditionRegion region) {
        State state = state(player);
        if ((state.regionRewardMask & region.bit()) != 0) return false;
        state.regionRewardMask |= region.bit();
        setDirty();
        return true;
    }

    public boolean incidentResolved(ServerPlayer player, ExpeditionRegion region) {
        return (state(player).incidentRewardMask & region.bit()) != 0;
    }

    public boolean claimIncidentReward(ServerPlayer player, ExpeditionRegion region) {
        State state = state(player);
        if ((state.incidentRewardMask & region.bit()) != 0) return false;
        state.incidentRewardMask |= region.bit();
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

    public String directiveSummary(ServerPlayer player, ExpeditionRegion region) {
        ExpeditionDirective directive = directive(player, region);
        StringBuilder out = new StringBuilder(directive.koreanName()).append(" · ");
        for (int i = 0; i < directive.tasks().size(); i++) {
            if (i > 0) out.append(" + ");
            ExpeditionDirective.Task task = directive.tasks().get(i);
            out.append(task.action().koreanName()).append(" ")
                    .append(progress(player, region, task.action())).append("/").append(task.target());
        }
        return out.toString();
    }

    private static boolean directiveComplete(State state, ExpeditionRegion region, ExpeditionDirective directive) {
        for (ExpeditionDirective.Task task : directive.tasks()) {
            if (state.progress.getOrDefault(taskProgressKey(region, task.action()), 0) < task.target()) return false;
        }
        return true;
    }

    private static ExpeditionDirective.Task taskFor(ExpeditionDirective directive, ExpeditionAction action) {
        for (ExpeditionDirective.Task task : directive.tasks()) if (task.action() == action) return task;
        return null;
    }

    private static String regionKey(ExpeditionRegion region) { return region.name().toLowerCase(Locale.ROOT); }
    private static String legacyProgressKey(ExpeditionRegion region) { return regionKey(region); }
    private static String taskProgressKey(ExpeditionRegion region, ExpeditionAction action) {
        return regionKey(region) + "." + action.name().toLowerCase(Locale.ROOT);
    }
}
