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
                    Codec.INT.optionalFieldOf("population", 0).forGetter(data -> data.population),
                    Codec.INT.optionalFieldOf("housing_capacity", 0).forGetter(data -> data.housingCapacity),
                    Codec.INT.optionalFieldOf("house_count", 0).forGetter(data -> data.houseCount),
                    Codec.INT.optionalFieldOf("lumber_camp_count", 0).forGetter(data -> data.lumberCampCount),
                    ConstructionState.CODEC.optionalFieldOf("construction", ConstructionState.EMPTY).forGetter(data -> data.construction)
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
    private int housingCapacity;
    private int houseCount;
    private int lumberCampCount;
    private ConstructionState construction;

    public SettlementData() {
        this(false, 0, 0, 0, 0, 0, 0, SettlementResources.ZERO,
                0, 0, 0, 0, ConstructionState.EMPTY);
    }

    public SettlementData(boolean founded, int centerX, int centerY, int centerZ,
                          int stockX, int stockY, int stockZ,
                          SettlementResources resources, int population,
                          int housingCapacity, int houseCount, int lumberCampCount,
                          ConstructionState construction) {
        this.founded = founded;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.stockX = stockX;
        this.stockY = stockY;
        this.stockZ = stockZ;
        this.resources = resources;
        this.population = population;
        this.housingCapacity = housingCapacity;
        this.houseCount = houseCount;
        this.lumberCampCount = lumberCampCount;
        this.construction = construction;
    }

    public static SettlementData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean founded() { return founded; }
    public BlockPos centerPos() { return new BlockPos(centerX, centerY, centerZ); }
    public BlockPos stockpilePos() { return new BlockPos(stockX, stockY, stockZ); }
    public SettlementResources resources() { return resources; }
    public int population() { return population; }
    public int housingCapacity() { return housingCapacity; }
    public int houseCount() { return houseCount; }
    public int lumberCampCount() { return lumberCampCount; }
    public ConstructionState construction() { return construction; }

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
        this.housingCapacity = 0;
        this.houseCount = 0;
        this.lumberCampCount = 0;
        this.construction = ConstructionState.EMPTY;
        setDirty();
    }

    public boolean updateResources(SettlementResources next) {
        if (next.equals(this.resources)) return false;
        this.resources = next;
        setDirty();
        return true;
    }

    public void beginConstruction(BuildingType type, BlockPos origin) {
        this.construction = new ConstructionState(type.id(), origin.getX(), origin.getY(), origin.getZ(), 0);
        setDirty();
    }

    public void advanceConstruction() {
        if (!construction.active()) return;
        this.construction = construction.advance();
        setDirty();
    }

    public void completeConstruction(BuildingType type) {
        switch (type) {
            case HOUSE -> houseCount++;
            case LUMBER_CAMP -> lumberCampCount++;
        }
        housingCapacity += type.housingGain();
        construction = ConstructionState.EMPTY;
        setDirty();
    }

    public void clearConstruction() {
        if (!construction.active()) return;
        construction = ConstructionState.EMPTY;
        setDirty();
    }
}
