package kr.moonseungjun.turnboundre.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.world.FunctionalWorldSliceBuilder;
import kr.moonseungjun.turnboundre.world.FunctionalWorldSliceLayout;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Operator-only harness for the pre-art M6 authored world slice. */
public final class TurnboundWorldSliceCommands {
    private TurnboundWorldSliceCommands() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            DefinitionRepository definitions
    ) {
        if (definitions == null) throw new IllegalArgumentException("definitions required");
        dispatcher.register(Commands.literal("turnbound_re_world_slice")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("validate")
                        .executes(ctx -> validate(ctx.getSource(), definitions)))
                .then(Commands.literal("build")
                        .executes(ctx -> build(ctx.getSource(), definitions))));
    }

    private static int validate(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String dimension = player.level().dimension().identifier().toString();
        List<String> errors = FunctionalWorldSliceLayout.validate(definitions.snapshot().registry(), dimension);
        if (!errors.isEmpty()) {
            source.sendFailure(Component.literal("World slice contract rejected: " + String.join("; ", errors)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "World slice contract valid: HUB_01 -> REGION_01 with mining, farming, fishing and 2 encounters."), false);
        return 1;
    }

    private static int build(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        final FunctionalWorldSliceBuilder.Result result;
        try {
            result = FunctionalWorldSliceBuilder.build(player, definitions.snapshot().registry());
        } catch (RuntimeException failure) {
            source.sendFailure(Component.literal("World slice build rejected: " + failure.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "World slice built from origin "
                        + result.origin().getX() + " " + result.origin().getY() + " " + result.origin().getZ()
                        + ". Follow the east road: quarry/farm/river -> patrol -> elite. "
                        + "Encounter anchors=" + result.encounterAnchors().size()), false);
        return 1;
    }
}
