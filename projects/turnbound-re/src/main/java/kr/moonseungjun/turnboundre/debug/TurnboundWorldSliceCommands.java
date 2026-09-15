package kr.moonseungjun.turnboundre.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.world.DrehmalExternalWorldBinding;
import kr.moonseungjun.turnboundre.world.FunctionalWorldSliceBuilder;
import kr.moonseungjun.turnboundre.world.FunctionalWorldSliceLayout;
import kr.moonseungjun.turnboundre.world.ProductionWorldSlicePlan;
import kr.moonseungjun.turnboundre.world.ProductionWorldSlicePrototypeBuilder;
import kr.moonseungjun.turnboundre.world.WorldFastTravelPrototype;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Operator-only world integration harness. Production world geometry comes from the selected external map. */
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
                .then(Commands.literal("bind_drehmal")
                        .executes(ctx -> bindDrehmal(ctx.getSource(), definitions)))
                .then(Commands.literal("build")
                        .executes(ctx -> buildFunctional(ctx.getSource(), definitions)))
                .then(Commands.literal("prototype")
                        .executes(ctx -> buildPrototype(ctx.getSource(), definitions))));
    }

    private static int validate(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String dimension = player.level().dimension().identifier().toString();
        DefinitionRegistry registry = definitions.snapshot().registry();
        List<String> errors = new ArrayList<>(FunctionalWorldSliceLayout.validate(registry, dimension));
        errors.addAll(ProductionWorldSlicePlan.validate());
        errors.addAll(WorldFastTravelPrototype.validate(registry));
        errors.addAll(DrehmalExternalWorldBinding.validate(registry));
        if (!errors.isEmpty()) {
            source.sendFailure(Component.literal("World integration contract rejected: " + String.join("; ", errors)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "World integration contracts valid: external-world binding plus retained functional harnesses."), false);
        return 1;
    }

    private static int bindDrehmal(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        final DrehmalExternalWorldBinding.Result result;
        try {
            result = DrehmalExternalWorldBinding.install(player, definitions.snapshot().registry());
        } catch (RuntimeException failure) {
            source.sendFailure(Component.literal("Drehmal world binding rejected: " + failure.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "External world bound without rebuilding map geometry. Hub="
                        + result.hubArrival().getX() + " " + result.hubArrival().getY() + " " + result.hubArrival().getZ()
                        + ", Region=" + result.regionArrival().getX() + " " + result.regionArrival().getY() + " " + result.regionArrival().getZ()), false);
        return 1;
    }

    private static int buildFunctional(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        final FunctionalWorldSliceBuilder.Result result;
        try {
            result = FunctionalWorldSliceBuilder.build(player, definitions.snapshot().registry());
        } catch (RuntimeException failure) {
            source.sendFailure(Component.literal("Functional harness build rejected: " + failure.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Functional harness built from origin "
                        + result.origin().getX() + " " + result.origin().getY() + " " + result.origin().getZ()
                        + ". This is not the production world. Encounter anchors=" + result.encounterAnchors().size()), false);
        return 1;
    }

    private static int buildPrototype(CommandSourceStack source, DefinitionRepository definitions) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        final ProductionWorldSlicePrototypeBuilder.Result result;
        final WorldFastTravelPrototype.Result travel;
        try {
            DefinitionRegistry registry = definitions.snapshot().registry();
            result = ProductionWorldSlicePrototypeBuilder.build(player, registry);
            travel = WorldFastTravelPrototype.install(player, registry, result.origin());
        } catch (RuntimeException failure) {
            source.sendFailure(Component.literal("Legacy world-layout harness rejected: " + failure.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Legacy world-layout harness built from origin "
                        + result.origin().getX() + " " + result.origin().getY() + " " + result.origin().getZ()
                        + ". It is retained only for mechanics comparison, not production map design. "
                        + "Encounter anchors=" + result.encounterAnchors().size()
                        + ", travel anchors=" + travel.anchorEntityIds().size()), false);
        return 1;
    }
}
