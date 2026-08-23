package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import kr.moonseungjun.frontiersettlement.settlement.SettlementAdvancedWorkshopService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementBarracksService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementCartStationService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementConstructionOfficeService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementConstructionService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementData;
import kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementRoadService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementWatchtowerService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementWorkshopService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

public final class SettlementNetwork {
    private static final String PROTOCOL = "7";
    private static Consumer<SettlementSnapshotPayload> snapshotSink = payload -> {};
    private static Consumer<PlacementPreviewPayload> placementPreviewSink = payload -> {};
    private static Consumer<RoadPreviewPayload> roadPreviewSink = payload -> {};
    private static Consumer<OutpostPreviewPayload> outpostPreviewSink = payload -> {};

    private SettlementNetwork() {}
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar=event.registrar(PROTOCOL);
        registrar.playToClient(SettlementSnapshotPayload.TYPE,SettlementSnapshotPayload.CODEC,(p,c)->snapshotSink.accept(p));
        registrar.playToClient(PlacementPreviewPayload.TYPE,PlacementPreviewPayload.CODEC,(p,c)->placementPreviewSink.accept(p));
        registrar.playToClient(RoadPreviewPayload.TYPE,RoadPreviewPayload.CODEC,(p,c)->roadPreviewSink.accept(p));
        registrar.playToClient(OutpostPreviewPayload.TYPE,OutpostPreviewPayload.CODEC,(p,c)->outpostPreviewSink.accept(p));
        registrar.playToServer(PlacementRequestPayload.TYPE,PlacementRequestPayload.CODEC,SettlementNetwork::handlePlacementRequest);
        registrar.playToServer(RoadPlacementRequestPayload.TYPE,RoadPlacementRequestPayload.CODEC,SettlementNetwork::handleRoadPlacementRequest);
        registrar.playToServer(OutpostPlacementRequestPayload.TYPE,OutpostPlacementRequestPayload.CODEC,SettlementNetwork::handleOutpostPlacementRequest);
    }
    private static void handlePlacementRequest(PlacementRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return; BuildingType type=BuildingType.fromId(payload.buildingType());
        if(type==null){context.reply(new PlacementPreviewPayload(payload.nonce(),payload.buildingType(),false,false,0,0,0,payload.rotation(),"알 수 없는 건물입니다."));return;}
        SettlementData data=SettlementData.get(player.level().getServer()); String lock=null;
        if(type==BuildingType.WORKSHOP)lock=SettlementWorkshopService.lockedReason(data);
        else if(type==BuildingType.ADVANCED_WORKSHOP)lock=SettlementAdvancedWorkshopService.lockedReason(data);
        else if(type==BuildingType.CART_STATION)lock=SettlementCartStationService.lockedReason(data);
        else if(type==BuildingType.WATCHTOWER)lock=SettlementWatchtowerService.lockedReason(data);
        else if(type==BuildingType.BARRACKS)lock=SettlementBarracksService.lockedReason(data);
        else if(type==BuildingType.CONSTRUCTION_OFFICE)lock=SettlementConstructionOfficeService.lockedReason(data);
        if(lock!=null){context.reply(new PlacementPreviewPayload(payload.nonce(),type.id(),false,false,0,0,0,payload.rotation(),lock));return;}
        if(data.construction().active()||data.roadConstruction().active()||data.outpostConstruction().active()){context.reply(new PlacementPreviewPayload(payload.nonce(),type.id(),false,false,0,0,0,payload.rotation(),"현재 공사가 끝난 뒤 새 건물을 배치해 주세요."));return;}
        BlockPos center=new BlockPos(payload.centerX(),payload.centerY(),payload.centerZ());
        if(type==BuildingType.CART_STATION){String p=SettlementCartStationService.placementReason(data,center);if(p!=null){context.reply(new PlacementPreviewPayload(payload.nonce(),type.id(),false,false,0,0,0,payload.rotation(),p));return;}}
        var check=SettlementConstructionService.checkPlacement(player,type,center,payload.rotation());
        if(!check.valid()){context.reply(new PlacementPreviewPayload(payload.nonce(),type.id(),false,false,0,0,0,payload.rotation(),check.message()));return;}
        if(!payload.confirm()){context.reply(new PlacementPreviewPayload(payload.nonce(),type.id(),true,false,check.origin().getX(),check.origin().getY(),check.origin().getZ(),payload.rotation(),check.message()));return;}
        var result=SettlementConstructionService.startAt(player,type,center,payload.rotation()); player.sendSystemMessage(Component.literal(result.message())); context.reply(new PlacementPreviewPayload(payload.nonce(),type.id(),result.started(),result.started(),check.origin().getX(),check.origin().getY(),check.origin().getZ(),payload.rotation(),result.message()));
    }
    private static void handleRoadPlacementRequest(RoadPlacementRequestPayload p, net.neoforged.neoforge.network.handling.IPayloadContext c){if(!(c.player() instanceof ServerPlayer player))return;BlockPos s=new BlockPos(p.startX(),p.startY(),p.startZ()),e=new BlockPos(p.endX(),p.endY(),p.endZ());var check=SettlementRoadService.checkRoute(player,s,e);if(!p.confirm()||!check.valid()){c.reply(RoadPreviewPayload.fromCheck(p.nonce(),check,false));return;}var r=SettlementRoadService.startAt(player,s,e);player.sendSystemMessage(Component.literal(r.message()));if(r.started())c.reply(RoadPreviewPayload.fromCheck(p.nonce(),check,true));else{var f=RoadPreviewPayload.fromCheck(p.nonce(),check,false);c.reply(new RoadPreviewPayload(p.nonce(),false,false,f.stoneCost(),f.path(),r.message()));}}
    private static void handleOutpostPlacementRequest(OutpostPlacementRequestPayload p, net.neoforged.neoforge.network.handling.IPayloadContext c){if(!(c.player() instanceof ServerPlayer player))return;BlockPos selected=new BlockPos(p.targetX(),p.targetY(),p.targetZ());var check=SettlementOutpostService.checkPlacement(player,selected);if(!p.confirm()||!check.valid()){c.reply(OutpostPreviewPayload.fromCheck(p.nonce(),check,false));return;}var r=SettlementOutpostService.startAt(player,check.roadIndex());player.sendSystemMessage(Component.literal(r.message()));if(r.started())c.reply(OutpostPreviewPayload.fromCheck(p.nonce(),check,true));else c.reply(new OutpostPreviewPayload(p.nonce(),false,false,check.roadIndex(),check.gate().getX(),check.gate().getY(),check.gate().getZ(),check.directionX(),check.directionZ(),check.specialization(),r.message()));}
    public static void setSnapshotSink(Consumer<SettlementSnapshotPayload> s){snapshotSink=s==null?p->{}:s;} public static void setPlacementPreviewSink(Consumer<PlacementPreviewPayload>s){placementPreviewSink=s==null?p->{}:s;} public static void setRoadPreviewSink(Consumer<RoadPreviewPayload>s){roadPreviewSink=s==null?p->{}:s;} public static void setOutpostPreviewSink(Consumer<OutpostPreviewPayload>s){outpostPreviewSink=s==null?p->{}:s;} public static void sendSnapshot(ServerPlayer p,SettlementSnapshotPayload payload){PacketDistributor.sendToPlayer(p,payload);}
}
