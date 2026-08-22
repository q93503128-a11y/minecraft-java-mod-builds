package kr.moonseungjun.frontiersettlement.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.frontiersettlement.settlement.SettlementData;
import kr.moonseungjun.frontiersettlement.settlement.SettlementResources;
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
                .then(Commands.literal("rescan").executes(SettlementCommands::rescan)));
    }

    private static int found(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (data.founded()) {
            player.sendSystemMessage(Component.literal("이미 공동 마을이 세워져 있습니다."));
            return 0;
        }
        if (player.level() != server.overworld()) {
            player.sendSystemMessage(Component.literal("alpha.1에서는 오버월드에서 마을을 시작해 주세요."));
            return 0;
        }
        if (!SettlementService.found(player)) {
            player.sendSystemMessage(Component.literal("주변에 공동 창고를 둘 빈 공간이 없습니다."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("공동 개척지가 시작되었습니다. 건설 주민 1명과 공동 창고가 배치되었습니다."));
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SettlementData data = SettlementData.get(player.level().getServer());
        if (!data.founded()) {
            player.sendSystemMessage(Component.literal("아직 공동 마을이 없습니다. /frontier found"));
            return 0;
        }
        SettlementResources r = data.resources();
        player.sendSystemMessage(Component.literal("마을 자원 | 목재 " + r.wood()
                + " | 석재 " + r.stone() + " | 금속 " + r.metal()
                + " | 식량 " + r.food() + " | 인구 " + data.population()));
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
