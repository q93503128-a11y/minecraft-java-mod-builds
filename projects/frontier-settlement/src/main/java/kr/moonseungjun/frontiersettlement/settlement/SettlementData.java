package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

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
                    SettlementInfrastructureState.CODEC.optionalFieldOf("infrastructure", SettlementInfrastructureState.EMPTY).forGetter(data -> data.infrastructure),
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
    private SettlementInfrastructureState infrastructure;
    private ConstructionState construction;

    public SettlementData() {
        this(false, 0, 0, 0, 0, 0, 0, SettlementResources.ZERO,
                0, 0, 0, 0, SettlementInfrastructureState.EMPTY, ConstructionState.EMPTY);
    }

    public SettlementData(boolean founded, int centerX, int centerY, int centerZ,
                          int stockX, int stockY, int stockZ,
                          SettlementResources resources, int population,
                          int housingCapacity, int houseCount, int lumberCampCount,
                          SettlementInfrastructureState infrastructure,
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
        this.infrastructure = infrastructure;
        this.construction = construction;
    }

    public static SettlementData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public boolean founded() { return founded; }
    public BlockPos centerPos() { return new BlockPos(centerX, centerY, centerZ); }
    public BlockPos stockpilePos() { return new BlockPos(stockX, stockY, stockZ); }
    public SettlementResources resources() { return resources; }
    public int population() { return population; }
    public int housingCapacity() { return housingCapacity; }
    public int houseCount() { return houseCount; }
    public int lumberCampCount() { return lumberCampCount; }
    public List<BuildingRecord> buildings() { return infrastructure.buildings(); }
    public List<RoadSegment> roads() { return infrastructure.roads(); }
    public RoadConstructionState roadConstruction() { return infrastructure.roadConstruction(); }
    public List<OutpostRecord> outposts() { return infrastructure.outposts(); }
    public OutpostConstructionState outpostConstruction() { return infrastructure.outpostConstruction(); }
    public ConstructionState construction() { return construction; }

    public int buildingCount(BuildingType type) {
        int count = 0;
        for (BuildingRecord building : buildings()) if (type.id().equals(building.type())) count++;
        return count;
    }

    public void found(BlockPos center, BlockPos stockpile) {
        founded = true;
        centerX = center.getX(); centerY = center.getY(); centerZ = center.getZ();
        stockX = stockpile.getX(); stockY = stockpile.getY(); stockZ = stockpile.getZ();
        resources = SettlementResources.ZERO; population = 1; housingCapacity = 0; houseCount = 0; lumberCampCount = 0;
        infrastructure = SettlementInfrastructureState.EMPTY; construction = ConstructionState.EMPTY; setDirty();
    }

    public boolean updateResources(SettlementResources next) { if (next.equals(resources)) return false; resources = next; setDirty(); return true; }
    public void beginConstruction(BuildingType type, BlockPos origin) { beginConstruction(type, origin, BuildingRotation.NONE); }
    public void beginConstruction(BuildingType type, BlockPos origin, BuildingRotation rotation) { construction = new ConstructionState(type.id(), origin.getX(), origin.getY(), origin.getZ(), rotation.id(), 0, 0); setDirty(); }
    public void advanceConstruction() { if (!construction.active()) return; construction = construction.advance(); setDirty(); }
    public void replaceConstructionStep(int step) { if (!construction.active()) return; construction = construction.withStep(step); setDirty(); }
    public void setConstructionScaffoldMask(int mask) { if (!construction.active() || construction.scaffoldMask() == mask) return; construction = construction.withScaffoldMask(mask); setDirty(); }
    public void completeConstruction(BuildingType type) { completeConstruction(type, construction.origin(), construction.buildingRotation()); }
    public void completeConstruction(BuildingType type, BlockPos origin) { completeConstruction(type, origin, BuildingRotation.NONE); }

    public void completeConstruction(BuildingType type, BlockPos origin, BuildingRotation rotation) {
        switch (type) {
            case HOUSE -> houseCount++;
            case LUMBER_CAMP -> lumberCampCount++;
            case FARM, QUARRY, MINE, WAREHOUSE, BLACKSMITH, WORKSHOP, CART_STATION, GUARD_POST, WATCHTOWER, BARRACKS, MARKET -> { }
        }
        housingCapacity += type.housingGain();
        List<BuildingRecord> next = new ArrayList<>(buildings());
        next.add(new BuildingRecord(type.id(), origin.getX(), origin.getY(), origin.getZ(), rotation.id()));
        infrastructure = new SettlementInfrastructureState(next, roads(), roadConstruction(), outposts(), outpostConstruction());
        construction = ConstructionState.EMPTY;
        setDirty();
    }

    public void clearConstruction() { if (!construction.active()) return; construction = ConstructionState.EMPTY; setDirty(); }

    public void beginRoadConstruction(BlockPos start, int directionX, int directionZ, int length) {
        RoadConstructionState next = new RoadConstructionState(start.getX(), start.getY(), start.getZ(), directionX, directionZ, length, 0);
        infrastructure = new SettlementInfrastructureState(buildings(), roads(), next, outposts(), outpostConstruction());
        setDirty();
    }

    public void beginRoadConstruction(List<BlockPos> centers) { beginRoadConstruction(centers, List.of()); }

    public void beginRoadConstruction(List<BlockPos> centers, List<Integer> profile) {
        RoadConstructionState next = RoadConstructionState.fromPath(centers, profile);
        if (!next.active()) return;
        infrastructure = new SettlementInfrastructureState(buildings(), roads(), next, outposts(), outpostConstruction());
        setDirty();
    }

    public void advanceRoadConstruction() { if (!roadConstruction().active()) return; infrastructure = new SettlementInfrastructureState(buildings(), roads(), roadConstruction().advance(), outposts(), outpostConstruction()); setDirty(); }
    public void replaceRoadConstructionStep(int step) { if (!roadConstruction().active()) return; infrastructure = new SettlementInfrastructureState(buildings(), roads(), roadConstruction().withStep(step), outposts(), outpostConstruction()); setDirty(); }
    public void completeRoad(RoadSegment segment) { List<RoadSegment> next = new ArrayList<>(roads()); next.add(segment); infrastructure = new SettlementInfrastructureState(buildings(), next, RoadConstructionState.EMPTY, outposts(), outpostConstruction()); setDirty(); }
    public void clearRoadConstruction() { if (!roadConstruction().active()) return; infrastructure = new SettlementInfrastructureState(buildings(), roads(), RoadConstructionState.EMPTY, outposts(), outpostConstruction()); setDirty(); }

    public void beginOutpostConstruction(int roadIndex, BlockPos gate, int directionX, int directionZ) { OutpostConstructionState next = new OutpostConstructionState(roadIndex, gate.getX(), gate.getY(), gate.getZ(), directionX, directionZ, 0); infrastructure = new SettlementInfrastructureState(buildings(), roads(), roadConstruction(), outposts(), next); setDirty(); }
    public void advanceOutpostConstruction() { if (!outpostConstruction().active()) return; infrastructure = new SettlementInfrastructureState(buildings(), roads(), roadConstruction(), outposts(), outpostConstruction().advance()); setDirty(); }
    public void replaceOutpostConstructionStep(int step) { if (!outpostConstruction().active()) return; infrastructure = new SettlementInfrastructureState(buildings(), roads(), roadConstruction(), outposts(), outpostConstruction().withStep(step)); setDirty(); }
    public void completeOutpost(OutpostRecord outpost) { List<OutpostRecord> next = new ArrayList<>(outposts()); next.add(outpost); infrastructure = new SettlementInfrastructureState(buildings(), roads(), roadConstruction(), next, OutpostConstructionState.EMPTY); setDirty(); }
    public void clearOutpostConstruction() { if (!outpostConstruction().active()) return; infrastructure = new SettlementInfrastructureState(buildings(), roads(), roadConstruction(), outposts(), OutpostConstructionState.EMPTY); setDirty(); }
    public void addPopulation(int amount) { if (amount <= 0) return; population += amount; setDirty(); }
    public void setPopulation(int next) { int normalized = Math.max(1, next); if (population == normalized) return; population = normalized; setDirty(); }
}
