package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record SettlementInfrastructureState(
        List<BuildingRecord> buildings,
        List<RoadSegment> roads,
        RoadConstructionState roadConstruction,
        List<OutpostRecord> outposts,
        OutpostConstructionState outpostConstruction) {

    public static final SettlementInfrastructureState EMPTY = new SettlementInfrastructureState(
            List.of(), List.of(), RoadConstructionState.EMPTY, List.of(), OutpostConstructionState.EMPTY);

    public static final Codec<SettlementInfrastructureState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    BuildingRecord.CODEC.listOf().optionalFieldOf("buildings", List.<BuildingRecord>of())
                            .forGetter(SettlementInfrastructureState::buildings),
                    RoadSegment.CODEC.listOf().optionalFieldOf("roads", List.<RoadSegment>of())
                            .forGetter(SettlementInfrastructureState::roads),
                    RoadConstructionState.CODEC.optionalFieldOf("road_construction", RoadConstructionState.EMPTY)
                            .forGetter(SettlementInfrastructureState::roadConstruction),
                    OutpostRecord.CODEC.listOf().optionalFieldOf("outposts", List.<OutpostRecord>of())
                            .forGetter(SettlementInfrastructureState::outposts),
                    OutpostConstructionState.CODEC
                            .optionalFieldOf("outpost_construction", OutpostConstructionState.EMPTY)
                            .forGetter(SettlementInfrastructureState::outpostConstruction)
            ).apply(instance, SettlementInfrastructureState::new));

    public SettlementInfrastructureState {
        buildings = List.copyOf(buildings);
        roads = List.copyOf(roads);
        outposts = List.copyOf(outposts);
    }
}
