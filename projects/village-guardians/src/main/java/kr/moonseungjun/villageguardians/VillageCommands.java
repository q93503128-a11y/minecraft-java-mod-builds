package kr.moonseungjun.villageguardians;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class VillageCommands {
    private VillageCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("vg")
                .then(Commands.literal("menu")
                        .executes(context -> openMenu(context.getSource())))
                .then(Commands.literal("stats")
                        .executes(context -> openStats(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("progression")
                        .executes(context -> progressionStatus(context.getSource())))
                .then(Commands.literal("raid")
                        .then(Commands.literal("status")
                                .executes(context -> raidStatus(context.getSource()))))
                .then(Commands.literal("village")
                        .then(Commands.literal("status")
                                .executes(context -> villageStatus(context.getSource())))
                        .then(Commands.literal("set_center")
                                .executes(context -> setVillageCenter(context.getSource()))))
                .then(Commands.literal("role")
                        .then(Commands.argument("role", StringArgumentType.word())
                                .executes(context -> chooseRole(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "role")))))
                .then(Commands.literal("skill")
                        .executes(context -> useSkill(context.getSource())))
                .then(Commands.literal("propose")
                        .then(Commands.literal("advance_time")
                                .executes(context -> proposeAdvanceTime(context.getSource()))))
                .then(Commands.literal("vote")
                        .then(Commands.literal("yes")
                                .executes(context -> vote(context.getSource(), true)))
                        .then(Commands.literal("no")
                                .executes(context -> vote(context.getSource(), false))));

        dispatcher.register(root);
    }

    private static int openMenu(CommandSourceStack source) throws CommandSyntaxException {
        VillageUiController.openDashboard(source.getPlayerOrException());
        return 1;
    }

    private static int openStats(CommandSourceStack source) throws CommandSyntaxException {
        VillageUiController.openStatus(source.getPlayerOrException());
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(
                () -> Component.literal(VillageCouncilState.status(source.getServer(), player)),
                false);
        source.sendSuccess(() -> Component.literal(VillageProgressionSystem.status(player)), false);
        source.sendSuccess(() -> Component.literal(VillageRaidSystem.status()), false);
        return 1;
    }

    private static int progressionStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(VillageProgressionSystem.status()), false);
        return 1;
    }

    private static int raidStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(VillageRaidSystem.status()), false);
        return 1;
    }

    private static int villageStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal(VillageCouncilState.villageStatus(player)), false);
        return 1;
    }

    private static int setVillageCenter(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String result = VillageCouncilState.setVillageCenter(player);
        boolean success = result.startsWith("마을 중심 지정 완료");
        if (success) {
            source.sendSuccess(() -> Component.literal(result), true);
            return 1;
        }
        source.sendFailure(Component.literal(result));
        return 0;
    }

    private static int chooseRole(CommandSourceStack source, String roleId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        VillageRole role = VillageRole.parse(roleId).orElse(null);
        if (role == null) {
            source.sendFailure(Component.literal("알 수 없는 역할입니다. 사용 가능: " + VillageRole.ids()));
            return 0;
        }

        String result = VillageCouncilState.chooseRole(player, role);
        source.sendSuccess(() -> Component.literal(result), true);
        return 1;
    }

    private static int useSkill(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String result = VillageRpgSystem.useRoleSkill(player);
        boolean success = result.contains("사용 완료");
        if (success) {
            source.sendSuccess(() -> Component.literal(result), false);
            return 1;
        }
        source.sendFailure(Component.literal(result));
        return 0;
    }

    private static int proposeAdvanceTime(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String result = VillageCouncilState.proposeAdvanceTime(player);
        source.sendSuccess(() -> Component.literal(result), false);
        return result.contains("진행") || result.contains("투표") ? 1 : 0;
    }

    private static int vote(CommandSourceStack source, boolean yes) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String result = VillageCouncilState.vote(player, yes);
        source.sendSuccess(() -> Component.literal(result), false);
        return result.contains("투표했습니다") ? 1 : 0;
    }
}
