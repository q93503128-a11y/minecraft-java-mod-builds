package kr.moonseungjun.anomalousclasses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.moonseungjun.anomalousclasses.state.ProfileService;
import kr.moonseungjun.anomalousclasses.state.ClassSelectionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ClassCommands {
    private ClassCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("class")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ClassSelectionService.openMenu(player);
                    return 1;
                })
                .then(Commands.literal("test").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("grant-levels")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100_000))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            if (!ProfileService.grantLevelsForTesting(player, amount)) {
                                                context.getSource().sendFailure(Component.translatable("command.anomalousclasses.test.level_grant_failed"));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    "command.anomalousclasses.test.level_granted", amount), false);
                                            return 1;
                                        })))));
    }
}
