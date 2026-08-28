package io.github.q93503128.turnbound.command;

import com.mojang.brigadier.Command;
import io.github.q93503128.turnbound.combat.P0Scenario;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TurnboundCommands {
    private TurnboundCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("turnbound")
                .then(Commands.literal("p0").executes(context -> {
                    String report = P0Scenario.runAutoDiagnostic(120);
                    context.getSource().sendSuccess(() -> Component.literal("TURNBOUND P0: " + report), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
