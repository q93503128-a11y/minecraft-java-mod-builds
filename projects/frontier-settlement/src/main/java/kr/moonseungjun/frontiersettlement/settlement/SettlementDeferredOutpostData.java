package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Separate auxiliary SavedData so Alpha.42 catch-up state does not mutate the settlement ledger. */
public final class SettlementDeferredOutpostData extends SavedData {
    public static final SavedDataType<SettlementDeferredOutpostData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "outpost_deferred_work"),
            SettlementDeferredOutpostData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    OutpostDeferredState.CODEC.listOf().optionalFieldOf("states", List.<OutpostDeferredState>of())
                            .forGetter(data -> data.states)
            ).apply(instance, SettlementDeferredOutpostData::new))
    );

    private List<OutpostDeferredState> states;

    public SettlementDeferredOutpostData() {
        this(List.of());
    }

    public SettlementDeferredOutpostData(List<OutpostDeferredState> states) {
        this.states = List.copyOf(states);
    }

    public static SettlementDeferredOutpostData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<OutpostDeferredState> states() {
        return states;
    }

    public OutpostDeferredState stateFor(int outpostId) {
        for (OutpostDeferredState state : states) if (state.outpostId() == outpostId) return state;
        return OutpostDeferredState.empty(outpostId);
    }

    public void replace(OutpostDeferredState replacement) {
        List<OutpostDeferredState> next = new ArrayList<>(states);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).outpostId() != replacement.outpostId()) continue;
            if (next.get(i).equals(replacement)) return;
            next.set(i, replacement);
            states = List.copyOf(next);
            setDirty();
            return;
        }
        if (replacement.equals(OutpostDeferredState.empty(replacement.outpostId()))) return;
        next.add(replacement);
        states = List.copyOf(next);
        setDirty();
    }
}
