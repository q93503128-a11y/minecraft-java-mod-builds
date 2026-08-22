package kr.moonseungjun.frontiersettlement.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import kr.moonseungjun.frontiersettlement.settlement.ConstructionState;
import kr.moonseungjun.frontiersettlement.settlement.OutpostConstructionState;
import kr.moonseungjun.frontiersettlement.settlement.OutpostRecord;
import kr.moonseungjun.frontiersettlement.settlement.RoadConstructionState;
import kr.moonseungjun.frontiersettlement.settlement.RoadSegment;
import kr.moonseungjun.frontiersettlement.settlement.SettlementConstructionService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementData;
import kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementResources;
import kr.moonseungjun.frontiersettlement.settlement.SettlementRoadService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                        .then(Commands.literal("house").executes(context -> build(context, BuildingType.HOUSE)))
                        .then(Commands.literal("lumber_camp").executes(context -> build(context, BuildingType.LUMBER_CAMP)))
                        .then(Commands.literal("farm").executes(context -> build(context, BuildingType.FARM)))
                        .then(Commands.literal("quarry").executes(context -> build(context, BuildingType.QUARRY)))
                        .then(Commands.literal("mine").executes(context -> build(context, BuildingType.MINE)))));
    }

    private static int found(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) { player.sendSystemMessage(Component.literal("이미 공동 마을이 세워져 있습니다.")); return 0; }
        if (player.level() != server.overworld()) { player.sendSystemMessage(Component.literal("현재는 오버월드에서 마을을 시작해 주세요.")); return 0; }
        if (!SettlementService.found(player)) { player.sendSystemMessage(Component.literal("주변에 공동 창고를 둘 빈 공간이 없습니다.")); return 0; }
        player.sendSystemMessage(Component.literal("공동 개척지가 시작되었습니다. 건물은 위치만 정하면 주민이 건설합니다."));
        return 1;
    }

    private static int build(CommandContext<CommandSourceStack> context, BuildingType type) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SettlementData data = SettlementData.get(player.level().getServer());
        if (data.outpostConstruction().active()) { player.sendSystemMessage(Component.literal("전초기지 공사가 끝난 뒤 건물을 시작해 주세요.")); return 0; }
        String locked = lockedReason(data, type);
        if (locked != null) { player.sendSystemMessage(Component.literal(locked)); return 0; }
        SettlementConstructionService.StartResult result = SettlementConstructionService.start(player, type);
        player.sendSystemMessage(Component.literal(result.message()));
        return result.started() ? 1 : 0;
    }

    private static String lockedReason(SettlementData data, BuildingType type) {
        if (type == BuildingType.FARM && data.houseCount() < 1) return "농장은 주택 1채를 먼저 완성하면 열립니다.";
        if (type == BuildingType.QUARRY && data.lumberCampCount() < 1) return "채석장은 벌목소 1곳을 먼저 완성하면 열립니다.";
        if (type == BuildingType.MINE && (data.buildingCount(BuildingType.QUARRY) < 1 || data.outposts().isEmpty())) {
            return "광산은 채석장 1곳과 연결된 전초기지 1곳을 만든 뒤 열립니다.";
        }
        return null;
    }

    private static int road(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SettlementData data = SettlementData.get(player.level().getServer());
        if (data.outpostConstruction().active()) { player.sendSystemMessage(Component.literal("전초기지 공사가 끝난 뒤 도로를 시작해 주세요.")); return 0; }
        SettlementRoadService.StartResult result = SettlementRoadService.start(player);
        player.sendSystemMessage(Component.literal(result.message()));
        return result.started() ? 1 : 0;
    }

    private static int outpost(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SettlementOutpostService.StartResult result = SettlementOutpostService.start(player);
        player.sendSystemMessage(Component.literal(result.message()));
        return result.started() ? 1 : 0;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SettlementData data = SettlementData.get(player.level().getServer());
        if (!data.founded()) { player.sendSystemMessage(Component.literal("아직 공동 마을이 없습니다. /frontier found")); return 0; }
        SettlementResources r = data.resources();
        player.sendSystemMessage(Component.literal("마을 자원 | 목재 " + r.wood() + " | 석재 " + r.stone() + " | 금속 " + r.metal()
                + " | 식량 " + r.food() + " | 인구 " + data.population() + " | 주거 " + data.housingCapacity()));
        player.sendSystemMessage(Component.literal("인프라 | 주택 " + data.houseCount() + " | 벌목소 " + data.lumberCampCount()
                + " | 농장 " + data.buildingCount(BuildingType.FARM) + " | 채석장 " + data.buildingCount(BuildingType.QUARRY)
                + " | 광산 " + data.buildingCount(BuildingType.MINE) + " | 도로 " + data.roads().size() + " | 전초기지 " + data.outposts().size()));
        if (!data.roads().isEmpty()) {
            RoadSegment last = data.roads().get(data.roads().size() - 1);
            player.sendSystemMessage(Component.literal("최근 도로 끝점 | " + last.end().getX() + ", " + last.end().getY() + ", " + last.end().getZ()));
        }
        if (!data.outposts().isEmpty()) {
            OutpostRecord last = data.outposts().get(data.outposts().size() - 1);
            player.sendSystemMessage(Component.literal("최근 전초기지 | " + last.centerX() + ", " + last.centerY() + ", " + last.centerZ()
                    + " | 특화 " + last.specializationDisplayName()));
        }
        ConstructionState construction = data.construction();
        if (construction.active()) {
            BuildingType type = BuildingType.fromId(construction.type());
            if (type != null) {
                int total = SettlementConstructionService.totalSteps(type, construction.origin());
                int percent = total <= 0 ? 0 : Math.min(100, construction.step() * 100 / total);
                player.sendSystemMessage(Component.literal("공사 중 | " + type.displayName() + " " + percent + "%"));
            }
        }
        RoadConstructionState road = data.roadConstruction();
        if (road.active()) {
            int total = SettlementRoadService.totalSteps(road);
            int percent = total <= 0 ? 0 : Math.min(100, road.step() * 100 / total);
            player.sendSystemMessage(Component.literal("도로 공사 중 | " + percent + "%"));
        }
        OutpostConstructionState outpost = data.outpostConstruction();
        if (outpost.active()) {
            int total = SettlementOutpostService.totalSteps(outpost);
            int percent = total <= 0 ? 0 : Math.min(100, outpost.step() * 100 / total);
            player.sendSystemMessage(Component.literal("전초기지 공사 중 | " + percent + "%"));
        }
        return 1;
    }

    private static int rescan(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return 0;
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return status(context);
    }
}
