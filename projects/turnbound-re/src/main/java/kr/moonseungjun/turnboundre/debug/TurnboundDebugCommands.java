package kr.moonseungjun.turnboundre.debug;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class TurnboundDebugCommands {
    private TurnboundDebugCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("turnbound_re")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("contracts")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("TURNBOUND: RE M0 contracts ready"), false);
                            return 1;
                        })));
    }
}
