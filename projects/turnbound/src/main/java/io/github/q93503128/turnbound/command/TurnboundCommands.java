package io.github.q93503128.turnbound.command;

import com.mojang.brigadier.Command;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import io.github.q93503128.turnbound.world.FieldSessionManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TurnboundCommands {
    private TurnboundCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("turnbound")
                .then(Commands.literal("field").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    return FieldSessionManager.enter(player) ? Command.SINGLE_SUCCESS : 0;
                }))
                .then(Commands.literal("status").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    FieldSessionManager.sendStatus(player);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("p0").executes(context -> {
                    String result = P0Scenario.runAutoDiagnostic(160);
                    context.getSource().sendSuccess(() -> Component.literal("TURNBOUND P0: " + result), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("battle").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    BattleSessionManager.start(player);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("leave").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    BattleSessionManager.end(player);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
