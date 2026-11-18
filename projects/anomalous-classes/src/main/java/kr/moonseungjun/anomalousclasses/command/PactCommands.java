package kr.moonseungjun.anomalousclasses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import kr.moonseungjun.anomalousclasses.state.PactService;
import kr.moonseungjun.anomalousclasses.state.PactTrialService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/** Functional contract access until the dedicated contract/altar screen is produced. */
public final class PactCommands {
    private PactCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pact")
                .executes(ctx -> { PactService.status(ctx.getSource().getPlayerOrException()); return 1; })
                .then(Commands.literal("contact")
                    .then(Commands.argument("patron",StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player=ctx.getSource().getPlayerOrException();
                            PactService.previewSign(player,StringArgumentType.getString(ctx,"patron"));
                            return 1;
                        })))
                .then(Commands.literal("release")
                    .then(Commands.argument("patron",StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer player=ctx.getSource().getPlayerOrException();
                            PactService.previewRelease(player,StringArgumentType.getString(ctx,"patron"));
                            return 1;
                        })))
                .then(Commands.literal("challenge")
                    .executes(ctx -> {
                        PactTrialService.challenge(ctx.getSource().getPlayerOrException());
                        return 1;
                    }))
                .then(Commands.literal("confirm")
                    .executes(ctx -> {
                        PactService.confirm(ctx.getSource().getPlayerOrException());
                        return 1;
                    })));
    }
}
