package kr.moonseungjun.villageguardians;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class VillageCommands {
    private VillageCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vg")
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
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
                .then(Commands.literal("rpg")
                        .then(Commands.literal("status")
                                .executes(context -> rpgStatus(context.getSource())))
                        .then(Commands.literal("test_xp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 5000))
                                        .executes(context -> grantTestExperience(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("propose")
                        .then(Commands.literal("advance_time")
                                .executes(context -> proposeAdvanceTime(context.getSource()))))
                .then(Commands.literal("vote")
                        .then(Commands.literal("yes")
                                .executes(context -> vote(context.getSource(), true)))
                        .then(Commands.literal("no")
                                .executes(context -> vote(context.getSource(), false))))
                .then(Commands.literal("mayor")
                        .then(Commands.literal("transfer")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> transferMayor(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))))))));
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(
                () -> Component.literal(VillageCouncilState.status(source.getServer(), player)),
                false);
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

    private static int rpgStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal(VillageCouncilState.rpgStatus(player)), false);
        return 1;
    }

    private static int grantTestExperience(CommandSourceStack source, int amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!VillageCouncilState.isMayor(player)) {
            source.sendFailure(Component.literal("알파 테스트 경험치 지급은 촌장만 사용할 수 있습니다."));
            return 0;
        }

        VillageCouncilState.ExperienceResult result = VillageCouncilState.grantExperience(player, amount);
        VillageRpgSystem.refreshPlayerPassive(player);
        player.heal(player.getMaxHealth());
        source.sendSuccess(() -> Component.literal("§d테스트 XP " + result.awardedExperience()
                + " 지급 | 현재 레벨 " + result.current().level()
                + " | 현재 XP " + result.current().experience()), true);
        return 1;
    }

    private static int proposeAdvanceTime(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String result = VillageCouncilState.proposeAdvanceTime(player);
        source.sendSuccess(() -> Component.literal(result), false);
        return result.startsWith("시간 진행") ? 1 : 0;
    }

    private static int vote(CommandSourceStack source, boolean yes) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String result = VillageCouncilState.vote(player, yes);
        source.sendSuccess(() -> Component.literal(result), false);
        return result.contains("투표했습니다") ? 1 : 0;
    }

    private static int transferMayor(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer actor = source.getPlayerOrException();
        String result = VillageCouncilState.transferMayor(actor, target);
        source.sendSuccess(() -> Component.literal(result), true);
        return result.equals("촌장직 이전 완료") ? 1 : 0;
    }
}
