package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Auxiliary SavedData for dynamic fishing-outpost waterfront works; never a resource ledger. */
public final class SettlementWaterfrontData extends SavedData {
    public static final SavedDataType<SettlementWaterfrontData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "waterfront_works"),
            SettlementWaterfrontData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    WaterfrontState.CODEC.listOf().optionalFieldOf("states", List.<WaterfrontState>of())
                            .forGetter(data -> data.states)
            ).apply(instance, SettlementWaterfrontData::new))
    );

    private List<WaterfrontState> states;

    public SettlementWaterfrontData() {
        this(List.of());
    }

    public SettlementWaterfrontData(List<WaterfrontState> states) {
        this.states = List.copyOf(states);
    }

    public static SettlementWaterfrontData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<WaterfrontState> states() {
        return states;
    }

    public WaterfrontState stateFor(int outpostId) {
        for (WaterfrontState state : states) if (state.outpostId() == outpostId) return state;
        return null;
    }

    public void replace(WaterfrontState replacement) {
        List<WaterfrontState> next = new ArrayList<>(states);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).outpostId() != replacement.outpostId()) continue;
            if (next.get(i).equals(replacement)) return;
            next.set(i, replacement);
            states = List.copyOf(next);
            setDirty();
            return;
        }
        next.add(replacement);
        states = List.copyOf(next);
        setDirty();
    }
}
