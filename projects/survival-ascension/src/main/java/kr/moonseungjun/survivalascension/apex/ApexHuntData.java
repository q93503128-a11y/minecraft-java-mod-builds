package kr.moonseungjun.survivalascension.apex;

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

public final class ApexHuntData extends SavedData {
    private static final int ALL_APEX_MASK = (1 << ApexArchetype.values().length) - 1;

    private record PlayerEntry(String uuid, int defeatedMask, int victories, boolean masteryClaimed) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.INT.optionalFieldOf("defeated", 0).forGetter(PlayerEntry::defeatedMask),
                Codec.INT.optionalFieldOf("victories", 0).forGetter(PlayerEntry::victories),
                Codec.BOOL.optionalFieldOf("mastery_claimed", false).forGetter(PlayerEntry::masteryClaimed)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<ApexHuntData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "apex_hunt_v1"),
            ApexHuntData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ApexHuntData::entries)
            ).apply(instance, ApexHuntData::new))
    );

    private static final class State {
        int defeatedMask;
        int victories;
        boolean masteryClaimed;

        State(int defeatedMask, int victories, boolean masteryClaimed) {
            this.defeatedMask = defeatedMask & ALL_APEX_MASK;
            this.victories = Math.max(0, victories);
            this.masteryClaimed = masteryClaimed;
        }
    }

    private final Map<String, State> players = new HashMap<>();

    public ApexHuntData() {}

    private ApexHuntData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            players.put(entry.uuid(), new State(entry.defeatedMask(), entry.victories(), entry.masteryClaimed()));
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> out.add(new PlayerEntry(
                uuid, state.defeatedMask, state.victories, state.masteryClaimed)));
        return out;
    }

    public static ApexHuntData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static ApexHuntData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private State state(ServerPlayer player) {
        String key = player.getUUID().toString();
        State state = players.get(key);
        if (state == null) {
            state = new State(0, 0, false);
            players.put(key, state);
            setDirty();
        }
        return state;
    }

    public boolean recordVictory(ServerPlayer player, ApexArchetype archetype) {
        State state = state(player);
        int bit = 1 << archetype.ordinal();
        boolean first = (state.defeatedMask & bit) == 0;
        state.defeatedMask |= bit;
        state.victories++;
        setDirty();
        return first;
    }

    public boolean hasDefeated(ServerPlayer player, ApexArchetype archetype) {
        return (state(player).defeatedMask & (1 << archetype.ordinal())) != 0;
    }

    public int uniqueDefeated(ServerPlayer player) { return Integer.bitCount(state(player).defeatedMask); }
    public int victories(ServerPlayer player) { return state(player).victories; }
    public boolean allDefeated(ServerPlayer player) { return state(player).defeatedMask == ALL_APEX_MASK; }

    public boolean claimMasteryReward(ServerPlayer player) {
        State state = state(player);
        if (!allDefeated(player) || state.masteryClaimed) return false;
        state.masteryClaimed = true;
        setDirty();
        return true;
    }

    public boolean masteryClaimed(ServerPlayer player) { return state(player).masteryClaimed; }
}
