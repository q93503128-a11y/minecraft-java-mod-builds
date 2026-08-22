package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class SettlementData extends SavedData {
    public static final SavedDataType<SettlementData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "settlement"),
            SettlementData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("founded", false).forGetter(data -> data.founded),
                    Codec.INT.optionalFieldOf("center_x", 0).forGetter(data -> data.centerX),
                    Codec.INT.optionalFieldOf("center_y", 0).forGetter(data -> data.centerY),
                    Codec.INT.optionalFieldOf("center_z", 0).forGetter(data -> data.centerZ),
                    Codec.INT.optionalFieldOf("stock_x", 0).forGetter(data -> data.stockX),
                    Codec.INT.optionalFieldOf("stock_y", 0).forGetter(data -> data.stockY),
                    Codec.INT.optionalFieldOf("stock_z", 0).forGetter(data -> data.stockZ),
                    SettlementResources.CODEC.optionalFieldOf("resources", SettlementResources.ZERO).forGetter(data -> data.resources),
                    Codec.INT.optionalFieldOf("population", 0).forGetter(data -> data.population)
            ).apply(instance, SettlementData::new))
    );

    private boolean founded;
    private int centerX;
    private int centerY;
    private int centerZ;
    private int stockX;
    private int stockY;
    private int stockZ;
    private SettlementResources resources;
    private int population;

    public SettlementData() {
        this(false, 0, 0, 0, 0, 0, 0, SettlementResources.ZERO, 0);
    }

    public SettlementData(boolean founded, int centerX, int centerY, int centerZ,
                          int stockX, int stockY, int stockZ,
                          SettlementResources resources, int population) {
        this.founded = founded;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.stockX = stockX;
        this.stockY = stockY;
        this.stockZ = stockZ;
        this.resources = resources;
        this.population = population;
    }

    public static SettlementData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean founded() {
        return founded;
    }

    public BlockPos centerPos() {
        return new BlockPos(centerX, centerY, centerZ);
    }

    public BlockPos stockpilePos() {
        return new BlockPos(stockX, stockY, stockZ);
    }

    public SettlementResources resources() {
        return resources;
    }

    public int population() {
        return population;
    }

    public void found(BlockPos center, BlockPos stockpile) {
        this.founded = true;
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();
        this.stockX = stockpile.getX();
        this.stockY = stockpile.getY();
        this.stockZ = stockpile.getZ();
        this.resources = SettlementResources.ZERO;
        this.population = 1;
        setDirty();
    }

    public boolean updateResources(SettlementResources next) {
        if (next.equals(this.resources)) return false;
        this.resources = next;
        setDirty();
        return true;
    }
}
