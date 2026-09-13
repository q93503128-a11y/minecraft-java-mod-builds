package kr.moonseungjun.turnboundre.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.world.FunctionalWorldSliceBuilder;
import kr.moonseungjun.turnboundre.world.FunctionalWorldSliceLayout;
import kr.moonseungjun.turnboundre.world.ProductionWorldSlicePlan;
import kr.moonseungjun.turnboundre.world.ProductionWorldSlicePrototypeBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Operator-only harness for M6 authored-world integration and the post-reference production prototype. */
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
                        .executes(ctx -> buildFunctional(ctx.getSource(), definitions)))
                .then(Commands.literal("prototype")
                        .executes(ctx -> buildPrototype(ctx.getSource(), definitions))));
    }

    private static int validate(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String dimension = player.level().dimension().identifier().toString();
        List<String> errors = new ArrayList<>(FunctionalWorldSliceLayout.validate(definitions.snapshot().registry(), dimension));
        errors.addAll(ProductionWorldSlicePlan.validate());
        if (!errors.isEmpty()) {
            source.sendFailure(Component.literal("World slice contract rejected: " + String.join("; ", errors)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "World slice contracts valid: functional loop plus production scale/readability plan."), false);
        return 1;
    }

    private static int buildFunctional(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        final FunctionalWorldSliceBuilder.Result result;
        try {
            result = FunctionalWorldSliceBuilder.build(player, definitions.snapshot().registry());
        } catch (RuntimeException failure) {
            source.sendFailure(Component.literal("World slice build rejected: " + failure.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Functional world slice built from origin "
                        + result.origin().getX() + " " + result.origin().getY() + " " + result.origin().getZ()
                        + ". Follow the east road: quarry/farm/river -> patrol -> elite. "
                        + "Encounter anchors=" + result.encounterAnchors().size()), false);
        return 1;
    }

    private static int buildPrototype(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        final ProductionWorldSlicePrototypeBuilder.Result result;
        try {
            result = ProductionWorldSlicePrototypeBuilder.build(player, definitions.snapshot().registry());
        } catch (RuntimeException failure) {
            source.sendFailure(Component.literal("Production world prototype rejected: " + failure.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Production-facing world prototype built from origin "
                        + result.origin().getX() + " " + result.origin().getY() + " " + result.origin().getZ()
                        + ". Forge hall -> resource branches -> patrol ruin -> rift landmark. "
                        + "Encounter anchors=" + result.encounterAnchors().size()), false);
        return 1;
    }
}
