package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ConstructionLogisticsData extends SavedData {
    public static final SavedDataType<ConstructionLogisticsData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "construction_logistics"),
            ConstructionLogisticsData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("active", false).forGetter(data -> data.active),
                    Codec.STRING.optionalFieldOf("type", "").forGetter(data -> data.type),
                    Codec.INT.optionalFieldOf("origin_x", 0).forGetter(data -> data.originX),
                    Codec.INT.optionalFieldOf("origin_y", 0).forGetter(data -> data.originY),
                    Codec.INT.optionalFieldOf("origin_z", 0).forGetter(data -> data.originZ),
                    Codec.INT.optionalFieldOf("rotation", 0).forGetter(data -> data.rotation)
            ).apply(instance, ConstructionLogisticsData::new))
    );

    private boolean active;
    private String type;
    private int originX;
    private int originY;
    private int originZ;
    private int rotation;

    public ConstructionLogisticsData() {
        this(false, "", 0, 0, 0, 0);
    }

    public ConstructionLogisticsData(boolean active, String type, int originX, int originY, int originZ, int rotation) {
        this.active = active;
        this.type = type;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.rotation = rotation;
    }

    public static ConstructionLogisticsData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean active() { return active; }

    public boolean matches(ConstructionState state) {
        return active
                && type.equals(state.type())
                && originX == state.originX()
                && originY == state.originY()
                && originZ == state.originZ()
                && rotation == state.rotation();
    }

    public void begin(ConstructionState state) {
        active = true;
        type = state.type();
        originX = state.originX();
        originY = state.originY();
        originZ = state.originZ();
        rotation = state.rotation();
        setDirty();
    }

    public void clear() {
        if (!active) return;
        active = false;
        type = "";
        originX = 0;
        originY = 0;
        originZ = 0;
        rotation = 0;
        setDirty();
    }
}
