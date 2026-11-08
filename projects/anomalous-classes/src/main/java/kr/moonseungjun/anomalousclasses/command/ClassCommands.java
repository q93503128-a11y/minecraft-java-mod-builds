package kr.moonseungjun.anomalousclasses.command;

import com.mojang.brigadier.CommandDispatcher;
import kr.moonseungjun.anomalousclasses.state.ClassSelectionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class ClassCommands {
    private ClassCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("class")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ClassSelectionService.openMenu(player);
                    return 1;
                }));
    }
}
