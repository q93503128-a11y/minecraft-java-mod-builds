package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Persistent storm load and the bounded set of water cells created by Erden's drainage simulation. */
public final class ErdenDrainageSavedData extends SavedData {
    public record FloodCell(int x, int y, int z, int drainX, int drainY, int drainZ) {
        private static final Codec<FloodCell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(FloodCell::x),
                Codec.INT.fieldOf("y").forGetter(FloodCell::y),
                Codec.INT.fieldOf("z").forGetter(FloodCell::z),
                Codec.INT.fieldOf("drain_x").forGetter(FloodCell::drainX),
                Codec.INT.fieldOf("drain_y").forGetter(FloodCell::drainY),
                Codec.INT.fieldOf("drain_z").forGetter(FloodCell::drainZ)
        ).apply(instance, FloodCell::new));
    }

    private static final Codec<ErdenDrainageSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("storm_load", 0).forGetter(data -> data.stormLoad),
            Codec.INT.optionalFieldOf("river_stage_y", Integer.MIN_VALUE).forGetter(data -> data.riverStageY),
            Codec.INT.optionalFieldOf("overflow_events", 0).forGetter(data -> data.overflowEvents),
            Codec.INT.optionalFieldOf("drained_cells", 0).forGetter(data -> data.drainedCells),
            Codec.INT.optionalFieldOf("blocked_samples", 0).forGetter(data -> data.blockedSamples),
            FloodCell.CODEC.listOf().optionalFieldOf("flood_cells", List.of())
                    .forGetter(data -> List.copyOf(data.floodCells))
    ).apply(instance, ErdenDrainageSavedData::new));

    public static final SavedDataType<ErdenDrainageSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_drainage"),
            level -> new ErdenDrainageSavedData(),
            level -> CODEC
    );

    private int stormLoad;
    private int riverStageY;
    private int overflowEvents;
    private int drainedCells;
    private int blockedSamples;
    private final List<FloodCell> floodCells;

    public ErdenDrainageSavedData() {
        this(0, Integer.MIN_VALUE, 0, 0, 0, List.of());
    }

    private ErdenDrainageSavedData(
            int stormLoad,
            int riverStageY,
            int overflowEvents,
            int drainedCells,
            int blockedSamples,
            List<FloodCell> floodCells) {
        this.stormLoad = Math.max(0, Math.min(120, stormLoad));
        this.riverStageY = riverStageY;
        this.overflowEvents = Math.max(0, overflowEvents);
        this.drainedCells = Math.max(0, drainedCells);
        this.blockedSamples = Math.max(0, blockedSamples);
        this.floodCells = new ArrayList<>(floodCells);
    }

    public int stormLoad() {
        return stormLoad;
    }

    public int riverStageY() {
        return riverStageY;
    }

    public int overflowEvents() {
        return overflowEvents;
    }

    public int drainedCells() {
        return drainedCells;
    }

    public int blockedSamples() {
        return blockedSamples;
    }

    public List<FloodCell> floodCells() {
        return List.copyOf(floodCells);
    }

    public void updateStormLoad(int nextLoad) {
        int clamped = Math.max(0, Math.min(120, nextLoad));
        if (clamped == stormLoad) return;
        stormLoad = clamped;
        setDirty();
    }

    public void updateRiverStage(int stageY) {
        if (stageY == riverStageY) return;
        riverStageY = stageY;
        setDirty();
    }

    public boolean hasFloodCell(int x, int y, int z) {
        return floodCells.stream().anyMatch(cell -> cell.x() == x && cell.y() == y && cell.z() == z);
    }

    public void addFloodCell(FloodCell cell) {
        if (hasFloodCell(cell.x(), cell.y(), cell.z())) return;
        floodCells.add(cell);
        overflowEvents++;
        setDirty();
    }

    public void removeFloodCell(FloodCell cell) {
        if (!floodCells.remove(cell)) return;
        drainedCells++;
        setDirty();
    }

    public void recordBlockedSample() {
        blockedSamples++;
        setDirty();
    }
}
