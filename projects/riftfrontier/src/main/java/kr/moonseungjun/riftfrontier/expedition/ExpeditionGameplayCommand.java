package kr.moonseungjun.riftfrontier.expedition;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Temporary command surface for M2-B. Final contract UI remains behind the UI reference/design gate. */
public final class ExpeditionGameplayCommand {
    private ExpeditionGameplayCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("riftfrontier")
                .then(Commands.literal("expedition")
                    .then(Commands.literal("start").executes(context -> mutate(context.getSource(), () -> ExpeditionGameplayService.start(context.getSource().getPlayerOrException()))))
                    .then(Commands.literal("extract").executes(context -> mutate(context.getSource(), () -> ExpeditionGameplayService.extract(context.getSource().getPlayerOrException()))))
                    .then(Commands.literal("abort").executes(context -> mutate(context.getSource(), () -> ExpeditionGameplayService.failActive(context.getSource().getPlayerOrException(), "aborted by player"))))
                    .then(Commands.literal("status").executes(context -> {
                        var source = context.getSource();
                        var player = source.getPlayerOrException();
                        String status = ExpeditionGameplayService.status(player);
                        source.sendSuccess(() -> Component.literal("Riftfrontier expedition | " + status), false);
                        return Command.SINGLE_SUCCESS;
                    }))
                )
        );
    }

    private static int mutate(CommandSourceStack source, ThrowingAction action) throws CommandSyntaxException {
        try {
            action.run();
            return Command.SINGLE_SUCCESS;
        } catch (CommandSyntaxException error) {
            throw error;
        } catch (RuntimeException error) {
            source.sendFailure(Component.literal("Riftfrontier expedition rejected: " + error.getMessage()));
            return 0;
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws CommandSyntaxException;
    }
}
