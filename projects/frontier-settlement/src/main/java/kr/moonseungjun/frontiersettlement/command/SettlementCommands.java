package kr.moonseungjun.frontiersettlement.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.frontiersettlement.settlement.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SettlementCommands {
    private SettlementCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("frontier")
                .then(Commands.literal("found").executes(SettlementCommands::found))
                .then(Commands.literal("status").executes(SettlementCommands::status))
                .then(Commands.literal("rescan").executes(SettlementCommands::rescan))
                .then(Commands.literal("road").executes(SettlementCommands::road))
                .then(Commands.literal("outpost").executes(SettlementCommands::outpost))
                .then(Commands.literal("build")
                        .then(Commands.literal("house").executes(c -> build(c, BuildingType.HOUSE)))
                        .then(Commands.literal("lumber_camp").executes(c -> build(c, BuildingType.LUMBER_CAMP)))
                        .then(Commands.literal("farm").executes(c -> build(c, BuildingType.FARM)))
                        .then(Commands.literal("quarry").executes(c -> build(c, BuildingType.QUARRY)))
                        .then(Commands.literal("mine").executes(c -> build(c, BuildingType.MINE)))
                        .then(Commands.literal("warehouse").executes(c -> build(c, BuildingType.WAREHOUSE)))
                        .then(Commands.literal("construction_office").executes(c -> build(c, BuildingType.CONSTRUCTION_OFFICE)))
                        .then(Commands.literal("blacksmith").executes(c -> build(c, BuildingType.BLACKSMITH)))
                        .then(Commands.literal("workshop").executes(c -> build(c, BuildingType.WORKSHOP)))
                        .then(Commands.literal("advanced_workshop").executes(c -> build(c, BuildingType.ADVANCED_WORKSHOP)))
                        .then(Commands.literal("guard_post").executes(c -> build(c, BuildingType.GUARD_POST)))
                        .then(Commands.literal("watchtower").executes(c -> build(c, BuildingType.WATCHTOWER)))
                        .then(Commands.literal("barracks").executes(c -> build(c, BuildingType.BARRACKS)))
                        .then(Commands.literal("market").executes(c -> build(c, BuildingType.MARKET)))
                        .then(Commands.literal("cart_station").executes(c -> build(c, BuildingType.CART_STATION)))));
    }

    private static int found(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player=context.getSource().getPlayerOrException(); MinecraftServer server=player.level().getServer(); SettlementData data=SettlementData.get(server);
        if(data.founded()){player.sendSystemMessage(Component.literal("이미 공동 마을이 세워져 있습니다."));return 0;}
        if(player.level()!=server.overworld()){player.sendSystemMessage(Component.literal("현재는 오버월드에서 마을을 시작해 주세요."));return 0;}
        if(!SettlementService.found(player)){player.sendSystemMessage(Component.literal("주변에 공동 창고를 둘 빈 공간이 없습니다."));return 0;}
        player.sendSystemMessage(Component.literal("공동 개척지가 시작되었습니다. 건물은 위치만 정하면 주민이 건설합니다.")); return 1;
    }

    private static int build(CommandContext<CommandSourceStack> context, BuildingType type) throws CommandSyntaxException {
        ServerPlayer player=context.getSource().getPlayerOrException(); SettlementData data=SettlementData.get(player.level().getServer());
        if(data.outpostConstruction().active()){player.sendSystemMessage(Component.literal("전초기지 공사가 끝난 뒤 건물을 시작해 주세요."));return 0;}
        String locked;
        if(type==BuildingType.WORKSHOP) locked=SettlementWorkshopService.lockedReason(data);
        else if(type==BuildingType.ADVANCED_WORKSHOP) locked=SettlementAdvancedWorkshopService.lockedReason(data);
        else if(type==BuildingType.CART_STATION) locked=SettlementCartStationService.lockedReason(data);
        else if(type==BuildingType.WATCHTOWER) locked=SettlementWatchtowerService.lockedReason(data);
        else if(type==BuildingType.BARRACKS) locked=SettlementBarracksService.lockedReason(data);
        else if(type==BuildingType.CONSTRUCTION_OFFICE) locked=SettlementConstructionOfficeService.lockedReason(data);
        else locked=SettlementConstructionService.lockedReason(data,type);
        if(locked!=null){player.sendSystemMessage(Component.literal(locked));return 0;}
        if(type==BuildingType.CART_STATION){BlockPos center=player.blockPosition().relative(player.getDirection(),10);String p=SettlementCartStationService.placementReason(data,center);if(p!=null){player.sendSystemMessage(Component.literal(p));return 0;}}
        SettlementConstructionService.StartResult result=SettlementConstructionService.start(player,type); player.sendSystemMessage(Component.literal(result.message())); return result.started()?1:0;
    }

    private static int road(CommandContext<CommandSourceStack> context) throws CommandSyntaxException { ServerPlayer p=context.getSource().getPlayerOrException(); SettlementData d=SettlementData.get(p.level().getServer()); if(d.outpostConstruction().active()){p.sendSystemMessage(Component.literal("전초기지 공사가 끝난 뒤 도로를 시작해 주세요."));return 0;} var r=SettlementRoadService.start(p); p.sendSystemMessage(Component.literal(r.message())); return r.started()?1:0; }
    private static int outpost(CommandContext<CommandSourceStack> context) throws CommandSyntaxException { ServerPlayer p=context.getSource().getPlayerOrException(); var r=SettlementOutpostService.start(p); p.sendSystemMessage(Component.literal(r.message())); return r.started()?1:0; }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player=context.getSource().getPlayerOrException(); MinecraftServer server=player.level().getServer(); SettlementData data=SettlementData.get(server);
        if(!data.founded()){player.sendSystemMessage(Component.literal("아직 공동 마을이 없습니다. /frontier found"));return 0;}
        SettlementResources r=data.resources();
        player.sendSystemMessage(Component.literal("마을 단계 | "+SettlementTier.current(data).displayName()));
        player.sendSystemMessage(Component.literal("마을 자원 | 목재 "+r.wood()+" | 석재 "+r.stone()+" | 금속 "+r.metal()+" | 식량 "+r.food()+" | 인구 "+data.population()+" | 주거 "+data.housingCapacity()));
        player.sendSystemMessage(Component.literal("인프라 | 주택 "+data.houseCount()+" | 벌목소 "+data.lumberCampCount()+" | 농장 "+data.buildingCount(BuildingType.FARM)+" | 채석장 "+data.buildingCount(BuildingType.QUARRY)+" | 광산 "+data.buildingCount(BuildingType.MINE)+" | 창고 "+data.buildingCount(BuildingType.WAREHOUSE)+" | 건설소 "+data.buildingCount(BuildingType.CONSTRUCTION_OFFICE)+" | 대장간 "+data.buildingCount(BuildingType.BLACKSMITH)+" | 작업장 "+data.buildingCount(BuildingType.WORKSHOP)+" | 고급 제작소 "+data.buildingCount(BuildingType.ADVANCED_WORKSHOP)+" | 경비초소 "+data.buildingCount(BuildingType.GUARD_POST)+" | 감시탑 "+data.buildingCount(BuildingType.WATCHTOWER)+" | 병영 "+data.buildingCount(BuildingType.BARRACKS)+" | 시장 "+data.buildingCount(BuildingType.MARKET)+" | 수레 정거장 "+data.buildingCount(BuildingType.CART_STATION)+" | 도로 "+data.roads().size()+" | 전초기지 "+data.outposts().size()));
        if(data.buildingCount(BuildingType.CONSTRUCTION_OFFICE)>0){SettlementConstructionOfficeService.SupplySnapshot supply=SettlementConstructionOfficeService.snapshot(server.overworld(),data);player.sendSystemMessage(Component.literal("건설 보급 | 집결 목재 "+supply.wood()+" | 석재 "+supply.stone()+" | 보급 주민 "+supply.runners()));}
        if(data.buildingCount(BuildingType.ADVANCED_WORKSHOP)>0){player.sendSystemMessage(Component.literal("고급 제작 | 준비 의뢰 "+SettlementAdvancedWorkshopService.readyCommissionCount(server.overworld(),data)+" | 1회 유물 "+SettlementAdvancedWorkshopService.RELIC_COST+" + 금속 "+SettlementAdvancedWorkshopService.METAL_COST+" | 강화력 "+SettlementAdvancedWorkshopService.ENCHANTMENT_POWER));}
        if(SettlementBarracksService.militaryStateLoaded(server.overworld(),data)) player.sendSystemMessage(Component.literal("군사 | 주둔병 "+SettlementBarracksService.loadedSoldierCount(server.overworld(),data)+" / "+SettlementBarracksService.militaryCapacity(data)+" | 충원비 1명당 식량 "+SettlementBarracksService.RECRUIT_FOOD_COST+" 금속 "+SettlementBarracksService.RECRUIT_METAL_COST));
        else player.sendSystemMessage(Component.literal("군사 | 병영 주변 청크가 로드되면 주둔병 상태를 확인합니다."));
        SettlementMilitaryOutpostService.SupplySnapshot militarySupply=SettlementMilitaryOutpostService.activeSupplySnapshot(server.overworld(),data);
        player.sendSystemMessage(Component.literal("위험지역 전초 | 활성 "+SettlementMilitaryOutpostService.activeMilitaryOutpostCount(server.overworld(),data)+"곳 | 로드된 전초 수비대 "+SettlementMilitaryOutpostService.loadedSentryCount(server.overworld(),data)+" | 현지 보급 식량 "+militarySupply.food()+" 금속 "+militarySupply.metal()+" | 충원비 식량 "+SettlementMilitaryOutpostService.RECRUIT_FOOD_COST+" 금속 "+SettlementMilitaryOutpostService.RECRUIT_METAL_COST));
        player.sendSystemMessage(Component.literal("물류 | 운송 1회 적재 "+SettlementOutpostLogisticsService.transportBatchSize(data)+" | 수레 정거장 화물 배럴 "+(data.buildingCount(BuildingType.CART_STATION)*4)+" | 군사 전초도 같은 도로 운송자가 역방향 보급"));
        player.sendSystemMessage(Component.literal("수변 전초 | 로드된 어업·수변교역 "+SettlementFishingOutpostService.activeFishingOutpostCount(server.overworld(),data)+"곳 | 위험지역 군사 역할이 우선 | 어획물은 기존 도로 물류로 운송"));
        SettlementDeferredOutpostService.Snapshot deferred=SettlementDeferredOutpostService.snapshot(server,data);
        player.sendSystemMessage(Component.literal("언로드 보정 | 생산 작업시간 "+deferred.productionTicks()+"틱 / "+deferred.productionBacklogOutposts()+"전초 | 물류 작업시간 "+deferred.logisticsTicks()+"틱 / "+deferred.logisticsBacklogOutposts()+"전초 | 가상 자원·가상 화물 0"));
        SettlementExternalContentService.Snapshot external=SettlementExternalContentService.snapshot(server.overworld(),data);
        player.sendSystemMessage(Component.literal(external.storageLoaded()?"탐험 연동 | 유물 "+external.expeditionRelics()+" | 외부 무기 "+external.externalWeapons():"탐험 연동 | 마을 저장소 청크가 로드되면 물리 전리품을 확인합니다."));
        player.sendSystemMessage(Component.literal("개척 진척 | 외부 구조물 "+data.discoveredExternalStructures().size()+"종 | 정복 강적 "+data.defeatedExternalBosses().size()+"종 | 진척 "+data.explorationScore()+" / 8 | 동일 종류 반복은 중복 없음"));
        if(!data.roads().isEmpty()){RoadSegment last=data.roads().getLast();player.sendSystemMessage(Component.literal("최근 도로 끝점 | "+last.end().getX()+", "+last.end().getY()+", "+last.end().getZ()));}
        if(!data.outposts().isEmpty()){OutpostRecord last=data.outposts().getLast();player.sendSystemMessage(Component.literal("최근 전초기지 | "+last.centerX()+", "+last.centerY()+", "+last.centerZ()+" | 특화 "+SettlementFishingOutpostService.specializationDisplayName(server.overworld(),last)));}
        ConstructionState c=data.construction(); if(c.active()){BuildingType t=BuildingType.fromId(c.type()); if(t!=null){int total=SettlementConstructionService.totalSteps(t,c.origin());player.sendSystemMessage(Component.literal("공사 중 | "+t.displayName()+" "+(total<=0?0:Math.min(100,c.step()*100/total))+"%"));}}
        RoadConstructionState road=data.roadConstruction(); if(road.active()){int total=SettlementRoadService.totalSteps(road);player.sendSystemMessage(Component.literal("도로 공사 중 | "+(total<=0?0:Math.min(100,road.step()*100/total))+"%"));}
        OutpostConstructionState o=data.outpostConstruction(); if(o.active()){int total=SettlementOutpostService.totalSteps(o);player.sendSystemMessage(Component.literal("전초기지 공사 중 | "+(total<=0?0:Math.min(100,o.step()*100/total))+"%"));}
        return 1;
    }

    private static int rescan(CommandContext<CommandSourceStack> context) throws CommandSyntaxException { ServerPlayer p=context.getSource().getPlayerOrException(); MinecraftServer s=p.level().getServer(); SettlementData d=SettlementData.get(s); if(!d.founded())return 0; SettlementService.refreshResources(s,d); SettlementService.broadcast(s,d); return status(context); }
}
