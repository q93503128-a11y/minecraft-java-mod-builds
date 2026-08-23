package kr.moonseungjun.survivalascension.production;

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

public final class ProductionData extends SavedData {
    public static final int MAX_BUFFER = 3;
    public static final int MAX_SUPPLY_CHARGES = 3;

    private record PlayerEntry(String uuid, int metalworks, int timberworks, int provisions, int precision,
                               int cycles, int supplyCharges) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.INT.optionalFieldOf("metalworks", 0).forGetter(PlayerEntry::metalworks),
                Codec.INT.optionalFieldOf("timberworks", 0).forGetter(PlayerEntry::timberworks),
                Codec.INT.optionalFieldOf("provisions", 0).forGetter(PlayerEntry::provisions),
                Codec.INT.optionalFieldOf("precision", 0).forGetter(PlayerEntry::precision),
                Codec.INT.optionalFieldOf("cycles", 0).forGetter(PlayerEntry::cycles),
                Codec.INT.optionalFieldOf("supply_charges", 0).forGetter(PlayerEntry::supplyCharges)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<ProductionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "production_v1"),
            ProductionData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ProductionData::entries)
            ).apply(instance, ProductionData::new))
    );

    private static final class State {
        int metalworks;
        int timberworks;
        int provisions;
        int precision;
        int cycles;
        int supplyCharges;

        State(int metalworks, int timberworks, int provisions, int precision, int cycles, int supplyCharges) {
            this.metalworks = clampBuffer(metalworks);
            this.timberworks = clampBuffer(timberworks);
            this.provisions = clampBuffer(provisions);
            this.precision = clampBuffer(precision);
            this.cycles = Math.max(0, cycles);
            this.supplyCharges = Math.max(0, Math.min(MAX_SUPPLY_CHARGES, supplyCharges));
            normalizeCycles(this);
        }
    }

    private final Map<String, State> players = new HashMap<>();

    public ProductionData() {}

    private ProductionData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            players.put(entry.uuid(), new State(entry.metalworks(), entry.timberworks(), entry.provisions(),
                    entry.precision(), entry.cycles(), entry.supplyCharges()));
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> out = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> out.add(new PlayerEntry(uuid, state.metalworks, state.timberworks,
                state.provisions, state.precision, state.cycles, state.supplyCharges)));
        return out;
    }

    public static ProductionData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static ProductionData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    private State state(ServerPlayer player) {
        String key = player.getUUID().toString();
        State state = players.get(key);
        if (state == null) {
            state = new State(0, 0, 0, 0, 0, 0);
            players.put(key, state);
            setDirty();
        }
        return state;
    }

    public int buffer(ServerPlayer player, ProductionProgram program) {
        State state = state(player);
        return switch (program) {
            case METALWORKS -> state.metalworks;
            case TIMBERWORKS -> state.timberworks;
            case PROVISIONS -> state.provisions;
            case PRECISION -> state.precision;
        };
    }

    public boolean canAccept(ServerPlayer player, ProductionProgram program) {
        return buffer(player, program) < MAX_BUFFER;
    }

    public BatchResult addBatch(ServerPlayer player, ProductionProgram program) {
        State state = state(player);
        if (buffer(player, program) >= MAX_BUFFER) return new BatchResult(false, false, state.supplyCharges);
        switch (program) {
            case METALWORKS -> state.metalworks++;
            case TIMBERWORKS -> state.timberworks++;
            case PROVISIONS -> state.provisions++;
            case PRECISION -> state.precision++;
        }
        boolean cycleCompleted = normalizeCycles(state) > 0;
        setDirty();
        return new BatchResult(true, cycleCompleted, state.supplyCharges);
    }

    public int cycles(ServerPlayer player) { return state(player).cycles; }
    public int supplyCharges(ServerPlayer player) { return state(player).supplyCharges; }

    public boolean consumeSupplyCharge(ServerPlayer player) {
        State state = state(player);
        if (state.supplyCharges <= 0) return false;
        state.supplyCharges--;
        normalizeCycles(state);
        setDirty();
        return true;
    }

    private static int normalizeCycles(State state) {
        int completed = 0;
        while (state.supplyCharges < MAX_SUPPLY_CHARGES && state.metalworks > 0 && state.timberworks > 0
                && state.provisions > 0 && state.precision > 0) {
            state.metalworks--;
            state.timberworks--;
            state.provisions--;
            state.precision--;
            state.cycles++;
            state.supplyCharges++;
            completed++;
        }
        return completed;
    }

    private static int clampBuffer(int value) { return Math.max(0, Math.min(MAX_BUFFER, value)); }

    public record BatchResult(boolean accepted, boolean cycleCompleted, int supplyCharges) {}
}
