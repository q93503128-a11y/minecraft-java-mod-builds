package kr.moonseungjun.turnboundre.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.DefinitionRepository;
import kr.moonseungjun.turnboundre.world.WorldEncounterAnchorResolver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.phys.Vec3;

/** DEBUG_ONLY authored-anchor placement harness. It never defines production world coordinates or visual art. */
public final class TurnboundDebugAnchorCommands {
    public static final String DEBUG_ANCHOR_TAG = "turnbound_re:debug_anchor";
    private static final double SPAWN_DISTANCE = 2.5D;

    private TurnboundDebugAnchorCommands() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            DefinitionRepository definitions
    ) {
        if (definitions == null) throw new IllegalArgumentException("definitions must not be null");
        dispatcher.register(Commands.literal("turnbound_re")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("debug_anchor")
                        .then(Commands.argument("locator", StringArgumentType.greedyString())
                                .executes(ctx -> placeAnchor(
                                        ctx.getSource(),
                                        definitions,
                                        StringArgumentType.getString(ctx, "locator"))))));
    }

    private static int placeAnchor(
            CommandSourceStack source,
            DefinitionRepository definitions,
            String rawLocator
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String locator = rawLocator == null ? "" : rawLocator.trim();
        if (locator.isEmpty()) {
            source.sendFailure(Component.literal("DEBUG_ONLY locator is required"));
            return 0;
        }

        DefinitionRegistry registry = definitions.snapshot().registry();
        String dimensionId = player.level().dimension().identifier().toString();
        WorldEncounterAnchorResolver.Resolved resolved = WorldEncounterAnchorResolver
                .resolve(registry, locator, dimensionId)
                .orElse(null);
        if (resolved == null) {
            source.sendFailure(Component.literal(
                    "DEBUG_ONLY locator is not authored for dimension=" + dimensionId + ": " + locator));
            return 0;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        else horizontal = horizontal.normalize();
        Vec3 position = player.position().add(horizontal.scale(SPAWN_DISTANCE));

        // Minecraft 26.2 keeps Interaction width/height/response setters private.
        // The vanilla 1x1 hitbox is sufficient for this operator harness; center it around normal eye aim.
        // response=false still consumes a normal interaction on both sides, so no access-transformer/NBT hack is needed.
        Interaction anchor = new Interaction(EntityTypes.INTERACTION, player.level());
        anchor.setPos(position.x, player.getEyeY() - 0.5D, position.z);
        anchor.entityTags().add(WorldEncounterAnchorResolver.tagFor(locator));
        anchor.entityTags().add(DEBUG_ANCHOR_TAG);
        player.level().addFreshEntity(anchor);

        source.sendSuccess(() -> Component.literal(
                "DEBUG_ONLY authored anchor placed locator=" + locator
                        + " encounter=" + resolved.encounter().id()
                        + " entity=" + anchor.getUUID()
                        + " — right-click straight ahead; cleanup: /kill @e[type=minecraft:interaction,tag="
                        + DEBUG_ANCHOR_TAG + "]"), false);
        return 1;
    }
}
